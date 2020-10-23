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
.controller('RepositoryCtrl', ['$scope', '$state', 'server', 'apps', 'status', 'ENV', '$http', '$window', '$translatePartialLoader', '$interval', function($scope, $state, server, apps, status, ENV, $http, $window, $translatePartialLoader, $interval) {
	
	// init
	$scope.error = null;
	
	$scope.status = new status(false, $scope);
	
	$scope.crit = { sel : null };
			
	$scope.loadApp = function(appId) {
		$scope.appId=appId;
		$scope.status.doBusy(apps.getApps({ "_id" : appId }, ["creator", "filename", "name", "description", "repositoryUrl", "repositoryDate" ]))
		.then(function(data) { 
			$scope.app = data.data[0];			
		});
	};
	
	$scope.loadReport = function(appId) {
		server.get(jsRoutes.controllers.Market.updateFromRepository($scope.appId).url).then(function(result) {
		   $scope.report = result.data;	
		   if (!$scope.crit.sel && $scope.report && $scope.report.clusterNodes && $scope.report.clusterNodes.length) {			   
			   $scope.crit.sel = $scope.dot($scope.report.clusterNodes[0]); 
		   }
		});
		
		
	};
	
	$scope.dot = function(x) {
		return x!=null ? x.replaceAll(".","[dot]") : "";
	};
	
	$scope.style = function(p,c,n) {
		if (!$scope.report || !$scope.report.status) return "list-group-item-light";
		if ($scope.report.status.indexOf(c)>=0) {
			if ($scope.report.status.indexOf("FAILED")>=0 && $scope.report.status.indexOf(n)<0) {
			   return "list-group-item-danger";
			} else {
			   return "list-group-item-success";
			}
		} else if ($scope.report.status.indexOf(p)>=0 && $scope.report.status.indexOf("FAILED")<0) {
		  return "active"; 
		} else {
		  return "list-group-item-light";
		}
	}
	
	$scope.submit = function() {
		
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
				
		if (! $scope.myform.$valid) {
			var elem = $window.document.querySelector('input.ng-invalid');
			if (elem && elem.focus) elem.focus();
			return;
		}
		
		$scope.app.doDelete = undefined;
		$scope.status.doAction("submit", server.post(jsRoutes.controllers.Market.updateFromRepository($scope.app._id).url, JSON.stringify($scope.app)))
    	.then(function(result) {
    		$scope.report = result.data; 
    	});

	};
	
    $scope.doDelete = function() {
		
		$scope.submitted = true;			
		$scope.error = null;
		$scope.app.doDelete = true; 							
		$scope.status.doAction("submit", server.post(jsRoutes.controllers.Market.updateFromRepository($scope.app._id).url, JSON.stringify($scope.app)))
    	.then(function(result) {
    		$scope.report = result.data; 
    	});

	};
			
	$translatePartialLoader.addPart("developers");	
	$scope.loadApp($state.params.appId);	
	
	var pull = $interval(function() { $scope.loadReport($state.params.appId); }, 5000); // pull after 5 seconds

	$scope.$on('$destroy', function(){
	  $interval.cancel(pull);
	});
	
	$scope.loadReport($state.params.appId);
}]);