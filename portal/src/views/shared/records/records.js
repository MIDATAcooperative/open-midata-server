angular.module('portal')
.controller('RecordsCtrl', ['$scope', '$state', '$translate', 'server',  '$filter', 'dateService', 'records', 'circles', 'formats', 'apps', 'status', 'studies', 'session', 'spaces', function($scope, $state, $translate, server, $filter, dateService, records, circles, formats, apps, status, studies, session, spaces) {
	
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
	
	
	// get current user
	session.currentUser
	.then(function(userId) {		
			$scope.userId = userId;
			$scope.availableAps = [{ i18n : "records.my_data" , name : "My Data", aps:userId, owner : "self"  }, { i18n:"records.all_data", name : "All Data", aps:userId, owner : "all"}];
			$scope.displayAps = $scope.availableAps[0];
			
			if ($state.params.selected != null) {	
				 var selectedType = $state.params.selectedType;
				 var selected = $state.params.selected;
				 $scope.selectedType = selectedType;
				 $scope.selectedAps = { "_id" : { "$oid" : selected }, type : selectedType };
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
	
	
	
	// get records
	$scope.getRecords = function(userId, owner, group, study) {
		//$scope.loadingRecords = true;
		var properties = {};
		if (owner) properties.owner = owner;
		if (study) properties.study = study;
		if (group) {
			properties["group-strict"] = group;
			properties["group-system"] = "v1";
		}
		if ($scope.debug) properties.streams = "true";
		return $scope.status.doAction("load", records.getRecords(userId, properties, ["id", "owner", "ownerName", "content", "created", "name", "group"])).
		then(function(results) {
			$scope.records = results.data;
			if ($scope.gi != null) $scope.prepareRecords();				
		});
	};
	
	$scope.getInfos = function(userId, owner, study) {
		console.log(owner);
		var properties = {};
		if (owner) properties.owner = owner;
		if (study) properties.study = study;
		if ($scope.debug) properties.streams = "true";
		return $scope.status.doBusy(records.getInfos(userId, properties)).
		then(function(results) {
			$scope.infos = results.data;
			if ($scope.gi != null) $scope.prepareInfos();				
		});
	};
	
	$scope.setOpen = function(group, open) {
		group.open = open;
		if (open && !group.loaded) {
			group.loaded = true;
			$scope.getRecords($scope.displayAps.aps, $scope.displayAps.owner, group.name, $scope.displayAps.study);
		}
	};
	
	
	$scope.getAvailableSets = function(userId) {
		
		if ($state.current.role == "research") {
			
			studies.research.list()
			.then(function(results) {
				angular.forEach(results.data, function(study) { 
					$scope.availableAps.push({ i18n:"records.study", name:study.name, aps:userId, study : study._id.$oid });
				});
			});
			
		} else {
		
			circles.listConsents({ "member": userId }, ["name","owner", "ownerName"])
			.then(function(results) {
				//$scope.availableAps = [{ name : "Your Data", aps:userId, owner : "self"  }, { name : "All Data", aps:userId, owner : "all"}];
				angular.forEach(results.data, function(circle) { 
					$scope.availableAps.push({ i18n:"records.shared", name:circle.ownerName, aps:circle._id.$oid });
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
		formats.listGroups().
		then(function(result) { 
			$scope.gi = result.data;			
			console.log($scope.gi);
			if ($scope.infos.length > 0) $scope.prepareInfos();	
			if ($scope.records.length > 0) $scope.prepareRecords();	
		});
	};
	
	var groups = {};
		
	
	var getOrCreateGroup = function(group) {
	   	if (groups[group] != null) return groups[group];
	   	console.log(group);
	   	var newgroup = $filter("filter")($scope.gi, function(x){  return x.name == group; })[0];
	   	console.log(newgroup);
	   	newgroup.children = [];
	   	newgroup.records = [];
	   	newgroup.infoCount = 0;
	   	newgroup.countShared = 0;
	   	newgroup.id = newgroup.name.replace(/[/\-]/g,'_');
	   	
	   	if (newgroup.parent == null || newgroup.parent === "") {
	   		newgroup.fullLabel = newgroup.label[$scope.lang] || newgroup.name;
	   		$scope.tree.push(newgroup);
	   	} else {
	   		var prt = getOrCreateGroup(newgroup.parent);
	   		if (prt.parent != null) newgroup.fullLabel = (prt.label[$scope.lang] || prt.name) + " / "+(newgroup.label[$scope.lang] || newgroup.name);
	   		else newgroup.fullLabel = newgroup.label[$scope.lang] || newgroup.name;
	   		prt.children.push(newgroup);
	   	}
	   	
	   	groups[group] = newgroup;
	   	return newgroup;
	};
	
	/*
	var getOrCreateFormat = function(format, group) {
	   	if (groups["_"+format] != null) return groups["_"+format];
	   	console.log(format);
	   	var grp = getOrCreateGroup(group);
	   	var newfmt = { name : format, type:"content", fullLabel:"Content: "+format, parent:group, children:[], records:[] }; 
	   	grp.children.push(newfmt);	   		   	
	   	groups["_"+format] = newfmt;
	   	return newfmt;
	};*/
		
	var countRecords = function(group) {
		var c = group.infoCount || group.records.length;		
		angular.forEach(group.children, function(g) { c+= countRecords(g); });
		group.count = c;
		group.open =  /*group.type == "content" ||*/ group.open || (group.parent == null);
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
		//$scope.tree = [];
		//groups = {};
		angular.forEach($scope.records, function(record) {
		    var format = record.content;
		    var group = record.group;
		    var groupItem = getOrCreateGroup(group);
		    groupItem.records.push(record);
		    if (!record.name) record.name="no name";
		});
		angular.forEach($scope.tree, function(t) { countRecords(t); });
	};
	
	$scope.prepareInfos = function() {
		$scope.tree = [];
		groups = {};
		$scope.addAllGroups();
		angular.forEach($scope.infos, function(info) {		    
		    var group = info.groups[0];
		    var groupItem = getOrCreateGroup(group);
		    groupItem.infoCount = info.count;
		    groupItem.records = [];
		    groupItem.loaded = false;
		    groupItem.open = false;
		});
		angular.forEach($scope.tree, function(t) { countRecords(t); });
	};
	
	$scope.deleteRecord = function(record, group) {
		server.post(jsRoutes.controllers.Records["delete"]().url, { "_id" : record.id }).
		success(function(data) {
			group.records.splice(group.records.indexOf(record), 1);
		});
	};
	
	$scope.deleteGroup = function(group) {
		server.post(jsRoutes.controllers.Records["delete"]().url, { "group" : group }).
		success(function(data) {
			$scope.loadGroups();
			$scope.getInfos($scope.userId, "self");
		});
	};
	
	
	// go to record creation/import dialog
	$scope.createOrImport = function(app) {
		if (app.type === "create") {
			$state.go('^.createrecord', { appId : app._id.$oid });
		} else {
			$state.go('^.importrecords', { appId : app._id.$oid });
		}
	};
	
	// show record details
	$scope.showDetails = function(record) {
		$state.go('^.recorddetail', { recordId : record.id });
	};
	
	// check whether the user is the owner of the record
	$scope.isOwnRecord = function(record) {
		return $scope.userId.$oid === record.owner.$oid;
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
		$scope.status.doBusy(server.get(jsRoutes.controllers.Records.getSharingDetails($scope.selectedAps._id.$oid).url)).
		then(function(results) {
			console.log(results.data);
		    $scope.sharing = results.data;
		    $scope.sharing.ids = {};
		    if ($scope.sharing.query) {
		    	if ($scope.sharing.query["group-exclude"] && !angular.isArray($scope.sharing.query["group-exclude"])) { $scope.sharing.query["group-exclude"] = [ $scope.sharing.query["group-exclude"] ]; }
		    	if ($scope.sharing.query.group && !angular.isArray($scope.sharing.query.group)) { $scope.sharing.query.group = [ $scope.sharing.query.group ]; }
		    }
		    angular.forEach($scope.sharing.records, function(r) { $scope.sharing.ids[r] = true; });		 
		    angular.forEach($scope.tree, function(t) { resetShared(t); });
		    angular.forEach($scope.sharing.summary, function(s) {
		    	getOrCreateGroup(s.groups[0]).countShared = s.count;
		    });
		    angular.forEach($scope.tree, function(t) { countShared(t); });
		});
	};
	
	$scope.isShared = function(record) {
	   if (record == null) return;
	   if (!$scope.sharing) return;
	   return $scope.sharing.ids[record._id.$oid];
	};
	
	$scope.isSharedGroup = function(group) {
	   //var type = group.type == "content" ? "content" : "group";
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
		removeFromQuery("exclude-ids", record._id.$oid);
		$scope.status.doBusy(records.share($scope.selectedAps._id.$oid, record._id.$oid, $scope.selectedAps.type, $scope.sharing.query));
		$scope.sharing.ids[record._id.$oid] = true;
		
		while (group != null) {
			console.log(group);
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
		if (group.shared) addToQuery("exclude-ids", record._id.$oid);
		$scope.status.doBusy(records.unshare($scope.selectedAps._id.$oid, record._id.$oid, $scope.selectedAps.type, $scope.sharing.query));
		$scope.sharing.ids[record._id.$oid] = false;
		
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
		
		$scope.status.doBusy(records.share($scope.selectedAps._id.$oid, null, $scope.selectedAps.type, $scope.sharing.query)).
		then(function() { $scope.loadSharingDetails(); });
	};
	
	$scope.unshareGroup = function(group) {
		var type = /*group.type == "content" ? "content" :*/ "group";
		
		if (!removeFromQuery("group", group.name)) {
			addToQuery("group-exclude", group.name);
		}		
		var recs = [];
		
		var unselect = function(group) {
			angular.forEach(group.records, function(r) { recs.push(r._id.$oid); });
			angular.forEach(group.children, function(c) {
				removeFromQuery("group", c.name);
				removeFromQuery("group-exclude", c.name);
				unselect(c);
			});
		};
		unselect(group);
		
		console.log($scope.selectedAps);
		$scope.status.doBusy(records.unshare($scope.selectedAps._id.$oid, recs, $scope.selectedAps.type, $scope.sharing.query)).
		then(function() { $scope.loadSharingDetails(); });
	};
	
	$scope.explainPreselection = function() {
		if ($scope.selectedType == "circles") {
		   circles.listConsents({ _id : { "$oid" : $scope.selectedAps._id.$oid }}, ["name", "type", "authorized" ])
		   .then(function(data) {
			   $scope.consent = data.data[0];
		   });
		} else if ($scope.selectedType == "spaces") {
		   spaces.get({ _id : { "$oid" : $scope.selectedAps._id.$oid }}, ["name", "context"] )
		   .then(function(data) {
			 $scope.space = data.data[0];  
		   });
		}
	};
					
}]);