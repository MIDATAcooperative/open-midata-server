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
.controller('AddPatientCtrl', ['$scope', '$state', 'server', 'status', 'session', '$translate', 'languages', '$document', 'dateService', 'fhir', 'crypto', function($scope, $state, server, status, session, $translate, languages, $document, dateService, fhir, crypto) {
	
	
	$scope.studyid = $state.params.studyId;
    $scope.status = new status(false, $scope);
	
	
	
	$scope.registration = { email : $state.params.email, language : $translate.use(), subroles:[] };	
	$scope.languages = languages.all;
	$scope.countries = languages.countries;
	$scope.error = null;
	
	$scope.reload = function() {
		
		
				
	};
		
	
	// register new user
	$scope.register = function() {		
		        		
		$scope.success = false;
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		
		if ($scope.registration.birthdayDay) {
	          $scope.myform.birthday.$setValidity('date', dateService.isValidDate($scope.registration.birthdayDay, $scope.registration.birthdayMonth, $scope.registration.birthdayYear));
	    }
		
		var pwvalid = !$scope.registration.password || crypto.isValidPassword($scope.registration.password); 
	    $scope.myform.password.$setValidity('tooshort', pwvalid);
	    if (!pwvalid) {
	      	$scope.myform.password.$invalid = true;
	       	$scope.myform.password.$error = { 'tooshort' : true };
	    }
				
		if (! $scope.myform.$valid) {
			var elem = $document[0].querySelector('input.ng-invalid');
			if (elem && elem.focus) elem.focus();
			return;
		}
		
		var pad = function(n){
		    return ("0" + n).slice(-2);
		};
		
        $scope.registration.birthday = $scope.registration.birthdayYear + "-" + 
                                       pad($scope.registration.birthdayMonth) + "-" +
                                       pad($scope.registration.birthdayDay);	
						        
     
        var user = $scope.registration;
		var data = {
		      "resourceType" : "Patient",
		      "active" : !$scope.registration.precreate,
		      "name" : [{
			     "family" : user.lastname,
			     "given" : [ user.firstname ]
			  }],
			  "telecom" : [],
			  "gender" : user.gender,
			  "birthDate" : user.birthday,
			  "address" : [{
			     "line" : [],			   
			     "country" : user.country
			   }],
			  "communication" : [{
			    "language" : {
			       "coding" : { "code" : user.language, "system" : "urn:ietf:bcp:47" }
			    },
			    "preferred" : true
			  }],
			  "extension" : []
			};
		if (user.email) data.telecom.push({
		     "system" : "email",
		     "value" : user.email
		});
		if (user.phone) data.telecom.push({
		     "system" : "phone",
		     "value" : user.phone
		});
		if (user.mobile) data.telecom.push({
		     "system" : "phone",
		     "value" : user.mobile
		});
		if (user.initialPassword) {
			data.extension.push({
			  "url" : "http://midata.coop/extensions/account-password",
			  "valueString" : user.initialPassword
			});
		}
		if (user.address1) data.address[0].line.push(user.address1);
		if (user.address2) data.address[0].line.push(user.address2);
		if (user.city) data.address[0].city = user.city;
		if (user.zip) data.address[0].postalCode = user.zip;
		
		$scope.status.doAction("register", fhir.post("Patient", data)).
		then(function(result) { 
           $scope.success = true;
           //if (result.data.id) {
             $state.go("^.patientsearch", { email : user.email });
           //}
       
        });
        
		
	};
		
	$scope.emailNeeded = function() {
		return false; //$scope.study && $scope.study.requirements && ($scope.study.requirements.indexOf('EMAIL_ENTERED') >= 0 ||  $scope.study.requirements.indexOf('EMAIL_VERIFIED') >=0 );
	};
	
	$scope.addressNeeded = function() {
		return false; //$scope.study && $scope.study.requirements && ($scope.study.requirements.indexOf('ADDRESS_ENTERED') >= 0 ||  $scope.study.requirements.indexOf('ADDRESS_VERIFIED') >=0 );
	};
	
	$scope.phoneNeeded = function() {
		return false;//$scope.study && $scope.study.requirements && ($scope.study.requirements.indexOf('PHONE_ENTERED') >= 0 ||  $scope.study.requirements.indexOf('PHONE_VERIFIED') >=0 );
	};
	
	
	$scope.days = [];
	$scope.months = [];
	$scope.years = [];
	var i = 0;
	for (i=1;i <= 9; i++ ) { $scope.days.push("0"+i); $scope.months.push("0"+i); }
	for (i=10;i <= 31; i++ ) $scope.days.push(""+i);	
	for (i=10;i <= 12; i++ ) $scope.months.push(""+i);
	for (i=2015;i > 1900; i-- ) $scope.years.push(""+i);	
	
	$scope.reload();
	
}]);