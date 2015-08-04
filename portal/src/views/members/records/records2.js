angular.module('portal')
.controller('RecordsCtrl', ['$scope', '$http',  '$filter', '$location', 'dateService', 'records', 'circles', 'formats', 'status', function($scope, $http, $filter, $location, dateService, records, circles, formats, status) {
	
	// init
	$scope.error = null;
		
	$scope.loadingApps = true;
	$scope.loadingRecords = true;
	$scope.userId = null;
	$scope.apps = [];
	$scope.records = [];	
	$scope.tree = [ ];
	$scope.compare = [];
	$scope.selectedAps = null;
	$scope.status = new status(true);
	
	// get current user
	$http(jsRoutes.controllers.Users.getCurrentUser()).
		success(function(userId) {
			$scope.userId = userId;
			$scope.availableAps = [{ name : "My Data", aps:userId, owner : "self"  }, { name : "All Data", aps:userId, owner : "all"}];
			$scope.displayAps = $scope.availableAps[0];
			$scope.getApps(userId);
			$scope.getAvailableSets(userId);
			$scope.loadGroups();
			$scope.getRecords(userId, "self")
			.then(function() {
			
						
			var p = window.location.pathname.split("/");
			console.log(p);
			if (p.length >= 5) {
			  var selectedType = window.location.pathname.split("/")[3];
			  var selected = window.location.pathname.split("/")[4];
			  $scope.displayAps = $scope.availableAps[1];
			  $scope.selectedAps = { "_id" : { "$oid" : selected }, type : selectedType };
			  $scope.compare = null;
			  $scope.loadSharingDetails();
			} else $scope.loadShared(userId); 
			});
		});
	
	// get apps
	$scope.getApps = function(userId) {
		var properties = {"_id": userId};
		var fields = ["apps"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) {
				$scope.getAppDetails(users[0].apps);
			}).
			error(function(err) { $scope.error = "Failed to load apps: " + err; });
	};
	
	// get name and type for app ids
	$scope.getAppDetails = function(appIds) {
		var properties = {"_id": appIds, "type" : ["create","oauth1","oauth2"] };
		var fields = ["name", "type"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data)).
			success(function(apps) {
				$scope.apps = apps;
				$scope.loadingApps = false;
			}).
			error(function(err) { $scope.error = "Failed to load apps: " + err; });
	};
	
	// get records
	$scope.getRecords = function(userId, owner) {
		//$scope.loadingRecords = true;
		var properties = {};
		if (owner) properties.owner = owner;
		if ($scope.debug) properties.streams = "true";
		return records.getRecords(userId, properties, ["id", "owner", "ownerName", "content", "created", "name", "group"]).
		then(function(results) {
			$scope.records = results.data;
			if ($scope.gi != null) $scope.prepareRecords();	
			$scope.loadingRecords = false;
		});
	};
	
	$scope.getAvailableSets = function(userId) {
		circles.get({ "member": userId }, ["name","aps","ownerName"])
		.then(function(results) {
			//$scope.availableAps = [{ name : "Your Data", aps:userId, owner : "self"  }, { name : "All Data", aps:userId, owner : "all"}];
			angular.forEach(results.data, function(circle) { 
				$scope.availableAps.push({ name:"Shared by "+circle.ownerName, aps:circle._id.$oid });
			});
		});
	};
	
	$scope.selectSet = function() {
		$scope.getRecords($scope.displayAps.aps, $scope.displayAps.owner)
		.then(function() { $scope.loadSharingDetails(); });
		
	};
	
	$scope.showDebug = function() {
		$scope.debug = true;
		$scope.getRecords($scope.displayAps.aps, $scope.displayAps.owner)
		.then(function() { $scope.loadSharingDetails(); });
		
	};
	
	$scope.loadGroups = function() {
		formats.listGroups().
		then(function(result) { 
			$scope.gi = result.data;
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
	   	if (newgroup.parent == null) {
	   		newgroup.fullLabel = newgroup.label;
	   		$scope.tree.push(newgroup);
	   	} else {
	   		var prt = getOrCreateGroup(newgroup.parent);
	   		if (prt.parent != null) newgroup.fullLabel = prt.label + " / "+newgroup.label;
	   		else newgroup.fullLabel = newgroup.label;
	   		prt.children.push(newgroup);
	   	}
	   	
	   	groups[group] = newgroup;
	   	return newgroup;
	};
	
	var getOrCreateFormat = function(format, group) {
	   	if (groups["_"+format] != null) return groups["_"+format];
	   	console.log(format);
	   	var grp = getOrCreateGroup(group);
	   	var newfmt = { name : format, type:"content", fullLabel:"Content: "+format, parent:group, children:[], records:[] }; 
	   	grp.children.push(newfmt);	   		   	
	   	groups["_"+format] = newfmt;
	   	return newfmt;
	};
		
	var countRecords = function(group) {
		var c = group.records.length;		
		angular.forEach(group.children, function(g) { c+= countRecords(g); });
		group.count = c;
		group.open =  group.type == "content" || group.parent == null;
		return c;
	};
		
	
	var countShared = function(group, sh) {
		var s = 0;	
		var alls = $scope.isSharedGroup(group);
		
		angular.forEach(group.records, function(r) { if (alls || $scope.isShared(r)) s++; });		
		angular.forEach(group.children, function(g) { s+= countShared(g); });
		
		group.countShared = s;
		return s;
	};
	
	$scope.prepareRecords = function() {
		$scope.tree = [];
		groups = {};
		angular.forEach($scope.records, function(record) {
		    var format = record.content;
		    var group = record.group;
		    var groupItem = getOrCreateFormat(format, group);
		    groupItem.records.push(record);
		});
		angular.forEach($scope.tree, function(t) { countRecords(t); });
	};
	
	$scope.deleteRecord = function(record, group) {
		$http.post(jsRoutes.controllers.Records["delete"]().url, { "_id" : record.id }).
		success(function(data) {
			group.records.splice(group.records.indexOf(record), 1);
		});
	};
	
	
	// go to record creation/import dialog
	$scope.createOrImport = function(app) {
		if (app.type === "create") {
			window.location.href = portalRoutes.controllers.Records.create(app._id.$oid).url;
		} else {
			window.location.href = portalRoutes.controllers.Records.importRecords(app._id.$oid).url;
		}
	};
	
	// show record details
	$scope.showDetails = function(record) {
		window.location.href = portalRoutes.controllers.Records.details(record.id).url;
	};
	
	// check whether the user is the owner of the record
	$scope.isOwnRecord = function(record) {
		return $scope.userId.$oid === record.owner.$oid;
	};
	
	$scope.loadShared = function() {
		if ($scope.shared == null) {
			$http.get(jsRoutes.controllers.Records.getSharingInfo().url).
			success(function(data) {			
				$scope.shared = data.shared;
				$scope.circles = data.circles;
				$scope.spaces = data.spaces;
				$scope.participations = data.participations;	
				$scope.memberkeys = data.memberkeys;
				$scope.loadingSharing = false;
				
				$scope.compare = [];
				angular.forEach($scope.circles, function(circle) { circle.type="circles"; $scope.compare.push(circle); });
				angular.forEach($scope.participations, function(part) { part.type="participations"; $scope.compare.push(part); });
				angular.forEach($scope.memberkeys, function(mk) { mk.type="memberkeys"; $scope.compare.push(mk); });
				
			}).
			error(function(err) {
				$scope.error = "Failed to load Sharing: " + err;
				$scope.loadingSharing = false;
			});				
		} 
	};
	
	$scope.loadSharingDetails = function() {
		if ($scope.selectedAps == null) return;
		$scope.status.doBusy($http.get(jsRoutes.controllers.Records.getSharingDetails($scope.selectedAps._id.$oid).url)).
		then(function(results) {
			console.log(results.data);
		    $scope.sharing = results.data;
		    $scope.sharing.ids = {};
		    if ($scope.sharing.query) {
		    	if ($scope.sharing.query.format && !angular.isArray($scope.sharing.query.format)) { $scope.sharing.query.format = [ $scope.sharing.query.format ]; }
		    	if ($scope.sharing.query.group && !angular.isArray($scope.sharing.query.group)) { $scope.sharing.query.group = [ $scope.sharing.query.group ]; }
		    }
		    angular.forEach($scope.sharing.records, function(r) { $scope.sharing.ids[r] = true; });
		    angular.forEach($scope.tree, function(t) { countShared(t); });
		});
	};
	
	$scope.isShared = function(record) {
	   if (!$scope.sharing) return;
	   return $scope.sharing.ids[record._id.$oid];
	};
	
	$scope.isSharedGroup = function(group) {
	   var type = group.type == "content" ? "content" : "group";
	   group.parentShared = (group.parent != null && groups[group.parent].shared);
	   group.parentExcluded = (group.parent != null && groups[group.parent].excluded);
	   var excluded = $scope.sharing && 
	       $scope.sharing.query &&
	       $scope.sharing.query._exclude && 
	       $scope.sharing.query._exclude[type] &&
	       $scope.sharing.query._exclude[type].indexOf(group.name) >= 0;
       group.excluded = group.parentExcluded || excluded;
       
	   if (!$scope.sharing || !$scope.sharing.query || !$scope.sharing.query[type]) {
		   group.shared = group.parentShared && !excluded;
		   return group.shared;
	   }
	   
	   var r = group.shared = ($scope.sharing.query[type].indexOf(group.name) >= 0 || group.parentShared) && !excluded; 
	   return r;
	};
	
	
	
	$scope.share = function(record, group) {
		shareInQuery("_id", record._id.$oid);
		$scope.status.doBusy(records.share($scope.selectedAps._id.$oid, record._id.$oid, $scope.selectedAps.type, $scope.sharing.query));
		$scope.sharing.ids[record._id.$oid] = true;
		
		while (group != null) {
			console.log(group);
			group.countShared++;
			group = groups[group.parent];
		}
	};
	
	var unexclude = function(type, item) {
		if ($scope.sharing.query._exclude &&
				$scope.sharing.query._exclude[type] &&
				$scope.sharing.query._exclude[type].indexOf(item) >= 0) {
				 var h = $scope.sharing.query._exclude[type];
				 h.splice(h.indexOf(item), 1);
				 if (h.length === 0) {
					 $scope.sharing.query._exclude[type] = undefined;
				 }
				 return true;
		} else return false;
	};
	
	var shareInQuery = function(type, item) {
		if (!$scope.sharing.query) $scope.sharing.query = {};
		if (!$scope.sharing.query[type]) $scope.sharing.query[type] = [];
		
		if (!unexclude(type, item)) {
		  if (type != "_id") $scope.sharing.query[type].push(item);
		}
	};
	
	var unshareFromQuery = function(type, item) {
		var idx = $scope.sharing.query[type] != null ? $scope.sharing.query[type].indexOf(item) : -1;
		if (idx<0) {
			if ($scope.sharing.query._exclude == null) $scope.sharing.query._exclude = {};
			if ($scope.sharing.query._exclude[type] == null) $scope.sharing.query._exclude[type] = [];
			$scope.sharing.query._exclude[type].push(item);
		} else {
		  $scope.sharing.query[type].splice(idx, 1);
		  if ($scope.sharing.query[type].length === 0) $scope.sharing.query[type] = undefined;
		}
	};
	
	$scope.unshare = function(record, group) {
		if (group.shared) unshareFromQuery("_id", record._id.$oid);
		$scope.status.doBusy(records.unshare($scope.selectedAps._id.$oid, record._id.$oid, $scope.selectedAps.type, $scope.sharing.query));
		$scope.sharing.ids[record._id.$oid] = false;
		
		while (group != null) {
			group.countShared--;
			group = groups[group.parent];
		}
	};
	
	$scope.shareGroup = function(group) {
		var type = group.type == "content" ? "content" : "group";
		shareInQuery(type, group.name);
		
		$scope.status.doBusy(records.share($scope.selectedAps._id.$oid, null, $scope.selectedAps.type, $scope.sharing.query)).
		then(function() { $scope.loadSharingDetails(); });
	};
	
	$scope.unshareGroup = function(group) {
		var type = group.type == "content" ? "content" : "group";
		unshareFromQuery(type, group.name);
		var recs = [];
		
		var unselect = function(group) {
			angular.forEach(group.records, function(r) { recs.push(r._id.$oid); });
			angular.forEach(group.children, function(c) {
				unexclude(c.type == "content" ? "content" : "group", c.name);
				unselect(c);
			});
		};
		unselect(group);
		
		console.log($scope.selectedAps);
		$scope.status.doBusy(records.unshare($scope.selectedAps._id.$oid, recs, $scope.selectedAps.type, $scope.sharing.query)).
		then(function() { $scope.loadSharingDetails(); });
	};
					
}]);
