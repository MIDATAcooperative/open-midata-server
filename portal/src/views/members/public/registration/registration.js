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
.controller('RegistrationCtrl', ['$scope', '$state', 'server', 'status', 'session', '$translate', 'languages', '$stateParams', 'oauth', '$document', 'views', 'dateService', '$window', 'crypto', function($scope, $state, server, status, session, $translate, languages, $stateParams, oauth, $document, views, dateService, $window, crypto) {
	
	$scope.registration = { language : $translate.use(), confirmStudy : [] };
	$scope.languages = languages.all;
	$scope.countries = languages.countries;
	$scope.error = null;
	$scope.flags = {};
	$scope.status = new status(false, $scope);
	$scope.genders = ["FEMALE","MALE","OTHER"];
	
	$scope.offline = (window.jsRoutes === undefined) || (window.jsRoutes.controllers === undefined);
	
	$scope.view = views.getView("terms");
	
	server.get(jsRoutes.controllers.Terms.currentTerms().url).then(function(result) { $scope.currentTerms = result.data; });
				
	// register new user
	$scope.register = function() {		
		
        $scope.myform.password.$setValidity('compare', $scope.registration.password1 ==  $scope.registration.password2);
        $scope.myform.agb.$setValidity('mustaccept', $scope.registration.agb);        
        if (!$scope.registration.agb) {
        	
        	$scope.myform.agb.$invalid = true;
        	$scope.myform.agb.$error = { 'mustaccept' : true };
        }

        $scope.myform.birthday.$setValidity('date', true);
        if ($scope.registration.birthdayDay) {
          $scope.myform.birthday.$setValidity('date', dateService.isValidDate($scope.registration.birthdayDay, $scope.registration.birthdayMonth, $scope.registration.birthdayYear));
        }
        
        /*
        if ($scope.app && $scope.app.termsOfUse) {
        	$scope.myform.appAgb.$setValidity('mustaccept', $scope.registration.appAgb);
            if (!$scope.registration.appAgb) {        	
	        	$scope.myform.appAgb.$invalid = true;
	        	$scope.myform.appAgb.$error = { 'mustaccept' : true };
            }
		}
		*/
        
        var pwvalid = crypto.isValidPassword($scope.registration.password1); 
        $scope.myform.password.$setValidity('tooshort', pwvalid);
        if (!pwvalid) {
        	$scope.myform.password.$invalid = true;
        	$scope.myform.password.$error = { 'tooshort' : true };
        }
        
        /*
        if ($scope.links) {
	        for (var i=0;i<$scope.links.length;i++) {
				console.log($scope.links[i]);
				if ($scope.links[i].type.indexOf("OFFER_P") >=0 && $scope.links[i].type.indexOf("REQUIRE_P")>=0 && $scope.registration.confirmStudy.indexOf($scope.links[i].studyId) < 0) {
					if ($scope.links[i].linkTargetType == "ORGANIZATION") {
					   $scope.error = { code : "error.missing.consent_accept" };
					} else {
					   $scope.error = { code : "error.missing.study_accept" };
					}
					return;
				}
			}
		}
		*/
       

		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type && $scope.myform[$scope.error.field]) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) {
			var elem = $document[0].querySelector('input.ng-invalid');
			if (elem && elem.focus) elem.focus();			
			return;
		}
		
		if ($scope.registration.password1 !=  $scope.registration.password2) {
			$scope.error = { code : "error.invalid.password_repetition" };			
			return;
		}		
				
		var pad = function(n){
		    return ("0" + n).slice(-2);
		};
		
		var d = $scope.registration.birthdayDate;
		
		if (d) {
			var dparts = d.split("\.");
			if (dparts.length != 3 || !dateService.isValidDate(dparts[0],dparts[1],dparts[2])) {
			  $scope.myform.birthday.$setValidity('date', false);
			  return;
			} else {
				if (dparts[2].length==2) dparts[2] = "19"+dparts[2];
				$scope.registration.birthday = dparts[2]+"-"+pad(dparts[1])+"-"+pad(dparts[0]);				
			}
			
			//$scope.registration.birthday = d.getFullYear()+"-"+pad(d.getMonth()+1)+"-"+pad(d.getDate());
		} else {
        $scope.registration.birthday = $scope.registration.birthdayYear + "-" + 
                                       pad($scope.registration.birthdayMonth) + "-" +
                                       pad($scope.registration.birthdayDay);
		}
		// send the request
		var data = $scope.registration;	
		
		if ($stateParams.developer) {
			data.developer = $stateParams.developer;
		}
		
		
		var finishRegistration = function() { 
			if (oauth.getAppname()) {		  
			  data.app = oauth.getAppname();
			  data.device = oauth.getDevice();
			  if ($scope.registration.unlockCode) oauth.setUnlockCode($scope.registration.unlockCode);
			  if ($state.params.joincode) {
				  oauth.setJoinCode($state.params.joincode);
				  data.joinCode = $state.params.joincode;
			  }
			  
			  $scope.status.doAction("register", server.post(jsRoutes.controllers.QuickRegistration.register().url, JSON.stringify(data))).
			  then(function(datax) { 			 
				  oauth.setUser($scope.registration.email, $scope.registration.password1);			  
				  $scope.welcomemsg = true;	
				  
				  if ($scope.app && $scope.app.requirements && $scope.app.requirements.indexOf('EMAIL_VERIFIED') >= 0) {
					  $scope.confirmWelcome(); 
				  }
			  });
			  
			} else {
			
			  $scope.status.doAction("register", server.post(jsRoutes.controllers.Application.register().url, JSON.stringify(data))).
			  then(function(data) { session.postLogin(data, $state); });
			}
			
		};
				
		$scope.status.doAction("register", crypto.generateKeys($scope.registration.password1)).then(function(keys) {				
			if ($scope.registration.secure) {
			  $scope.registration.password = keys.pw_hash;	
			  $scope.registration.pub = keys.pub;
			  $scope.registration.priv_pw = keys.priv_pw;
			  $scope.registration.recoverKey = keys.recoverKey;
			  $scope.registration.recovery = keys.recovery;
			} else {
			  $scope.registration.password = $scope.registration.password1;
			};			
		    finishRegistration();						
		});
		
		
				
	};
	
	$scope.confirmWelcome = function() {
		$scope.status.doAction("register", oauth.login(false)) 
	    .then(function(result) {			 
			  if (result === "CONFIRM" || result === "CONFIRM-STUDYOK") {
				$state.go("^.oauthconfirm", $state.params);
			  }
			  if (result !== "ACTIVE") { session.postLogin({ data : result}, $state);}
		})
		.catch(function(err) { 
			$scope.error = err.data;
			session.failurePage($state, err.data);
		});			
	};
	
	$scope.changeLanguage = function(lang) {
		$translate.use(lang);
	};


	$scope.addressNeeded = function() {
		return $scope.app && $scope.app.requirements && ($scope.app.requirements.indexOf('ADDRESS_ENTERED') >= 0 ||  $scope.app.requirements.indexOf('ADDRESS_VERIFIED') >=0 );
	};
	
	$scope.phoneNeeded = function() {
		return $scope.app && $scope.app.requirements && ($scope.app.requirements.indexOf('PHONE_ENTERED') >= 0 ||  $scope.app.requirements.indexOf('PHONE_VERIFIED') >=0 );
	};
	
	$scope.terms = function(def) {		
		views.setView("terms", def, "Terms");
	};
	
	$scope.toggle = function(array,itm) {		
		var pos = array.indexOf(itm);
		if (pos < 0) array.push(itm); else array.splice(pos, 1);
	};
	
	$scope.removeDateError = function() {
		$scope.myform.birthday.$setValidity('date', true);
	};
	
	$scope.days = [];

	$scope.months = [];
	var i = 0;
	for (i=1;i <= 9; i++ ) { $scope.months.push("0"+i); }
	for (i=10;i <= 12; i++ ) $scope.months.push(""+i);
	
	if (oauth.getAppname()) {		
	   $scope.app = oauth.app;
	   $scope.links = oauth.links;
	}
	
	$scope.back = function() {
		$window.history.back();
	};
	/*
	$scope.getLinkLabel = function(link) {
		if (link.linkTargetType == "ORGANIZATION") {
			if (link.type.indexOf("REQUIRE_P") >= 0) return "oauth2.confirm_provider";
			return "oauth2.confirm_provider_opt";
		} 
		if (link.study.type == "CLINICAL") {
			if (link.type.indexOf("REQUIRE_P") >= 0) return "oauth2.confirm_study";
			return "oauth2.confirm_study_opt";
		}
		if (link.study.type == "CITIZENSCIENCE") return "oauth2.confirm_citizen_science";		
		if (link.study.type == "COMMUNITY") {
			if (link.type.indexOf("REQUIRE_P") >= 0) return "oauth2.confirm_community";
			return "oauth2.confirm_community_opt";
		}
		
	};
    */
	if ($state.params.login) {
		$scope.registration.email = $state.params.login;
		$scope.action = $state.params.action;
		$scope.login = $state.params.login;
		$scope.isNew = true;
	}
	if ($state.params.given) $scope.registration.firstname = $state.params.given;	
	if ($state.params.family) $scope.registration.lastname = $state.params.family;
	if ($state.params.gender) $scope.registration.gender = $state.params.gender;
	if ($state.params.country) $scope.registration.country = $state.params.country;
	
	if ($state.params.language) {
		$scope.registration.language = $state.params.language;
		$scope.changeLanguage($state.params.language);
	}
	if ($state.params.birthdate) {
		var d = new Date($state.params.birthdate);
		$scope.registration.birthdayDate = d.getDate()+"."+(1+d.getMonth())+"."+d.getFullYear();
		/*$scope.registration.birthdayDay = d.getDate();
		$scope.registration.birthdayMonth = $scope.months[d.getMonth()];
		console.log("DATE:"+$scope.registration.birthdayMonth);
		$scope.registration.birthdayYear = d.getFullYear();*/
	}
	if ($scope.addressNeeded()) {
		if ($state.params.city) $scope.registration.city = $state.params.city;
		if ($state.params.zip) $scope.registration.zip = $state.params.zip;
		if ($state.params.street) $scope.registration.address1 = $state.params.street;
	}
	if ($scope.addressNeeded() || $scope.phoneNeeded()) {
		if ($state.params.phone) $scope.registration.phone = $state.params.phone;
		if ($state.params.mobile) $scope.registration.mobile = $state.params.mobile;
	}
	
}]);
