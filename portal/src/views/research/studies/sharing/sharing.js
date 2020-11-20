/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('portal')
.controller('StudySharingCtrl', ['$scope', '$state', 'server', 'studies', 'status', 'records', 'session', 'apps', function($scope, $state, server, studies, status, records, session, apps) {
	
	$scope.studyId = $state.params.studyId;
	$scope.crit = { };
	$scope.status = new status(true, $scope);
	$scope.timeCrits = ["created-after","created-before", "updated-after","updated-before"];
	$scope.sources = ["me", "project"];
	$scope.ids = [];
	
	$scope.reload = function(userId) {
		$scope.userId = userId;
		$scope.status.doBusy(records.getInfos(userId, { }, "ALL")).
		then(function(results) {
			$scope.tooManyConsents = results.status == 202;
			$scope.infos = results.data;
            console.log(results.data);	
            $scope.contents = results.data[0].contents;
            $scope.apps = results.data[0].apps;
            $scope.formats = results.data[0].formats;
            
            apps.getApps({ _id : $scope.apps },["_id","name"]).then(function(result) {
            	$scope.appNames = {};
            	for (var i=0;i<result.data.length;i++) $scope.appNames[result.data[i]._id]=result.data[i].name;
            });
            
		});
	};
	
	$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyId).url))
    .then(function(data) { 				
		$scope.study = data.data;
		$scope.studyGroups = data.data.groups;
	});	
	
	var buildQuery = function() {
		var properties = {  };		
		var crit = $scope.crit;
		if (crit.source=="me") properties.owner="self";
		else {
			properties.usergroup = $scope.studyId;
			properties["force-local"] = true;
			//properties["study-related"] = true;
		}
		if (crit.content) properties.content = crit.content;
		if (crit.format) properties.format = crit.format;
		if (crit.app) properties.app = crit.app;
		if (crit.timeCrit == "created-after") properties["created-after"] = crit.time;
		if (crit.timeCrit == "created-before") properties["created-before"] = crit.time;
		if (crit.timeCrit == "updated-after") properties["updated-after"] = crit.time;
		if (crit.timeCrit == "updated-before") properties["updated-before"] = crit.time;
		return properties;
	};
	
	var sharedQuery = function() {
		var properties = buildQuery();
		properties["force-local"] = undefined;
		properties.study = $scope.studyId;
		properties["study-group"] = $scope.crit.studyGroup;
		properties["study-related"] = true;
		properties.owner = undefined;
		return properties;
	};
	
	var sharingQuery = function() {
		var properties = buildQuery();
		if ($scope.ids.length>0) {
			properties._id = $scope.ids;			
		}
		return properties;
	};
	
	$scope.dosearch = function() {
		$scope.submitted = true;
        $scope.error = null;		
		if (! $scope.myform.$valid) return;
		$scope.search();
	};
	
	$scope.search = function() {
		var properties = buildQuery();
		var sq = sharedQuery();
		$scope.ids = [];
		$scope.results = null;
		$scope.error = null;
		console.log(properties);
		$scope.status.doAction("search", records.getRecords($scope.userId, properties, ["_id", "name", "created","format","app"]))
		.then(function(result) {
			$scope.results = result.data;
			$scope.found = result.data.length;
			
			$scope.status.doAction("search", records.getRecords($scope.userId, sq, ["_id"]))
			.then(function(result2) {
				var map = {};
				for (var i=0;i<$scope.results.length;i++) {
					map[$scope.results[i]._id] = $scope.results[i]; 
				}
				for (var i2=0;i2<result2.data.length;i2++) {
					var r = map[result2.data[i2]._id];
					if (r) r.selected = true;
				}	
				$scope.submitted = false;
			});
			
		});
	};
	
	$scope.share = function() {
		console.log("SHARE");
		$scope.error = null;
		var data = {
			"properties" : sharingQuery(), 
			"target-study" : $scope.studyId, 
			"target-study-group" : $scope.crit.studyGroup
		};
		$scope.status.doAction("share", server.post(jsRoutes.controllers.Records.shareRecord().url, JSON.stringify(data)))
		.then(function(result) {
			$scope.search();
		});
	};
	
	$scope.unshare = function() {
		$scope.error = null;
		var data = {
				"properties" : sharingQuery(), 
				"target-study" : $scope.studyId, 
				"target-study-group" : $scope.crit.studyGroup
			};
		$scope.status.doAction("share", server.post(jsRoutes.controllers.Records.unshareRecord().url, JSON.stringify(data)))
		.then(function(result) {
			$scope.search();
		});
	};
	
	$scope.change = function() {
		$scope.results = [];
		$scope.submitted = false;
		$scope.error = null;
		$scope.found = 0;
		$scope.ids = [];
	};
	
	$scope.toggle = function(array,itm) {
		console.log(array);
		var pos = array.indexOf(itm);
		if (pos < 0) array.push(itm); else array.splice(pos, 1);
	};

	session.currentUser.then(function(userId) { $scope.reload(userId); });
	
}]);