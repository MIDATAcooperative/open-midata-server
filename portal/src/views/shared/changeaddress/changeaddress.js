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
.controller('ChangeAddressCtrl', ['$scope', '$state', 'status', 'users', 'session', 'dateService', '$q', function($scope, $state, status, users, session, dateService, $q) {
	// init
	$scope.error = null;
	$scope.status = new status(false, $scope);
		
	$scope.birthdayChanged = false;
	$scope.addressChanged = false;
	
	var pad = function(n){
	    return ("0" + n).slice(-2);
	};
	
	$scope.months = [];
	var i = 0;
	for (i=1;i <= 9; i++ ) { $scope.months.push("0"+i); }
	for (i=10;i <= 12; i++ ) $scope.months.push(""+i);
	
	session.currentUser.then(function(myUserId) { 
		$scope.userId = $state.params.userId || myUserId;
		$scope.status.doBusy(users.getMembers({"_id": $scope.userId }, ["name", "email", "gender", "address1", "address2", "zip", "city", "country", "firstname", "lastname", "mobile", "phone", "birthday"]))
		.then(function(results) {
			$scope.registration = results.data[0];
			if ($scope.registration.birthday) {
				var bday = new Date($scope.registration.birthday);
				$scope.registration.birthdayYear = bday.getFullYear();
				$scope.registration.birthdayDay = bday.getDate();
				$scope.registration.birthdayMonth = pad(bday.getMonth()+1);
			}
		});
		
	});
	
	$scope.adrChange = function() {
		$scope.addressChanged = true;
	};
	
	$scope.birthChange = function() {
		$scope.birthdayChanged = true;
	};
	
    $scope.changeAddress = function() {		
		        
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		
		if ($scope.registration.birthdayDay) {
	          $scope.myform.birthday.$setValidity('date', dateService.isValidDate($scope.registration.birthdayDay, $scope.registration.birthdayMonth, $scope.registration.birthdayYear));
	    }
		
		if (! $scope.myform.$valid) return;
		
		
		
		if ($scope.registration.birthdayYear) {
             $scope.registration.birthday = $scope.registration.birthdayYear + "-" + 
                                       pad($scope.registration.birthdayMonth) + "-" +
                                       pad($scope.registration.birthdayDay);
		}
		
		var q = $q.when(); 
		$scope.registration.user = $scope.registration._id;
		
		if ($scope.birthdayChanged) {		
			q = q.then(function() { return $scope.status.doAction("changeAddress", users.updateBirthday($scope.registration)); });
		}
		if ($scope.addressChanged) {
			q = q.then(function() { return $scope.status.doAction("changeAddress", users.updateAddress($scope.registration)); });
		}
        q.then(function() {	
        	$scope.success = true;
			//$state.go("^.user", { userId : $scope.registration._id });
		});
	};
	
	$scope.changeEmail = function() {
		console.log("XXX");
		$state.go("^.changeemail", { userId : $scope.registration._id });
	};
			
}]);