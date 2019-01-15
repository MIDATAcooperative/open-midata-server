angular.module('portal')
.controller('PostRegisterCtrl', ['$scope', '$state', '$stateParams', 'status', 'server', 'session', 'oauth', 'users', 'views', 'crypto', function($scope, $state, $stateParams, status, server, session, oauth, users, views, crypto) {
	
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
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
		$scope.registration.user = $scope.registration._id;									
		$scope.status.doAction("changeAddress", users.updateAddress($scope.registration)).
		then(function(data) { 
			$scope.retry();
		});
	};
	
	$scope.confirm = function() {
		$scope.error = null;
		$scope.resentSuccess = $scope.codeSuccess = $scope.mailSuccess = false;
		var data = { token : $stateParams.token, mode : $state.current.data.mode };
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
		$scope.error = null;
		// send the request
		
		crypto.generateKeys($scope.setpw.password).then(function(keys) {
				
			var data = { token : $stateParams.token, mode : $state.current.data.mode };		
			data.password = keys.pw_hash;
			data.pub = keys.pub;
			data.priv_pw = keys.priv_pw;
			data.recovery = keys.recovery;
				
			$scope.status.doAction('setpw', server.post(jsRoutes.controllers.Application.confirmAccountEmail().url, JSON.stringify(data)))
			.then(function(result) {
			   	$scope.retry(result);	    	
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
		   if (funcresult.data.istatus === "ACTIVE") oauth.postLogin(result);
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
	    		  $scope.error = err.data;
	    	  });
	      }
	    }
		
	};
	
	$scope.setSecurityToken = function() {		
		$scope.retry(null, { securityToken : $scope.setpw.securityToken });
	};
	
	$scope.addressNeeded = function() {
		return $scope.progress.requirements && ($scope.progress.requirements.indexOf('ADDRESS_ENTERED') >= 0  );
	};
	
	$scope.phoneNeeded = function() {
		return $scope.progress.requirements && ($scope.progress.requirements.indexOf('PHONE_ENTERED') >= 0  );
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
	
	$scope.agreedToTerms = function(terms) {
		var data = { terms : terms, app : oauth.getAppname() ? oauth.app._id : null };
		$scope.status.doAction('terms', server.post(jsRoutes.controllers.Terms.agreedToTerms().url, JSON.stringify(data) ))
		.then(function(result) {
	    	$scope.retry(result);	    	
	    });	
	};
	
	if ($stateParams.token && $state.current.data.mode) {
		$scope.confirm();
	}
	
	if (oauth.getAppname()) { $scope.isoauth = true; }
}]);

