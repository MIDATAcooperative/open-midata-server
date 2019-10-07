angular.module('portal')
.controller('QueryEditorCtrl', ['$scope', '$state', 'status', 'session', 'formats', '$translate', 'apps', 'server', 'labels', '$translatePartialLoader', function($scope, $state, status, session, formats, $translate, apps, server, labels, $translatePartialLoader) {
		
	$scope.error = null;	
	$scope.status = new status(true, $scope);
    $scope.query = { queryStr : "", json : {} };
    $scope.newentry = undefined;
    $scope.target = { type : "study" };
    $scope.blocks = [];
    $scope.currentBlock = undefined;
    $scope.writemodes = apps.writemodes;
    $scope.datePickers = {  };
    $scope.dateOptions = {
 	  	 formatYear: 'yy',
 	  	 startingDay: 1,
 	  	  
    };	
    
    $translatePartialLoader.addPart("developers");
    
	$scope.reload = function() {
		//console.log($state.current.data);
		//console.log($state.params.studyId);
		if ($state.current.data.mode == "study") {
			$scope.mode = "study";
			$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($state.params.studyId).url))
			.then(function(data) { 				
				$scope.study = data.data;	
			    $scope.query = { queryStr : JSON.stringify($scope.study.recordQuery), json : $scope.study.recordQuery };
			    $scope.blocks = parseAccessQuery($scope.query.json);			    
			    
			    if ($scope.blocks.length === 0) $scope.addNew();
			});				
		} else if ($state.current.data.mode == "app") {
			$scope.mode = "app";
			$scope.status.doBusy(apps.getApps({ "_id" : $state.params.appId }, ["creator", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type","accessTokenUrl", "authorizationUrl", "consumerKey", "consumerSecret", "tokenExchangeParams", "defaultQuery", "defaultSpaceContext", "defaultSpaceName", "previewUrl", "recommendedPlugins", "requestTokenUrl", "scopeParameters","secret","redirectUri", "url","developmentServer","version","i18n","status", "resharesData", "allowsUserSearch", "pluginVersion", "requirements", "termsOfUse", "orgName", "publisher", "unlockCode", "writes", "icons", "apiUrl", "noUpdateHistory"]))
			.then(function(data) { 
				$scope.app = data.data[0];
				$scope.target.appname = $scope.app.filename;
				$scope.query = { queryStr : JSON.stringify($scope.app.defaultQuery), json : $scope.app.defaultQuery };
			    $scope.blocks = parseAccessQuery($scope.query.json);
			    
			    if ($scope.blocks.length === 0) $scope.basicAppResources();
			    
			});
		}
	};
	
	$scope.basicAppResources = function() {
		var toadd = [ "Patient", "PseudonymizedPatient","Practitioner", "AuditEvent", "Consent", "Group", "Subscription"];
	    $scope.target.askresources = [];
	    angular.forEach(toadd, function(x) {
	    	lookupContent(x).then(function(y) { 
	    		$scope.target.askresources.push(y);
	    		$translate("queryeditor.ask."+y.content).then(function(v) { if (v && v!=y.content) y.text = v;});
	    	});
	    });
	};
	
	
	$scope.resourceOptions = {
	  "fhir/AuditEvent" : ["noapp", "noowner", "notime", "nopublic"], 
	  "fhir/Consent" : ["noapp", "noowner", "notime", "nopublic"],
	  "fhir/Group" : ["noowner"],
	  "fhir/Patient" : ["noapp", "notime", "nopublic"],
	  "fhir/Person" : ["noapp", "noowner", "notime", "nopublic"],
	  "fhir/Practitioner" : ["noapp", "noowner", "notime"],
	  "fhir/Subscription" : ["noapp", "noowner", "notime", "nopublic"],
      "fhir/Observation" : ["effective"],
      "fhir/QuestionnaireResponse" : ["custom"],
      "fhir/DocumentReference" : []	
	};
	
	var fullTextSearch = function(what) {
		var searchresult = [];
		var already = {};
		
		var add = function(entry) {
			if (!already[entry.key]) { searchresult.push(entry); already[entry.key] = entry; return true; }
			return false;
		};
		
		var lookup = function(content) {
			  return lookupCodes(content);		      
		};
		
		var addgroup = function(dat) {
			var grp = { key : "grp "+dat.name, group : dat.name, system : dat.system, display : dat.label[$translate.use()], contents:[] };
			var addgrp = function(what) {				
				grp.contents.push(what); 
			};
			var recproc = function(dat) {
				if (dat.contents && dat.contents.length > 1) {
				    for (var i2=0;i2<dat.contents.length;i2++) {
				    	lookupContent(dat.contents[i2]).then(addgrp);
				    }
				}
				if (dat.children) {
					for (var i3=0;i3<dat.children.length;i3++) {
						recproc(dat.children[i3]);					    
				    }
				}
			};
			if (dat.contents && dat.contents.length == 1) return;
			if (add(grp)) {
				recproc(dat);
			}
		};
		
		formats.listCodes()
		.then(function(result) {
			var l = result.data.length;		
			for (var i=0;i<l;i++) {
				var dat = result.data[i];
				//console.log(dat.code);
				if (dat.code.toLowerCase() == what) {
					lookupContent(dat.content)
					.then(lookup).then(add);
				}
			}
			//console.log(searchresult);
		});
		
		formats.listContents()
		.then(function(result) {
			var l = result.data.length;		
			for (var i=0;i<l;i++) {
				var dat = result.data[i];
				for (var lang in dat.label) {
				  if (dat.label[lang].toLowerCase().indexOf(what) >= 0) {					 
				    lookupCodes({ key : dat.content, content : dat.content, display : dat.label[$translate.use()], format : dat.resourceType })
				    .then(add);					
				  }
				}
			}
			//console.log(searchresult);
		});	
		
		
		formats.listGroups()
		.then(function(result) {
			var l = result.data.length; 		
			for (var i2=0;i2<l;i2++) { 
				var dat = result.data[i2];
				for (var lang in dat.label) {					
				  if (dat.label[lang].toLowerCase().indexOf(what) >= 0 || dat.name.toLowerCase().indexOf(what) >= 0) {					 
					  addgroup(dat);
				    
				  }
				} 
			}			
		});
		
		apps.getApps({ filename : what}, ["defaultQuery"])
		.then(function(r) {
			if (r.data && r.data.length == 1) {
				var q = r.data[0].defaultQuery;
				if (q.content) {
					
				}
			}
		});
		
		return searchresult;
	};
	
	var lookupCodes = function(entry) {
		return formats.searchCodes({ content : entry.content },["code","system","version","display"])
		.then(function(result) {			
		    entry.codes = [];
			for (var i=0;i<result.data.length;i++) {
				entry.codes.push(result.data[i]);
			}
			return entry;
		});
	};
	
	var lookupContent = function(name) {
		return formats.searchContents({ content : name }, ["content", "label", "resourceType"])
		.then(function(result) {
			if (result.data.length == 1) return { key : result.data[0].content, format : result.data[0].resourceType, content : result.data[0].content, display : result.data[0].label[$translate.use()] };
		});
	};
	
	var buildAccessQuery = function() {		
		var finalblocks = [];
		var keys = {};			
		for (var i=0;i<$scope.blocks.length;i++) {
			var block = $scope.blocks[i];			
			var fb = {};
			if (block.format) fb.format = [ block.format ];		
			if (block.system) fb["group-system"] = block.system;
			if (block.owner && block.owner != "all") fb.owner = [ block.owner ];
			if (block["public"] && block["public"] != "no") fb["public"] = block["public"];
			if (block.app && block.app != "all") {
				if (block.app == "self") {
					fb.app = [ $scope.target.appname ];					
				}
				else fb.app = [ block.appName ];
			}
			if (block.timeRestriction && block.timeRestrictionMode) {
				fb[block.timeRestrictionMode] = block.timeRestrictionDate;
			}
			if (block.dataPeriodRestriction && block.dataPeriodRestrictionMode) {
				if (!fb.data) fb.data = {};
				if (block.dataPeriodRestrictionMode === "effective") {
					if (block.dataPeriodRestrictionStart) fb.data["effectiveDateTime|effectivePeriod.start|null"] = { "!!!ge" : block.dataPeriodRestrictionStart };
					if (block.dataPeriodRestrictionEnd) fb.data["effectiveDateTime|effectivePeriod.end|null"] = { "!!!lt" : block.dataPeriodRestrictionEnd };
				}
			}
			if (block.customFilter && block.customFilterValue) {
				try {
				  fb.data = JSON.parse(block.customFilterValue);
				} catch (e) {}
			}
			
			var k = JSON.stringify(fb);
			if (block.code) k+="code/"+block.content;
			if (block.content) k+="content";
			if (block.group) k+="group";
			
			if (keys[k]) {
				if (block.content) keys[k].content.push(block.content);
				if (block.code) keys[k].code.push(block.code);
				if (block.group) keys[k].group.push(block.group);
			} else {
				keys[k] = fb;
				finalblocks.push(fb);
				if (block.content) fb.content = [ block.content ];
				if (block.code) fb.code = [ block.code ];
				if (block.group) fb.group = [ block.group ];
			}									
								
		}		
		if (finalblocks.length > 1) {
			return { "$or" : finalblocks };
		} else if (finalblocks.length == 1) {
			return finalblocks[0];
		} else {
			return {};
		}
	};
	
	var parseAccessQuery = function(query, outerquery, rarray) {
		var ac = function(path) {
			if (query[path] !== undefined) return query[path];
			if (outerquery && outerquery[path] !== undefined) return outerquery[path];
			return undefined;
		};
		var unwrap = function(arr, field) {
			var out = [];
			angular.forEach(arr, function(elem) {
				if (elem[field]) {
					if (angular.isArray(elem[field])) {						
						if (elem[field].length == 1) {
							var copy = JSON.parse(JSON.stringify(elem));
							copy[field] = copy[field][0];
							out.push(copy);
						} else {
							angular.forEach(elem[field], function(v) {
								var copy1 = JSON.parse(JSON.stringify(elem));
								copy1[field] = v;
								out.push(copy1);
							});
						}
					} else out.push(elem);
				} else out.push(elem);
			});
			return out;
		};
		var noarray = function(a) {
			if (angular.isArray(a) && a.length) return a[0];
			return a;
		};
	    
		var result = rarray || [];
		
		if (query.$or) {
			for (var i = 0;i<query.$or.length;i++) parseAccessQuery(query.$or[i], query, result);
		} else {
		
			var nblock = {};
			if (ac("format")) nblock.format = ac("format");
			if (ac("content")) nblock.content = ac("content");
			if (ac("code")) nblock.code = ac("code");		
			if (ac("group")) nblock.group = ac("group");
			if (ac("group-system")) nblock.system = ac("group-system");
			nblock["public"] = ac("public") || "no";
			if (ac("created-after")) {
				nblock.timeRestriction = true;
				nblock.timeRestrictionMode = "created-after";
				nblock.timeRestrictionDate = new Date(ac("created-after"));
			}
			if (ac("updated-after")) {
				nblock.timeRestriction = true;
				nblock.timeRestrictionMode = "updated-after";
				nblock.timeRestrictionDate = new Date(ac("updated-after"));
			}
			if (ac("data")) {
				var p = ac("data");
				if (p["effectiveDateTime|effectivePeriod.start|null"]) {
					nblock.dataPeriodRestriction = true;
					nblock.dataPeriodRestrictionMode = "effective";
					var d = p["effectiveDateTime|effectivePeriod.start|null"]["!!!ge"] || p["effectiveDateTime|effectivePeriod.start|null"].$ge; 
					nblock.dataPeriodRestrictionStart = new Date(d);					
				}
				if (p["effectiveDateTime|effectivePeriod.end|null"]) {
					nblock.dataPeriodRestriction = true;
					nblock.dataPeriodRestrictionMode = "effective";
					var d2 = p["effectiveDateTime|effectivePeriod.end|null"]["!!!lt"] || p["effectiveDateTime|effectivePeriod.end|null"].$lt;
					nblock.dataPeriodRestrictionEnd = new Date(d2);					
				}
				if (!nblock.dataPeriodRestriction) {
					nblock.customFilter = true;
					nblock.customFilterValue = JSON.stringify(p);
				}
			}
			if (ac("app")) {
				nblock.app = ac("app");
			}
			if (ac("owner")) {
				nblock.owner = noarray(ac("owner"));
			}
			console.log(nblock);
			angular.forEach(unwrap(unwrap(unwrap(unwrap(unwrap([ nblock ],"group"),"code"),"content"),"app"),"format"), function(r) {
				if (!r.app) r.app = "all";
				if (r.app == $scope.target.appname) { r.app = "self";r.appName = $scope.target.appname; }
				else if (r.app !== "all") { r.appName = r.app; r.app = "other"; }
				if (!r.owner) r.owner = "all";
				if (r.content) {
					labels.getContentLabel($translate.use(), r.content).then(function(v) { r.display = v; });
				} else if (r.group) {
					labels.getGroupLabel($translate.use(), r["group-system"] || "v1", r.group).then(function(v) { r.display = v; });
				} else if (r.format) {
					r.display = r.format;
				}				
				if (r.content || r.group || r.code || r.format) { result.push(r); } 
			});
		}
		return result;
	};
	
	$scope.search = function() {
		$scope.newentry.choices = undefined;
		var what = $scope.newentry.search.toLowerCase();
        $scope.newentry.choices = fullTextSearch(what);					
	};
	
	$scope.addNew = function() {
		 $scope.newentry = { search : "" };
	};
	
	$scope.addContent = function(content, code) {
		var newblock = { display : content.display, isnew : true, owner : "all", app : "all", "public" : "no"  };
		if (content.format) newblock.format = content.format;
		if (content.content) { newblock.content = content.content; }
		if (content.group) { newblock.group = content.group; newblock.system = content.system; }
		if (code) newblock.code = code.system+" "+code.code;
		//$scope.blocks.push(newblock);
		
		$scope.selectBlock(newblock);		
		
		$scope.newentry = undefined;
	};
	
	$scope.applyBlock = function() {
		if ($scope.currentBlock["public"] == "only" || $scope.currentBlock["public"] == "also" ) $scope.currentBlock.owner = "all";
		if ($scope.currentBlock.format && $scope.currentBlock.format.lengh===0) $scope.currentBlock.format = undefined;
		if ($scope.currentBlock.isnew) {
			$scope.blocks.push($scope.currentBlock);
			$scope.currentBlock.isnew = false;
		}
		
		if (!$scope.currentBlock.timeRestriction) {
			$scope.currentBlock.timeRestrictionDate = $scope.currentBlock.timeRestrictionMode = undefined; 
		}
		if (!$scope.currentBlock.dataPeriodRestriction) {
			$scope.currentBlock.dataPeriodRestrictionStart = $scope.currentBlock.dataPeriodRestrictionEnd = $scope.currentBlock.dataPeriodRestrictionMode = undefined; 
		}
		if ($scope.currentBlock.app == "self") {$scope.currentBlock.appName = $scope.target.appname; }
		$scope.currentBlock = undefined;
		
		$scope.query.json = buildAccessQuery();
		$scope.query.queryStr = JSON.stringify($scope.query.json);
		console.log($scope.query.queryStr);
	};
	
	$scope.selectBlock = function(block) {
		console.log(block);
		$scope.currentBlock = block;
		
		$scope.currentBlock.flags = {};
		
		var ro = $scope.resourceOptions[block.format];
		if (ro) {
			angular.forEach(ro, function(r) { $scope.currentBlock.flags[r] = true; });
		}
		
		if (!$scope.currentBlock.flags.notime) {
		   $scope.timeModes = ["created-after", "updated-after" ];
		} else $scope.timeModes = undefined;
		
		$scope.dataPeriodModes = [];
		
		if ($scope.currentBlock.flags.effective) {
			$scope.dataPeriodModes.push("effective");
		}		
		
		$scope.newentry = undefined;
	};
	
	$scope.deleteBlock = function() {
		if (!$scope.currentBlock.isNew) {
			$scope.blocks.splice($scope.blocks.indexOf($scope.currentBlock), 1);
		}
		$scope.currentBlock = undefined;
		
		$scope.query.json = buildAccessQuery();
		$scope.query.queryStr = JSON.stringify($scope.query.json);
		
		if ($scope.blocks.length === 0) $scope.addNew();
	};
	
	$scope.enableExpertMode = function() {
		$scope.expertmode = true;
		$scope.currentBlock = undefined;
		$scope.newentry = undefined;
	};
	
	$scope.expertModeDone = function() {
		

		 try {				  
			  $scope.query.json = JSON.parse($scope.query.queryStr);
			  $scope.error = null;
		      $scope.expertmode = false;
		      $scope.blocks = parseAccessQuery($scope.query.json);
		} catch (e) {
			console.log(e);
			$scope.error = e.message;
			//$scope.myform.defaultQuery.$setValidity('json', false);
			//$scope.error = "Invalid JSON in Access Query!";
			return;
		}
								
		
	};
	
	$scope.addPreselection = function() {
		angular.forEach($scope.target.askresources, function(r) {
			if (r.selected) {
				r.selected = undefined;				
				$scope.addContent(r);
				$scope.applyBlock();
			}
		});
		$scope.target.askresources = undefined;
	};
	
	$scope.saveExit = function() {
		//$scope.query.json = buildAccessQuery();
		if ($scope.mode == "study") {
			   var data = { recordQuery : $scope.query.json };
			   $scope.status.doAction("update", server.put(jsRoutes.controllers.research.Studies.update($state.params.studyId).url, JSON.stringify(data)))
			  .then(function(data) { 				
				    $scope.cancel();				    
			   }); 
		} else if ($scope.mode == "app") {
			$scope.app.defaultQuery = $scope.query.json;
			$scope.status.doAction('submit', apps.updatePlugin($scope.app))
			    .then(function() { $state.go("^.manageapp", { appId : $state.params.appId }); });
		}
	};
	
	$scope.cancel = function() {
		if ($scope.mode == "study") {
			$state.go("^.rules", { studyId : $state.params.studyId });
		} else if ($scope.mode == "app") {
			$state.go("^.manageapp", { appId : $state.params.appId });
		}
	};
	
	$scope.cancelsearch = function() {
		$scope.newentry = undefined;
	};
	
	session.currentUser.then(function() { $scope.reload(); });	
	
}]);