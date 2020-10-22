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
.controller('AutoImportCtrl', ['$scope', '$state', 'session', 'server', 'spaces', 'status', 'ENV', function($scope, $state, session, server, spaces, status, ENV) {
	
	$scope.baseurl = ENV.apiurl;

	$scope.calls = [];
			
	$scope.init = function(userId, appId) {
		$scope.userId = userId;
		var properties = {"owner": userId, "visualization" : appId, "context" : "sandbox" };
	    var fields = ["name", "type", "order", "autoImport", "context", "visualization"];
	    var data = {"properties": properties, "fields": fields};
	    spaces.get(properties, fields)
	    .then(function(results) {	    	
	    	angular.forEach(results.data, function(space) {
	    		spaces.getUrl(space._id)
	    		.then(function(spaceurl) {	    			
	    			var call = { name : space.name, autoImport : space.autoImport, authToken : spaceurl.data.token, loggedIn : !(spaceurl.data.authorizationUrl) };
	    			$scope.calls.push(call);	    			
	    		});
	    	});
	    });
				
	};
	
		
	$scope.doInstall = function() {
		$state.go("^.visualization", { visualizationId : $scope.app._id, context : "sandbox" });
	};
	
	session.currentUser.then(function(userId) { $scope.init(userId, $state.params.appId); });	
}]);