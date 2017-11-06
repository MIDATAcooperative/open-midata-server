angular.module('portal')
.controller('AddParticipantCtrl', ['$scope', '$state', 'server', 'status', 'session', '$translate', 'languages', '$document', 'dateService', 'fhir', function($scope, $state, server, status, session, $translate, languages, $document, dateService, fhir) {
	
	
	$scope.studyid = $state.params.studyId;
    $scope.status = new status(false, $scope);
	
	
	
	$scope.registration = { language : $translate.use(), subroles:[] };	
	$scope.languages = languages.all;
	$scope.countries = languages.countries;
	$scope.error = null;
	
	$scope.reload = function() {
		
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url))
		.then(function(data) { 				
			$scope.study = data.data;	
		});
				
	};
		
	
	// register new user
	$scope.register = function() {		
		        
		$scope.myform.agb.$setValidity('mustaccept', $scope.registration.agb);
	    $scope.myform.privacypolicy.$setValidity('mustaccept', $scope.registration.privacypolicy);  
	    if (!$scope.registration.agb) {
	        	
	        	$scope.myform.agb.$invalid = true;
	        	$scope.myform.agb.$error = { 'mustaccept' : true };
	    }
	    if (!$scope.registration.privacypolicy) {
        	
        	$scope.myform.privacypolicy.$invalid = true;
        	$scope.myform.privacypolicy.$error = { 'mustaccept' : true };
    }
		
		$scope.success = false;
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		
		if ($scope.registration.birthdayDay) {
	          $scope.myform.birthday.$setValidity('date', dateService.isValidDate($scope.registration.birthdayDay, $scope.registration.birthdayMonth, $scope.registration.birthdayYear));
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
		      "active" : true,
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
			  "extension" : [			   
			   { 
			      "url" : "http://midata.coop/extensions/terms-agreed",
			      "valueString" : "midata-terms-of-use--1.0"
			    },
			   { 
			      "url" : "http://midata.coop/extensions/terms-agreed",
			      "valueString" : "midata-privacy-policy--1.0"
			    },
			    { 
				  "url" : "http://midata.coop/extensions/join-study",
				  "valueCoding" : { "code" : $scope.study.code, "system" : "http://midata.coop/codesystems/study-code" }
				}
			  ]
	
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
		if (user.address1) data.address[0].line.push(user.address1);
		if (user.address2) data.address[0].line.push(user.address2);
		if (user.city) data.address[0].city = user.city;
		if (user.zip) data.address[0].postalCode = user.zip;
		
		$scope.status.doAction("register", fhir.post("Patient", data)).
		then(function(data) { 
           $scope.success = true;
           $state.go("^.participants");
		});
	};
		
	$scope.emailNeeded = function() {
		return $scope.study && $scope.study.requirements && ($scope.study.requirements.indexOf('EMAIL_ENTERED') >= 0 ||  $scope.study.requirements.indexOf('EMAIL_VERIFIED') >=0 );
	};
	
	$scope.addressNeeded = function() {
		return $scope.study && $scope.study.requirements && ($scope.study.requirements.indexOf('ADDRESS_ENTERED') >= 0 ||  $scope.study.requirements.indexOf('ADDRESS_VERIFIED') >=0 );
	};
	
	$scope.phoneNeeded = function() {
		return $scope.study && $scope.study.requirements && ($scope.study.requirements.indexOf('PHONE_ENTERED') >= 0 ||  $scope.study.requirements.indexOf('PHONE_VERIFIED') >=0 );
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