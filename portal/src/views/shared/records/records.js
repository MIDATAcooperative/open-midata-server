angular.module('portal')
.controller('RecordsCtrl', ['$scope', '$state', '$translate', '$timeout', 'server',  '$filter', 'dateService', 'records', 'circles', 'formats', 'apps', 'status', 'studies', 'session', 'spaces', function($scope, $state, $translate, $timeout, server, $filter, dateService, records, circles, formats, apps, status, studies, session, spaces) {
	
	// init
	$scope.error = null;
		
	$scope.userId = null;
	$scope.lang = $translate.use();
	
	$scope.records = [];
	$scope.infos = [];
	$scope.tree = [ ];
	$scope.compare = [];
	$scope.selectedAps = null;
	$scope.status = new status(true);
	$scope.allowDelete = $state.current.allowDelete;
	$scope.open = {};
	$scope.treeMode = "group";
	
	var contentLabels = {};
	var loadLabels = {};
	var loadPlugins = {};
	var doLoadLabels = false;
	var doLoadPlugins = false;
	
	// get current user
	session.currentUser
	.then(function(userId) {		
			$scope.userId = userId;
			$scope.availableAps = [{ i18n : "records.my_data" , name : "My Data", aps:userId, owner : "self"  }, { i18n:"records.all_data", name : "All Data", aps:userId, owner : "all"}];
			$scope.displayAps = $scope.availableAps[0];
			var n = "RecordsCtrl_"+$state.current.name;
			session.load(n, $scope, ["open"]);
			
			if ($state.params.selected != null) {	
				 var selectedType = $state.params.selectedType;
				 var selected = $state.params.selected;
				 $scope.selectedType = selectedType;
				 $scope.selectedAps = { "_id" : selected , type : selectedType };
				 $scope.explainPreselection();
			}
			
			$scope.getAvailableSets(userId);
			$scope.loadGroups();
			var what = ($state.params.selected != null) ? null : "self";
			$scope.getInfos(userId, what)
			.then(function() {
			
				if ($state.params.selected != null) {										 
				  $scope.displayAps = $scope.availableAps[1];				  
				  $scope.compare = null;
				  $scope.loadSharingDetails();				 
				} else $scope.loadShared(userId); 
			});
		});
	
	$scope.setTreeMode = function(mode) {
		if ($scope.treeMode === mode) return;
		
		$scope.records = [];
		$scope.infos = [];
		$scope.tree = [ ];
		$scope.compare = [];
		
		
		$scope.treeMode = mode;
		
		$scope.getInfos($scope.displayAps.aps, $scope.displayAps.owner, $scope.displayAps.study)
		.then(function() {				
		   $scope.loadSharingDetails();				 			
		});
		
	};
	
	
	// get records
	$scope.getRecords = function(userId, owner, group, study, groupObj) {
		//$scope.loadingRecords = true;
		var properties = {};
		if (owner) properties.owner = owner;
		if (study) properties.study = study;
		if (groupObj && groupObj.plugin) {
			properties.app = groupObj.plugin;
			properties.content = groupObj.content;
		}
		else if (group) {
			properties.group = group;
			properties["group-system"] = "v1";
		}
		if ($scope.debug) properties.streams = "true";
		return $scope.status.doAction("load", records.getRecords(userId, properties, ["id", "owner", "ownerName", "content", "created", "name", "group", "app"])).
		then(function(results) {
			$scope.records = results.data;
			if (groupObj) groupObj.allRecords = results.data;
			if ($scope.gi != null) $scope.prepareRecords();				
		});
	};
	
	$scope.getInfos = function(userId, owner, study) {
		
		var properties = {};
		if (owner) properties.owner = owner;
		if (study) properties.study = study;
		if ($scope.debug) properties.streams = "true";
		return $scope.status.doBusy(records.getInfos(userId, properties, $scope.treeMode === "plugin" ? "CONTENT_PER_APP" : "CONTENT")).
		then(function(results) {
			$scope.infos = results.data;
			if ($scope.gi != null) $scope.prepareInfos();				
		});
	};
	
	$scope.setOpen = function(group, open) {
		group.open = open;
		$scope.open[group.id] = open;
		/*if (open && !group.loaded) {
			group.loaded = true;
			$scope.getRecords($scope.displayAps.aps, $scope.displayAps.owner, group.name, $scope.displayAps.study);
		}*/
	};
	
	
	$scope.getAvailableSets = function(userId) {
		
		if ($state.current.role == "research") {
			
			studies.research.list()
			.then(function(results) {
				angular.forEach(results.data, function(study) { 
					$scope.availableAps.push({ i18n:"records.study", name:study.name, aps:userId, study : study._id });
				});
			});
			
		} else {
		
			circles.listConsents({ "member": userId }, ["name","owner", "ownerName"])
			.then(function(results) {
				//$scope.availableAps = [{ name : "Your Data", aps:userId, owner : "self"  }, { name : "All Data", aps:userId, owner : "all"}];
				angular.forEach(results.data, function(circle) { 
					$scope.availableAps.push({ i18n:"records.shared", name:circle.ownerName, aps:circle._id });
				});
			});
		}
	};
	
	$scope.selectSet = function() {
		$scope.getInfos($scope.displayAps.aps, $scope.displayAps.owner, $scope.displayAps.study)
		.then(function() { $scope.loadSharingDetails(); });
		
	};
	
	$scope.showDebug = function() {
		$scope.debug = true;
		$scope.getInfos($scope.displayAps.aps, $scope.displayAps.owner, $scope.displayAps.study)
		.then(function() { $scope.loadSharingDetails(); });
		
	};
	
	$scope.addAllGroups = function() {
		angular.forEach($scope.gi, function(grp) {
			getOrCreateGroup(grp.name);
		});
	};
	
	$scope.loadGroups = function() {
		$scope.status.doBusy(formats.listGroups()).
		then(function(result) { 
			$scope.gi = result.data;			
			
			if ($scope.infos.length > 0) $scope.prepareInfos();	
			if ($scope.records.length > 0) $scope.prepareRecords();	
		});
	};
	
	$scope.loadContentLabels = function() {
		$scope.loadPluginLabels();
		if (!doLoadLabels) return;
		doLoadLabels = false;
		formats.searchContents({ content : Object.keys(loadLabels) },["content","label"]).
		then(function(result) { 
		  angular.forEach(result.data, function(c) {
			 contentLabels[c.content] = loadLabels[c.content].fullLabel = c.label[$scope.lang] || c.label.en;
			 
		  });
		  loadLabels = {};
		  
		});
	};
	
	$scope.loadPluginLabels = function() {
		if (!doLoadPlugins) return;
		doLoadPlugins = false;
				
		apps.getApps({"_id" : Object.keys(loadPlugins) },["_id", "name", "i18n"])
		.then(function(result) {
			angular.forEach(result.data, function(c) {
				var label = c.name;
				if (c.i18n && c.i18n[$scope.lang]) label = c.i18n[$scope.lang].name;
				contentLabels[c._id] = loadPlugins[c._id].fullLabel = label || c.name;				
			});
			loadPlugins = {};
		});									
	};
	
	var groups = {};
	var plugins = {};
		
	
	var getOrCreateGroup = function(group) {
	   	if (groups[group] != null) return groups[group];
	  
	   	var newgroup = $filter("filter")($scope.gi, function(x){  return x.name == group; })[0];
	   
	   	newgroup.children = [];
	   	newgroup.records = [];
	   	newgroup.infoCount = 0;
	   	newgroup.countShared = 0;
	   	newgroup.group = newgroup.name;
	   	newgroup.id = newgroup.name.replace(/[/\-]/g,'_');
	   	
	   	if (newgroup.parent == null || newgroup.parent === "") {
	   		newgroup.fullLabel = newgroup.label[$scope.lang] || newgroup.name;
	   		$scope.tree.push(newgroup);
	   	} else {
	   		var prt = getOrCreateGroup(newgroup.parent);
	   		/*if (prt.parent != null) newgroup.fullLabel = (prt.label[$scope.lang] || prt.name) + " / "+(newgroup.label[$scope.lang] || newgroup.name);
	   		else*/ newgroup.fullLabel = newgroup.label[$scope.lang] || newgroup.name;
	   		prt.children.push(newgroup);
	   	}
	   	
	   	groups[group] = newgroup;
	   	return newgroup;
	};
	
	var getOrCreatePlugin = function(plugin) {
	   	if (plugins[plugin] != null) return plugins[plugin];
	  
	   	var newplugin = { id : "_"+plugin, plugin:plugin, parent:$scope.tree[0] };
	   
	   	newplugin.children = [];
	   	newplugin.contents = {};
	   	newplugin.records = [];
	   	newplugin.infoCount = 0;
	   	newplugin.countShared = 0;	   	
	   		   	
	   	newplugin.fullLabel = "Label "+newplugin.id;
	   	if (contentLabels[plugin]) {
	   		newplugin.fullLabel = contentLabels[plugin];
	   	} else {
	   		if (plugin) {
		   		loadPlugins[plugin] = newplugin;
		   		doLoadPlugins = true;
	   		}
	   	}
	   	
	   	
	   	$scope.tree[0].children.push(newplugin);
	   		   	
	   	plugins[plugin] = newplugin;
	   	return newplugin;
	};
	
	 	
	var getOrCreateFormat = function(format, group) {
	   	if (groups["cnt:"+format] != null) return groups["cnt:"+format];
	   
	   	var grp = getOrCreateGroup(group);
	   	var newfmt = { name : "cnt:"+format, content:format, type:"group", fullLabel:"Content: "+format, parent:group, children:[], records:[] };
	   	
	   	if (contentLabels[format]) {
	   		newfmt.fullLabel = contentLabels[format];
	   	} else {
	   		loadLabels[format] = newfmt;
	   		doLoadLabels = true;
	   	}
	   	
	   	grp.children.push(newfmt);	   		   	
	   	groups["cnt:"+format] = newfmt;
	   	return newfmt;
	};
	
	var getOrCreatePluginContent = function(content, plugin) {
	   	if (plugin.contents[content] != null) return plugin.contents[content];
	   	
	   	var newfmt = { name : "cnt:"+content, type:"group", fullLabel:"Content: "+content, parent:plugin, plugin:plugin.plugin, content:content, children:[], records:[] };
	   	
	   	if (contentLabels[content]) {
	   		newfmt.fullLabel = contentLabels[content];
	   	} else {
	   		loadLabels[content] = newfmt;
	   		doLoadLabels = true;
	   	}
	   	
	   	plugin.children.push(newfmt);	   		   	
	   	plugin.contents[content] = newfmt;
	   	return newfmt;
	};
		
	var countRecords = function(group) {
		var c = group.infoCount || group.records.length;		
		angular.forEach(group.children, function(g) { c+= countRecords(g); });
		group.count = c;
		group.open =  $scope.open[group.id] || group.open || (group.parent == null);
		return c;
	};
		
	
	var countShared = function(group) {
		var s = 0;			
		angular.forEach(group.children, function(g) { s+= countShared(g); });		
		group.countShared += s;
		return group.countShared;
	};
	
	var resetShared = function(group) {		
		angular.forEach(group.children, function(g) { resetShared(g); });		
		group.countShared = 0;		
	};
	
	$scope.prepareRecords = function() {
		
		if ($scope.treeMode === "plugin") {
			angular.forEach($scope.records, function(record) {
			    var format = record.content;
			    var pluginId = record.app;
			    var plugin = getOrCreatePlugin(pluginId);
			    var groupItem = getOrCreatePluginContent(format, plugin);
			    groupItem.records.push(record);
			    if (!record.name) record.name="no name";
			});
		} else {
			angular.forEach($scope.records, function(record) {
			    var format = record.content;
			    var group = record.group;
			    var groupItem = getOrCreateFormat(format, group);
			    groupItem.records.push(record);
			    if (!record.name) record.name="no name";
			});
		}
		angular.forEach($scope.tree, function(t) { countRecords(t); });
	};
	
	$scope.prepareInfos = function() {
		
		groups = {};
		plugins = {};		
		
		if ($scope.treeMode === "plugin") {
			$scope.tree = [ { name : "all",  fullLabel:"", parent:null, children:[], records:[] } ];
			$translate("records.all").then(function(f) { $scope.tree[0].fullLabel = f; });
			angular.forEach($scope.infos, function(info) {		    
			    var plugin = info.apps[0];
			    var pluginItem = getOrCreatePlugin(plugin);
			    			    
			    pluginItem.open = $scope.open[pluginItem.id] || false;
			    
			    var content = info.contents[0];
			    var contentItem = getOrCreatePluginContent(content, pluginItem);
			    contentItem.infoCount = info.count;
			    contentItem.loaded = false;
			    contentItem.records = [];
			    contentItem.open = $scope.open[contentItem.id] || false;
			    
			});
		} else {
			$scope.tree = [];
			$scope.addAllGroups();
			angular.forEach($scope.infos, function(info) {		    
			    var group = info.groups[0];
			    var groupItem = getOrCreateGroup(group);
			    
			    //groupItem.records = [];
			    //groupItem.loaded = false;
			    groupItem.open = $scope.open[groupItem.id] || false;
			    
			    var content = info.contents[0];
			    var contentItem = getOrCreateFormat(content, group);
			    contentItem.infoCount = info.count;
			    contentItem.loaded = false;
			    contentItem.records = [];
			    contentItem.open = $scope.open[contentItem.id] || false;
			    
			});
		}
		angular.forEach($scope.tree, function(t) { countRecords(t); });
		$scope.loadContentLabels();
	};
	
	$scope.deleteRecord = function(record, group) {
		server.post(jsRoutes.controllers.Records["delete"]().url, { "_id" : record.id }).
		success(function(data) {			
			group.allRecords.splice(group.allRecords.indexOf(record), 1);			
		});
	};
	
	$scope.deleteGroup = function(group) {
		var props = {};
		if (group.plugin) props.app = group.plugin;
		if (group.content) props.content = group.content;
		if (group.group) props.group = group.group;
		
		server.post(jsRoutes.controllers.Records["delete"]().url, props).
		success(function(data) {
			$scope.loadGroups();
			$scope.getInfos($scope.userId, "self");
		});
	};
	
	
	// go to record creation/import dialog
	$scope.createOrImport = function(app) {
		if (app.type === "create") {
			$state.go('^.createrecord', { appId : app._id });
		} else {
			$state.go('^.importrecords', { appId : app._id });
		}
	};
	
	// show record details
	$scope.showDetails = function(record) {
		$("#recdetailmodal").modal('hide');
		$timeout(function() { $state.go('^.recorddetail', { recordId : record.id }); },500);
	};
	
	$scope.showRecords = function(group) {
		$scope.selectedData = group;
		if (!group.loaded) {
			group.loaded = true;
			$scope.getRecords($scope.displayAps.aps, $scope.displayAps.owner, group.name, $scope.displayAps.study, group);
		}
		$("#recdetailmodal").modal('show');
	};
	
	// check whether the user is the owner of the record
	$scope.isOwnRecord = function(record) {
		return $scope.userId === record.owner;
	};
	
	$scope.loadShared = function() {
		if ($scope.circles == null) {
			circles.listConsents({ owner : true }, ["name", "type", "status"])
			.then(function(results) {
				
                $scope.loadingSharing = false;				
				$scope.compare = [];
				angular.forEach(results.data, function(entry) { 
					entry.i18n = "records.just_name";
					$scope.compare.push(entry);
				});
											
			});		
		} 
	};
	
	$scope.loadSharingDetails = function() {
		if ($scope.selectedAps == null) return;
		$scope.status.doBusy(server.get(jsRoutes.controllers.Records.getSharingDetails($scope.selectedAps._id).url)).
		then(function(results) {
		
		    $scope.sharing = results.data;
		    $scope.sharing.ids = {};
		    if ($scope.sharing.query) {
		    	if ($scope.sharing.query["group-exclude"] && !angular.isArray($scope.sharing.query["group-exclude"])) { $scope.sharing.query["group-exclude"] = [ $scope.sharing.query["group-exclude"] ]; }
		    	if ($scope.sharing.query.group && !angular.isArray($scope.sharing.query.group)) { $scope.sharing.query.group = [ $scope.sharing.query.group ]; }
		    }
		    angular.forEach($scope.sharing.records, function(r) { $scope.sharing.ids[r] = true; });		 
		    angular.forEach($scope.tree, function(t) { resetShared(t); });
		    angular.forEach($scope.sharing.summary, function(s) {
		    	getOrCreateFormat(s.contents[0],s.groups[0]).countShared = s.count;
		    });
		    angular.forEach($scope.tree, function(t) { countShared(t); });
		    
		    $scope.loadContentLabels();
		});
	};
	
	$scope.isShared = function(record) {
	   if (record == null) return;
	   if (!$scope.sharing) return;
	   return $scope.sharing.ids[record._id];
	};
	
	$scope.isSharedGroup = function(group) {
	   if ($scope.treeMode === "plugin") return false;
	   
	   group.parentShared = (group.parent != null && groups[group.parent].shared);
	   group.parentExcluded = (group.parent != null && groups[group.parent].excluded);
	   var excluded = $scope.sharing && 
	       $scope.sharing.query &&
	       $scope.sharing.query["group-exclude"] && 	       
	       $scope.sharing.query["group-exclude"].indexOf(group.name) >= 0;
       group.excluded = group.parentExcluded || excluded;
       
	   if (!$scope.sharing || !$scope.sharing.query || !$scope.sharing.query.group) {
		   group.shared = group.parentShared && !excluded;
		   return group.shared;
	   }
	   
	   var r = group.shared = ($scope.sharing.query.group.indexOf(group.name) >= 0 || group.parentShared) && !excluded; 
	   return r;
	};
	
	
	
	$scope.share = function(record, group) {
		removeFromQuery("exclude-ids", record._id);
		$scope.status.doBusy(records.share($scope.selectedAps._id, record._id, $scope.selectedAps.type, $scope.sharing.query));
		$scope.sharing.ids[record._id] = true;
		
		while (group != null) {
		
			group.countShared++;
			group = groups[group.parent];
		}
	};
		
	
	var addToQuery = function(type, item) {
		if (!$scope.sharing.query) $scope.sharing.query = {};
		if (!$scope.sharing.query[type]) $scope.sharing.query[type] = [];
		
		if ($scope.sharing.query[type].indexOf(item) < 0) {
		  $scope.sharing.query[type].push(item);
		  return true;
		}
		
		return false;
	};
	
	var removeFromQuery = function(type, item) {
		if (!$scope.sharing.query) return false;
		if (!$scope.sharing.query[type]) return false;
		var idx = $scope.sharing.query[type].indexOf(item);
		if (idx < 0) return false;		
		$scope.sharing.query[type].splice(idx, 1);
		if ($scope.sharing.query[type].length === 0 && type != "group") $scope.sharing.query[type] = undefined;		
		return true;
	};
	
	$scope.unshare = function(record, group) {
		if (group.shared) addToQuery("exclude-ids", record._id);
		$scope.status.doBusy(records.unshare($scope.selectedAps._id, record._id, $scope.selectedAps.type, $scope.sharing.query));
		$scope.sharing.ids[record._id] = false;
		
		while (group != null) {
			group.countShared--;
			group = groups[group.parent];
		}
	};
	
	$scope.shareGroup = function(group) {
		var type = /*group.type == "content" ? "content" :*/ "group";
		if (!removeFromQuery("group-exclude", group.name)) {
		  addToQuery("group", group.name);
		}
		
		var unselect = function(group) {			
			angular.forEach(group.children, function(c) {
				removeFromQuery("group", c.name);
				removeFromQuery("group-exclude", c.name);
				unselect(c);
			});
		};
		unselect(group);
		
		$scope.status.doBusy(records.share($scope.selectedAps._id, null, $scope.selectedAps.type, $scope.sharing.query)).
		then(function() { $scope.loadSharingDetails(); });
	};
	
	$scope.unshareGroup = function(group) {
		var type = /*group.type == "content" ? "content" :*/ "group";
		
		if (!removeFromQuery("group", group.name)) {
			addToQuery("group-exclude", group.name);
		}		
		var recs = [];
		
		var unselect = function(group) {
			angular.forEach(group.records, function(r) { recs.push(r._id); });
			angular.forEach(group.children, function(c) {
				removeFromQuery("group", c.name);
				removeFromQuery("group-exclude", c.name);
				unselect(c);
			});
		};
		unselect(group);
		
	
		$scope.status.doBusy(records.unshare($scope.selectedAps._id, recs, $scope.selectedAps.type, $scope.sharing.query)).
		then(function() { $scope.loadSharingDetails(); });
	};
	
	$scope.explainPreselection = function() {
		if ($scope.selectedType == "circles") {
		   circles.listConsents({ _id : $scope.selectedAps._id }, ["name", "type", "authorized" ])
		   .then(function(data) {
			   $scope.consent = data.data[0];
		   });
		} else if ($scope.selectedType == "spaces") {
		   spaces.get({ _id : $scope.selectedAps._id }, ["name", "context"] )
		   .then(function(data) {
			 $scope.space = data.data[0];  
		   });
		}
	};
					
}]);
