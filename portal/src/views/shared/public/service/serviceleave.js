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
.controller('ServiceLeaveCtrl', ['$scope', 'server', '$state', '$window', 'session', 'actions', function($scope, server, $state, $window, session, actions) {
	
	
	$scope.init = function() {
		console.log("init leave");
		actions.logout();
		if ($state.params.callback) {
			$scope.callback = $state.params.callback;			
		}
	};
	
	$scope.close = function() {
		server.post('/api/logout')
		.then(function() { 
			session.logout();
		    $window.close();
		});
	};
	
	$scope.leave = function() {
		server.post('/api/logout')
		.then(function() { 
			session.logout();
		    document.location.href = $scope.callback;
		});
	};
	
	$scope.logout = function() {		
		server.post('/api/logout')
		.then(function() { 
			session.logout();
			if ($state.includes("provider") || $state.includes("public_provider")) document.location.href="/#/provider/login";
			else if ($state.includes("research") || $state.includes("public_research")) document.location.href="/#/research/login";
			else if ($state.includes("admin") || $state.includes("developer") || $state.includes("public_developer")) document.location.href="/#/developer/login";
			else document.location.href="/#/public/login"; });
	};
			
	$scope.init();
}]);