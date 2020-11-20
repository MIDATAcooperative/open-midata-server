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
.controller('PostRegisterCtrl', ['$scope', '$state', '$stateParams', 'status', 'server', 'session', 'oauth', 'users', 'views', 'crypto', 'ENV', 'dateService', function($scope, $state, $stateParams, status, server, session, oauth, users, views, crypto, ENV, dateService) {
	
	// init
	$scope.passphrase = {};
	$scope.setpw = {};
	$scope.error = null;
	$scope.progress = $stateParams.progress || {};
	$scope.status = new status(false, $scope);
	$scope.status.isBusy = false;
	$scope.mailSuccess = false;
	$scope.codeSuccess = false;
	$scope.resentSuccess = false;
	$scope.isoauth = false;
	$scope.view = views.getView("terms");
	$scope.ENV = ENV;
    	
	$scope.addressNeeded = function() {
		return $scope.progress.requirements && ($scope.progress.requirements.indexOf('ADDRESS_ENTERED') >= 0  );
	};
	
	$scope.phoneNeeded = function() {
		return $scope.progress.requirements && ($scope.progress.requirements.indexOf('PHONE_ENTERED') >= 0  );
	};
	
	$scope.addAddressParams = function() {
		if ($scope.addressNeeded()) {
			if ($state.params.street) $scope.registration.address1 = $state.params.street;
			if ($state.params.city) $scope.registration.city = $state.params.city;
			if ($state.params.zip) $scope.registration.zip = $state.params.zip;
		}
		if ($scope.phoneNeeded()) {
			if ($state.params.phone) $scope.registration.phone = $state.params.phone;
			if ($state.params.mobile) $scope.registration.mobile = $state.params.mobile;
		}
	};
	
	$scope.init = function() {
	if ($stateParams.feature) {
		$scope.progress = { requirements : [ $stateParams.feature ] };
		
		session.currentUser.then(function (userId) {
			users.getMembers({"_id": userId}, ["name", "email", "searchable", "language", "address1", "address2", "zip", "city", "country", "firstname", "lastname", "mobile", "phone", "emailStatus", "agbStatus", "contractStatus", "role", "subroles", "confirmedAt"])
			.then(function(results) {
				$scope.registration = results.data[0];
				$scope.progress.emailStatus = $scope.registration.emailStatus;
				$scope.progress.agbStatus = $scope.registration.agbStatus;
				$scope.progress.contractStatus = $scope.registration.contractStatus;
				$scope.addAddressParams();
			});
		});
	}
	};
	$scope.init();
		
	$scope.setFlags = function() {
	if ($scope.progress && $scope.progress.requirements) {
		for (var i in $scope.progress.requirements) {
			$scope.progress[$scope.progress.requirements[i]] = true;
		}
		$scope.registration = $scope.progress.user;
		$scope.addAddressParams();
	}
	};
	$scope.setFlags();
		
	$scope.resend = function() {	
		$scope.resentSuccess = $scope.codeSuccess = $scope.mailSuccess = false;
		$scope.error = null;
	    $scope.status.doAction('resent', server.post(jsRoutes.controllers.Application.requestWelcomeMail().url, JSON.stringify({ userId : $scope.progress.userId })))
	    .then(function() {
	    	$scope.resentSuccess = true;	    		    	
	    });	    
	};
	
	$scope.sendCode = function() {
		$scope.resentSuccess = $scope.codeSuccess = $scope.mailSuccess = false;
		$scope.error = null;
		var data = { confirmationCode : $scope.passphrase.passphrase };
		if (data.confirmationCode && data.confirmationCode.length > 0) {
	    $scope.status.doAction('code', server.post(jsRoutes.controllers.Application.confirmAccountAddress().url, JSON.stringify(data) ))
	    .then(function(result) { 
	    	$scope.codeSuccess = true;
	    	$scope.progress.confirmationCode = true;
	    	$scope.retry(result);	    	
		});	    
		}
	};
	
	$scope.changeAddress = function() {		
        if (!$scope.addressNeeded()) {
        	$scope.registration = JSON.parse(JSON.stringify($scope.registration));
        	$scope.registration.firstname = $scope.registration.lastname = $scope.registration.gender = $scope.registration.city = $scope.registration.zip = $scope.registration.country = $scope.registration.address1 = undefined;
        }
        $scope.registration.authType = undefined;
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		
		if (! $scope.myform.$valid) return;
		//console.log($scope.registration);
		$scope.registration.user = $scope.registration._id;									
		$scope.status.doAction("changeAddress", users.updateAddress($scope.registration)).
		then(function(data) { 
			$scope.retry();
		});
	};
	
	$scope.changeBirthday = function() {		
        
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myformb[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		
		
        var d = $scope.registration.birthdayDate;
        var pad = function(n){
		    return ("0" + n).slice(-2);
		};
		
		var dparts = d.split("\.");
		if (dparts.length != 3 || !dateService.isValidDate(dparts[0],dparts[1],dparts[2])) {
		  $scope.myformb.birthday.$setValidity('date', false);
		  return;
		} else {
			if (dparts[2].length==2) dparts[2] = "19"+dparts[2];
			$scope.registration.birthday = dparts[2]+"-"+pad(dparts[1])+"-"+pad(dparts[0]);
			//console.log($scope.registration.birthday);
		}					
		
		if (! $scope.myformb.$valid) return;
		var upd = { user : $scope.registration._id, birthday : $scope.registration.birthday};
											
		$scope.status.doAction("changeAddress", server.post(jsRoutes.controllers.admin.Administration.changeBirthday().url, JSON.stringify(upd))).
		then(function(data) { 
			$scope.retry();
		});
	};
	
	$scope.changeAuthType = function() {		
        
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform2[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		
		if (! $scope.myform2.$valid) return;
		
		$scope.registration.user = $scope.registration._id;
		if ($scope.registration.mobile === "") $scope.registration.mobile = undefined;
		$scope.status.doAction("changeAddress", users.updateAddress({ user : $scope.registration._id, authType : $scope.registration.authType, mobile : $scope.registration.mobile, emailnotify : $scope.registration.emailnotify })).
		then(function(data) { 
			$scope.retry();
		});
	};
	
	$scope.confirm = function(forceConfirm) {
		$scope.error = null;
		$scope.resentSuccess = $scope.codeSuccess = $scope.mailSuccess = false;
		var data = { token : $stateParams.token, mode : $state.current.data.mode };
		if (forceConfirm) data.mode = "VALIDATED";
	    $scope.status.doAction('email', server.post(jsRoutes.controllers.Application.confirmAccountEmail().url, JSON.stringify(data) ))
	    .then(function(result) {
	    	$scope.progress = result.data;	 
	    	if (result.data.emailStatus !== "UNVALIDATED" && (!result.data.requirements || result.data.requirements.indexOf("PASSWORD_SET") < 0)) {
	    	  $scope.mailSuccess = true;	  
	    	  //session.postLogin(result, $state);
	    	} else {
	    		$scope.setFlags();
	    	}
	    });	    
	};
	
    $scope.enterMailCode = function(code) {
    	$scope.error = null;
		$scope.resentSuccess = $scope.codeSuccess = $scope.mailSuccess = false;
		var data = { code : code, mode : "VALIDATED", userId : $scope.progress.userId , role : $scope.progress.role };
	    $scope.status.doAction('email', server.post(jsRoutes.controllers.Application.confirmAccountEmail().url, JSON.stringify(data) ))
	    .then(function(result) {
	    	$scope.retry(result);	    	
	    });	    
	};
	
	$scope.pwsubmit = function() {
		// check user input
		if (!$scope.setpw.password) {
			$scope.error = { code : "error.missing.newpassword" };
			return;
		}
		if (!$scope.setpw.passwordRepeat || $scope.setpw.passwordRepeat !== $scope.setpw.password) {
			$scope.error = { code : "error.invalid.password_repetition" };
			return;
		}
		var pwvalid = crypto.isValidPassword($scope.setpw.password); 
        
        if (!pwvalid) {
        	$scope.error = { code : "error.tooshort.password" };
        	return;
        }
		
		$scope.error = null;
		// send the request
		
		crypto.generateKeys($scope.setpw.password).then(function(keys) {
				
			if ($scope.registration.secure) {
				  $scope.registration.password = keys.pw_hash;	
				  $scope.registration.pub = keys.pub;
				  $scope.registration.priv_pw = keys.priv_pw;
				  $scope.registration.recoverKey = keys.recoverKey;
				  $scope.registration.recovery = keys.recovery;
			} else {
				  $scope.registration.password = $scope.registration.password1;
			};	
						
			var data = { token : $stateParams.token, mode : $state.current.data.mode };	
			if ($scope.setpw.secure) {
			   data.password = keys.pw_hash;
			   data.pub = keys.pub;
			   data.priv_pw = keys.priv_pw;
			   data.recovery = keys.recovery;
			   data.recoverKey = keys.recoverKey;
			} else {
				data.password = $scope.setpw.password;
			}
				
			$scope.status.doAction('setpw', server.post(jsRoutes.controllers.Application.confirmAccountEmail().url, JSON.stringify(data)))
			.then(function(result) {
				if (result.data.challenge) {
				   
				} else {				
			   	   $scope.retry(result);
				}
			});
		});
	};
	
	$scope.retry = function(funcresult, params) {
		/*if (oauth.getAppname()) {
	    	  oauth.login(true)
	    	  .then(function(result) {
	  		      if (result !== "ACTIVE") {	  			    
	  				  session.postLogin({ data : result}, $state);  
	  			  }
	  		  }, function(error) {
	  			  $scope.error = error.data;
	  		  });
		} else*/ if (funcresult) {
		   if (funcresult.data.istatus === "ACTIVE") oauth.postLogin(funcresult);
		   else session.postLogin(funcresult, $state);		
	    } else {
	      var r = session.retryLogin(params);
	      if (!r) {	    	
		      try {
		        $state.go("^.user",{userId:$scope.registration._id});
		      } catch(e) {
		    	$state.go("^.login");
		      }
	      } else {
	    	  r.then(function(result) {
	    		  if (result.data.istatus === "ACTIVE") oauth.postLogin(result);
	    		  else session.postLogin(result, $state);
	    	  }, function(err) {
	    		  $scope.setpw = {};
	    		  $scope.error = err.data;
	    		  
	    		  if (err.data && err.data.code == "error.expired.securitytoken") {
	    			  $scope.progress = { RELOGIN : true };
	    		  }
	    	  });
	      }
	    }
		
	};
	
	$scope.setSecurityToken = function() {		
		$scope.retry(null, { securityToken : $scope.setpw.securityToken });
	};
	
	$scope.noSecurityToken = function() {		
		$scope.retry(null, { securityToken : "_FAIL" });
	};
	
	
	
	$scope.requestMembership = function() {
		$scope.error = null;
		$scope.status.doAction("requestmembership", users.requestMembership($scope.user))
		.then(function() {
		   $scope.retry();
		});
	};
		
	$scope.terms = function(def) {
		console.log("TERMS");
		views.setView("terms", def, "Terms");
	};
	
	$scope.removeDateError = function() {
		$scope.myformb.birthday.$setValidity('date', true);
	};
	
	$scope.agreedToTerms = function(terms) {
		var data = { terms : terms, app : oauth.getAppname() ? oauth.app._id : null };
		$scope.status.doAction('terms', server.post(jsRoutes.controllers.Terms.agreedToTerms().url, JSON.stringify(data) ))
		.then(function(result) {
	    	$scope.retry(result);	    	
	    });	
	};
	
	
	
	if ($stateParams.token && $state.current.data.mode) {
		$scope.tokenIncluded = true;
		if ($state.current.data.mode=="REJECTED") {
		   $scope.progress = { REJECT : true };
		} else $scope.confirm();
	}
	
	if (oauth.getAppname()) { $scope.isoauth = true; }
}]);

