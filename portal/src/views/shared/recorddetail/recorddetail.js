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
.controller('RecordCtrl', ['$scope', '$state', '$translate', 'server', '$sce', 'records', 'status', 'ENV', '$window','spaces', function($scope, $state, $translate, server, $sce, records, status, ENV, $window, spaces) {
	// init
	$scope.error = null;
	$scope.record = {};
	$scope.status = new status(true);
	
	var recordId = $state.params.recordId;
	var data = {"_id": recordId };
	
	$scope.status.doBusy(records.getUrl(recordId)).
	then(function(results) {
		if (results.data) {
          var url = spaces.mainUrl(results.data, $translate.use());          
		  $scope.url = $sce.trustAsResourceUrl(url);
		}
	});
		
	$scope.status.doBusy(server.post(jsRoutes.controllers.Records.get().url, JSON.stringify(data)))
	.then(function(records) {
			$scope.record = records.data;
			$scope.record.json = JSON.stringify($scope.record.data, null, "\t");
			if (_.has($scope.record.data, "type") && $scope.record.data.type === "file") {
				$scope.downloadLink = true;
			}
			//loadUserNames();
			loadAppName();										    	    	
									
		});
	
	var loadUserNames = function() {
		var data = {"properties": {"_id": [$scope.record.owner, $scope.record.creator]}, "fields": ["firstname", "lastname"]};
		server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			then(function(users1) {
				var users = users1.data;
				_.each(users, function(user) {
					if ($scope.record.owner === user._id) { $scope.record.owner = (user.firstname+" "+user.lastname).trim(); }
					if ($scope.record.creator === user._id) { $scope.record.creator = (user.firstname+" "+user.lastname).trim(); }
				});
			}, function(err) { $scope.error = { code : "error.internal" }; } );
	};
	
	var loadAppName = function() {
		var data = {"properties": {"_id": $scope.record.app}, "fields": ["name"]};
		server.post(jsRoutes.controllers.Plugins.get().url, JSON.stringify(data)).
			then(function(apps) { $scope.record.app = apps.data[0].name; }, function(err) { $scope.error = { code : "error.internal" }; });
	};
		
	$scope.goBack = function() {
		$window.history.back();
	};
	
	$scope.download = function() {
		$scope.status.doAction("download", server.token())
		.then(function(response) {
		  document.location.href = ENV.apiurl + jsRoutes.controllers.Records.getFile(recordId).url + "?token=" + encodeURIComponent(response.data.token);
		});
	};
	
	$scope.isFile = function() {
		return $scope.record && $scope.record.data && ($scope.record.data.resourceType === "Binary" || 
		       ($scope.record.data.resourceType === "DocumentReference" && $scope.record.data.content) ||
		       $scope.record.data.resourceType === "Media" ||
		       ($scope.record.data.resourceType === "DiagnosticReport" && $scope.record.data.presentedForm));
	};
	
	$scope.openAppLink = function(data) {		
		spaces.openAppLink($state, $scope.userId, data);	 
	};
	
}]);




