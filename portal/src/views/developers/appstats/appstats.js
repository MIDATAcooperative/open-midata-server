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
.controller('AppStatsCtrl', ['$scope', '$state', 'session', 'server', 'status', '$translatePartialLoader', 'apps', function($scope, $state, session, server, status, $translatePartialLoader, apps) {
	
	$scope.status = new status(true);    
	$scope.calls = [];
			
	$scope.init = function(userId, appId) {
		$scope.userId = userId;
		$scope.appId = appId;
		var properties = {"owner": userId, "visualization" : appId, "context" : "sandbox" };
	    var fields = ["name", "type", "order", "autoImport", "context", "visualization"];
	    var data = {"properties": properties, "fields": fields};
	    $scope.status.doBusy(server.get(jsRoutes.controllers.Market.getPluginStats(appId).url))
	    .then(function(results) {	    	
	    	$scope.calls = results.data;
	    	var firstrun;
	    	angular.forEach($scope.calls, function(c) {
	    	   if (!firstrun || c.firstrun < firstrun) firstrun = c.firstrun;
	    	   
	    	   c.queries = [];
	    	   angular.forEach(c.queryCount, function(v,k) {
			     c.queries.push({ k : k, v : v});
			   });
	    	});
	    	
	    	
	    	$scope.firstrun = firstrun;
	    });		
	    
	    $scope.status.doBusy(apps.getApps({ "_id" : appId }, ["filename", "name"]))
		.then(function(data) { 
			$scope.app = data.data[0];						
		});
	};
	
	$scope.reload = function() {
		$scope.init($scope.userId, $scope.appId);
	};
	
	$scope.reset = function() {
		$scope.status.doBusy(server.delete(jsRoutes.controllers.Market.deletePluginStats($scope.appId).url))
	    .then(function(results) {	    	
	    	$scope.calls = [];
	    });		
	};
	
	$translatePartialLoader.addPart("developers");
	
	session.currentUser.then(function(userId) { $scope.init(userId, $state.params.appId); });	
}]);