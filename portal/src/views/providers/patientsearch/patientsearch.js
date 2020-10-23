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
.controller('MemberSearchCtrl', ['$scope', '$state', 'status', 'provideraccess', 'circles', 'usergroups', function($scope, $state, status, provideraccess, circles, usergroups) {
	
	$scope.criteria = {};
	$scope.newconsent = {};
	$scope.member = null;
	$scope.error = null;
	$scope.status = new status(false, $scope);
	$scope.loading = false;
	$scope.status.isBusy = false;
	$scope.searched = false;
	
	$scope.dosearch = function() {
		
		$scope.member = null;
		$scope.consents = null;
		
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;		
		if (! $scope.myform.$valid) return;
		
		$scope.loading = true;
		
		$scope.init();
		
		
		$scope.status.doBusy(provideraccess.search($scope.criteria))
		.then(function(data) { 				
			    $scope.member = data.data.member;
			    $scope.consents = data.data.consents;
			    $scope.error = null;
			    $scope.loading = false;
			    $scope.submitted = false;	
			    $scope.searched = true;
			    
			    //$state.go('^.memberdetails', { memberId : $scope.member._id });		    
		});
		
		
	};
	
	$scope.changedCriteria = function() {
		$scope.member = null;
		$scope.consents = null;				
		$scope.error = null;	
	};
	
	$scope.selectPatient = function() {
		$state.go('^.memberdetails', { user : $scope.member._id });		
	};
	
	$scope.addConsent = function() {	
		if ($scope.criteria.passcode) {
		  $scope.usePasscode();
		} else {		
		  if ($scope.member) {
		    $state.go("^.newconsent", { "authorize" : $scope.newconsent.usergroup, "owner" : $scope.member._id, "request" : true });
		  } else {
			$state.go("^.newconsent", { "authorize" : $scope.newconsent.usergroup, "extowner" : $scope.criteria.email, "request" : true });
		  }
		}
	};
	
	$scope.usePasscode = function() {
		$scope.status.doAction("usepasscode", circles.joinByPasscode($scope.member._id, $scope.criteria.passcode, $scope.newconsent.usergroup ))
	    .then(function(data) {
	    	 $state.go("^.editconsent", { consentId : data.data._id });
	    });
	};
	
	$scope.init = function() {
		$scope.status.doBusy(usergroups.search({ "member" : true, "active" : true }, usergroups.ALLPUBLIC ))
    	.then(function(results) {
		  $scope.usergroups = results.data;
    	});
	};
	
	if ($state.params.email) {
		$scope.criteria.email = $state.params.email;
	}
	
}]);