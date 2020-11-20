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

angular.module('views')
.controller('RecordDetailCtrl', ['$scope', 'server', '$attrs', 'views', 'records', 'apps', 'status', '$state', '$timeout', function($scope, server, $attrs, views, records, apps, status, $state, $timeout) {
		
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.record = {};
	$scope.status = new status(true);
	
	$scope.reload = function() {
	   if (!$scope.view.active) return;	
       $scope.status.doBusy(records.getRecord($scope.view.setup.id)).
	   then(function(result) {
			$scope.record = result.data;
			$scope.record.json = JSON.stringify($scope.record.data, null, "\t");
						
			//loadUserNames();
			
			apps.getApps({"_id": $scope.record.app}, ["name"]).
			then(function(result) { $scope.record.app = result.data[0].name; });
						
			//var split = $scope.record.created.split(" ");
			//$scope.record.created = split[0] + " at " + split[1];
		});
	};
    
    
	var loadUserNames = function() {		
		var data = {"properties": {"_id": [$scope.record.owner, $scope.record.creator]}, "fields": ["firstname", "lastname"]};
		$scope.status.doSilent(server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data))).
			then(function(result) {				
				_.each(result.data, function(user) {
					if ($scope.record.owner && $scope.record.owner === user._id) { $scope.record.owner = (user.firstname+" "+user.lastname).trim(); }
					if ($scope.record.creator && $scope.record.creator === user._id) { $scope.record.creator = (user.firstname+" "+user.lastname).trim(); }
				});
				if (!$scope.record.owner) $scope.record.owner = "?";
				if (!$scope.record.creator) $scope.record.creator = "Same as owner";
			});
	};
	
	$scope.showDetail = function() {		
		var recordId = $scope.view.setup.id;
		views.disableView($scope.view.id);
		var sname = $state.current.name.split('.')[0]+".recorddetail";		
		$timeout(function() { $state.go(sname, { recordId : recordId }); }, 500);		
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);