/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('portal')
.controller('AppIconCtrl', ['$scope', '$state', 'server', 'apps', 'status', 'ENV', '$http', '$window', '$translatePartialLoader',function($scope, $state, server, apps, status, ENV, $http, $window, $translatePartialLoader) {
	
	// init
	$scope.error = null;
	
	$scope.status = new status(false, $scope);
	$scope.uses = ["LOGINPAGE", "APPICON"];
	$scope.meta = { };
			
	$scope.loadApp = function(appId) {
		$scope.appId=appId;
		$scope.status.doBusy(apps.getApps({ "_id" : appId }, ["creator", "filename", "name", "description", "icons" ]))
		.then(function(data) { 
			$scope.app = data.data[0];			
		});
	};
	
	$scope.submit = function() {
		
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		
		var fileelem = $window.document.getElementById("iconfile");
		
		if (! (fileelem && fileelem.files && fileelem.files.length == 1)) {
			$scope.error = "error.missing.file";
		}
		
		if (! $scope.myform.$valid) {
			var elem = $window.document.querySelector('input.ng-invalid');
			if (elem && elem.focus) elem.focus();
			return;
		}
		
		var fd = new FormData();
		
		console.log(fileelem);		
		fd.append('use', $scope.meta.use );
        fd.append('file', fileelem.files[0]);
        $scope.status.doAction("upload", $http.post(ENV.apiurl + "/api/developers/plugins/"+$scope.app._id+"/icon", fd, {
           transformRequest: angular.identity,
           headers: {'Content-Type': undefined, "X-Session-Token" : sessionStorage.token }
        })).then(function() {
        	$scope.loadApp($state.params.appId);
        });

	};
	
	$scope.doDelete = function(use) {
	   $scope.status.doAction("delete", server.delete(jsRoutes.controllers.Market.deleteIcon($scope.app._id, use).url))
	   .then(function() { $scope.loadApp($state.params.appId); });
	};
	
	$scope.getUrl = function(use) {
		if (!$scope.app) return null;
		return ENV.apiurl + "/api/shared/icon/" + use + "/" + $scope.app.filename;
	};
		
	$translatePartialLoader.addPart("developers");	
	$scope.loadApp($state.params.appId);	
}]);