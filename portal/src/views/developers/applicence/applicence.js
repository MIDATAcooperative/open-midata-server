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
.controller('AppLicenceCtrl', ['$scope', '$state', 'server', 'apps', 'status', function($scope, $state, server, apps, status) {
	
	// init
	$scope.error = null;
	
	$scope.status = new status(false, $scope);
	$scope.entities = ["USER","USERGROUP","ORGANIZATION"];
				
	$scope.loadApp = function(appId) {
		$scope.appId = appId;
		$scope.status.doBusy(apps.getApps({ "_id" : appId }, ["_id", "version", "creator", "filename", "name", "description", "licenceDef"]))
		.then(function(data) { 
			$scope.app = data.data[0];
			if ($scope.app.licenceDef) {
			  $scope.licence = $scope.app.licenceDef;
			  $scope.licence.required = true;
			} else {
			  $scope.licence = { required : false, allowedEntities : [] };
			}
			$scope.licence.version = $scope.app.version;
		});
	};
	
	
	$scope.updateApp = function() {		
		$scope.submitted = true;	
					
		$scope.app.msgOnly = true;				
		$scope.status.doAction('submit', server.post(jsRoutes.controllers.Market.updateLicence($scope.app._id).url, JSON.stringify($scope.licence)))
		.then(function() { $scope.submitted = false;$state.go("^.manageapp", { appId : $scope.app._id });  });
		
	};	
	
	$scope.toggle = function(array,itm) {		
		var pos = array.indexOf(itm);
		if (pos < 0) array.push(itm); else array.splice(pos, 1);
	};
			
	$scope.loadApp($state.params.appId);	
}]);