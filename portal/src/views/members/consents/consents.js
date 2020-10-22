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
.controller('ConsentsCtrl', ['$scope', '$state', 'circles', 'session', 'views', 'status', function($scope, $state, circles, session, views, status) {

	$scope.status = new status(true);
	$scope.role = $state.current.data.role;
	$scope.sortby="-dateOfCreation";  
		
	loadConsents = function(userId) {	
		var prop = {};
		if ($state.current.types) prop = { type : $state.current.types };
		$scope.status.doBusy(circles.listConsents(prop, [ "name", "authorized", "type", "status", "records", "dateOfCreation" ]))
		.then(function(data) {
			$scope.consents = data.data;						
		});
	};
	
	$scope.addConsent = function() {
		$state.go("^.newconsent");
	};
	
	$scope.editConsent = function(consent) {
		$state.go("^.editconsent", { consentId : consent._id });
	};
	
	$scope.changeView = function() {
		$state.go("^.revconsents");
	};
	
	$scope.setSort = function(key) {		
		if ($scope.sortby==key) $scope.sortby = "-"+key;
		else { $scope.sortby = key; }
	};
	
	session.currentUser.then(function(userId) { loadConsents(userId); });
	

}]);