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
.controller('PwRecoverCtrl', ['$scope', '$state', 'views', 'status', 'users', 'server', 'paginationService', 'session', '$window', 'crypto', function($scope, $state, views, status, users, server, paginationService, session, $window, crypto) {

	$scope.status = new status(true);
	$scope.page = { nr : 1 };
	$scope.criteria = { me : "" };
	
	$scope.reload = function(comeback) {	
		$scope.status.doBusy(server.get(jsRoutes.controllers.PWRecovery.getUnfinished().url))
		.then(function(result) {
			if (!comeback) paginationService.setCurrentPage("recovertable", 1);
			$scope.members = result.data;
		});
	};
	
	$scope.copyToClip = function(elem) {
		elem = elem.currentTarget;
		elem.focus();
		elem.select();
		
		$window.document.execCommand("copy");
	};
	
	
	$scope.commit = function(user) {
		console.log(Object.keys(user.shares).length);
		user.success = "[...]";
		if (Object.keys(user.shares).length == crypto.keysNeeded()) {
			var rec = JSON.parse(JSON.stringify(user.shares));
			rec.encrypted = user.encShares.encrypted;
			rec.iv = user.encShares.iv;
			try {
				var response = crypto.dorecover(rec, user.challenge);
				server.post(jsRoutes.controllers.PWRecovery.finishRecovery().url, JSON.stringify({ _id : user._id, session : response }))
				.then(function() {
					user.success = "["+Object.keys(user.shares).length+"/"+crypto.keysNeeded()+"]";
				});
			} catch (e) {
				console.log(e);
				user.success = null;
				user.fail = e.message;
			}
		} else {
			try {
			   server.post(jsRoutes.controllers.PWRecovery.storeRecoveryShare().url, JSON.stringify(user))
			   .then(function() {
				   user.success = "["+Object.keys(user.shares).length+"/"+crypto.keysNeeded()+"]";
			   });
			} catch (e) {
				console.log(e);
				user.success = null;
				user.fail = e.message;
			}
		   
		}
	};	
	
	
	$scope.reload(undefined, true);

}]);