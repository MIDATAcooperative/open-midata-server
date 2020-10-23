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
.controller('PassphraseCtrl', ['$scope', '$state', 'server', 'session', function($scope, $state, server, session) {
	
	// init
	$scope.passphrase = {};
	$scope.error = null;
		
	// submit
	$scope.submit = function() {
		// check user input
		if (!$scope.passphrase.passphrase) {
			$scope.error = { code : "error.missing.passphrase" };
			return;
		}
		
		$scope.error = null;
		
		// send the request
		var data = { "passphrase": $scope.passphrase.passphrase, "role" : $state.current.data.role };
		server.post(jsRoutes.controllers.Application.providePassphrase().url, JSON.stringify(data)).
			then(function() { 
				var result = { data : { role : $state.current.data.role }};
				session.postLogin(result, $state);
				/*switch ($state.current.data.role) {
				case "member": $state.go('member.overview');break;
				case "hpuser": $state.go('member.overview');break;
				case "research": $state.go('research.studies');break;
				case "developer": $state.go('developer.yourapps');break;	
				case "admin" : $state.go('admin.members');break;
				}*/
			}, function(err) { $scope.error = err.data; });
	};
			
}]);

