var records = angular.module('records', ['date', 'services']);
records.controller('RecordsCtrl', ['$scope', '$http',  '$filter', 'dateService', 'records', 'circles', function($scope, $http, $filter, dateService, records, circles) {
	
	// init
	$scope.error = null;
		
	$scope.loadingApps = true;
	$scope.loadingRecords = true;
	$scope.userId = null;
	$scope.apps = [];
	$scope.records = [];	
	$scope.tree = [ ];
	$scope.compare = [];
	
	// get current user
	$http(jsRoutes.controllers.Users.getCurrentUser()).
		success(function(userId) {
			$scope.userId = userId;
			$scope.availableAps = [{ name : "My Data", aps:userId, owner : "self"  }, { name : "All Data", aps:userId, owner : "all"}];
			$scope.displayAps = $scope.availableAps[0];
			$scope.getApps(userId);
			$scope.getRecords(userId, "self");	
			$scope.getAvailableSets(userId);
			$scope.loadShared(userId);
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
		records.getRecords(userId, properties, ["id", "owner", "ownerName", "format", "created", "name", "group"]).
		then(function(results) {
			$scope.records = results.data;
			$scope.prepareRecords();	
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
		$scope.getRecords($scope.displayAps.aps, $scope.displayAps.owner);
		$scope.selectedAps = null;
	};
		
	
	var groups = {};
	
	var gi = [
	{ name:"root", label:"All", parent:null },
	{ name:"lifestyle", label:"Lifestyle", parent:"root" },
	{ name:"nutrition", label:"Nutrition", parent:"lifestyle" },
	{ name:"mood", label:"Mood", parent:"lifestyle" },
	{ name:"qself", label:"Quantified self", parent:"lifestyle" },
	{ name:"heartrate", label:"Heartrate", parent:"qself" },
	{ name:"movement", label:"Movement", parent:"lifestyle" },
	{ name:"sleep", label:"Sleep", parent:"lifestyle" },
	{ name:"health", label:"Health", parent:"root" },
	{ name:"alergies", label:"Alergies", parent:"health" },
	{ name:"lab", label:"Lab results", parent:"health" },
	{ name:"studies", label:"Studies", parent:"health" },
	{ name:"medication", label:"Medication", parent:"health" },
	{ name:"genome", label:"Genome", parent:"health" },
	{ name:"desease", label:"Desease specific", parent:"health" },
	{ name:"other", label:"Other", parent:"root" }];
	
	var getOrCreateGroup = function(group) {
	   	if (groups[group] != null) return groups[group];
	   	console.log(group);
	   	var newgroup = $filter("filter")(gi, function(x){  return x.name == group; })[0];
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
	   	if (groups[format] != null) return groups[format];
	   	console.log(format);
	   	var grp = getOrCreateGroup(group);
	   	var newfmt = { name : format, type:"format", fullLabel:"Format: "+format, parent:group, children:[], records:[] }; 
	   	grp.children.push(newfmt);	   		   	
	   	groups[format] = newfmt;
	   	return newfmt;
	};
		
	var countRecords = function(group) {
		var c = group.records.length;		
		angular.forEach(group.children, function(g) { c+= countRecords(g); });
		group.count = c;
		group.open = (c < 5) || group.type == "format" || group.parent == null;
		return c;
	};
	
	var countShared = function(group) {
		var s = 0;	
		if ($scope.isSharedGroup(group)) s = group.count;
		else {
			angular.forEach(group.records, function(r) { if ($scope.isShared(r)) s++; });		
		    angular.forEach(group.children, function(g) { s+= countShared(g); });
		}
		group.countShared = s;
		return s;
	};
	
	$scope.prepareRecords = function() {
		$scope.tree = [];
		groups = {};
		angular.forEach($scope.records, function(record) {
		    var format = record.format;
		    var group = record.group;
		    var groupItem = getOrCreateFormat(format, group);
		    groupItem.records.push(record);
		});
		angular.forEach($scope.tree, function(t) { countRecords(t); });
	};
	
	$scope.deleteRecord = function(record) {
		$http.post(jsRoutes.controllers.Records["delete"]().url, { "_id" : record.id }).
		success(function(data) {
			$scope.records.splice($scope.records.indexOf(record), 1);
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
		$http.get(jsRoutes.controllers.Records.getSharingDetails($scope.selectedAps._id.$oid).url).
		then(function(results) {
			console.log(results.data);
		    $scope.sharing = results.data;
		    $scope.sharing.ids = {};
		    angular.forEach($scope.sharing.records, function(r) { $scope.sharing.ids[r] = true; });
		    angular.forEach($scope.tree, function(t) { countShared(t); });
		});
	};
	
	$scope.isShared = function(record) {
	   if (!$scope.sharing) return;
	   return $scope.sharing.ids[record._id.$oid];
	};
	
	$scope.isSharedGroup = function(group) {
	   var type = group.type == "format" ? "format" : "group";
	   group.parentShared = (group.parent != null && groups[group.parent].shared);
	   if (!$scope.sharing || !$scope.sharing.query || !$scope.sharing.query[type]) {
		   group.shared = group.parentShared;
		   return group.shared;
	   }
	   
	   var r = group.shared = ($scope.sharing.query[type].indexOf(group.name) >= 0 || group.parentShared); 
	   return r;
	};
	
	$scope.share = function(record) {
		records.share($scope.selectedAps._id.$oid, record._id.$oid, $scope.selectedAps.type);
		$scope.sharing.ids[record._id.$oid] = true;
	};
	
	$scope.unshare = function(record) {
		records.unshare($scope.selectedAps._id.$oid, record._id.$oid, $scope.selectedAps.type);
		$scope.sharing.ids[record._id.$oid] = false;
	};
	
	$scope.shareGroup = function(group) {
		var type = group.type == "format" ? "format" : "group";
		if (!$scope.sharing.query) $scope.sharing.query = {};
		if (!$scope.sharing.query[type]) $scope.sharing.query[type] = [];
		$scope.sharing.query[type].push(group.name);
		records.share($scope.selectedAps._id.$oid, null, $scope.selectedAps.type, $scope.sharing.query);		
	};
	
	$scope.unshareGroup = function(group) {
		var type = group.type == "format" ? "format" : "group";
		var idx = $scope.sharing.query[type].indexOf(group.name);
		if (idx<0) return;
		$scope.sharing.query[type].splice(idx, 1);
		if ($scope.sharing.query[type].length == 0) $scope.sharing.query[type] = undefined;
		var recs = [];
		angular.forEach(group.records, function(r) { recs.push(r._id.$oid); });
		records.unshare($scope.selectedAps._id.$oid, recs, $scope.selectedAps.type, $scope.sharing.query);		
	};
					
}]);