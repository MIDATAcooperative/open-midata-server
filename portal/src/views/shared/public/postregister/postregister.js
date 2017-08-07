angular.module('portal')
.controller('PostRegisterCtrl', ['$scope', '$state', '$stateParams', 'status', 'server', 'session', 'oauth', 'users', function($scope, $state, $stateParams, status, server, session, oauth, users) {
	
	// init
	$scope.passphrase = {};
	$scope.error = null;
	$scope.progress = $stateParams.progress;
	$scope.status = new status(false, $scope);
	$scope.status.isBusy = false;
	$scope.mailSuccess = false;
	$scope.codeSuccess = false;
	$scope.resentSuccess = false;
	$scope.isoauth = false;
	
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
				
	for (var i in $scope.progress.requirements) {
		$scope.progress[$scope.progress.requirements[i]] = true;
	}
	$scope.registration = $scope.progress.user;
	
		
	$scope.resend = function() {	
		$scope.resentSuccess = $scope.codeSuccess = $scope.mailSuccess = false;
	    $scope.status.doAction('resent', server.post(jsRoutes.controllers.Application.requestWelcomeMail().url, JSON.stringify({ userId : $scope.progress.userId })))
	    .then(function() {
	    	$scope.resentSuccess = true;	    		    	
	    });	    
	};
	
	$scope.sendCode = function() {
		$scope.resentSuccess = $scope.codeSuccess = $scope.mailSuccess = false;
		var data = { confirmationCode : $scope.passphrase.passphrase };
		if (data.confirmationCode && data.confirmationCode.length > 0) {
	    $scope.status.doAction('code', server.post(jsRoutes.controllers.Application.confirmAccountAddress().url, JSON.stringify(data) ))
	    .then(function(result) { 
	    	$scope.codeSuccess = true;
	    	$scope.progress.confirmationCode = true;
	    	session.postLogin(result, $state);
		});	    
		}
	};
	
	$scope.changeAddress = function() {		
        
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
												
		$scope.status.doAction("changeAddress", users.updateAddress($scope.registration)).
		then(function(data) { 
			$scope.retry();
		});
	};
	
	$scope.confirm = function() {
		
		$scope.resentSuccess = $scope.codeSuccess = $scope.mailSuccess = false;
		var data = { token : $stateParams.token, mode : $state.current.data.mode };
	    $scope.status.doAction('email', server.post(jsRoutes.controllers.Application.confirmAccountEmail().url, JSON.stringify(data) ))
	    .then(function(result) {
	    	$scope.progress = result.data;	    	
	    	$scope.mailSuccess = true;	  
	    	session.postLogin(result, $state);
	    });	    
	};
	
    $scope.enterMailCode = function(code) {
		
		$scope.resentSuccess = $scope.codeSuccess = $scope.mailSuccess = false;
		var data = { code : code, mode : "VALIDATED", userId : $scope.progress.userId , role : $scope.progress.role };
	    $scope.status.doAction('email', server.post(jsRoutes.controllers.Application.confirmAccountEmail().url, JSON.stringify(data) ))
	    .then(function(result) {
	    	$scope.progress = result.data;	    	
	    	$scope.mailSuccess = true;	  
	    	
	    	if (oauth.getAppname()) {
	    	  oauth.login(true);	
	    	} else {	    	
	    	  session.postLogin(result, $state);
	    	}
	    });	    
	};
	
	$scope.retry = function() {
		if (oauth.getAppname()) {
	    	  oauth.login(true)
	    	  .then(function(result) {
	  		      if (result !== "ACTIVE") {	  			    
	  				  session.postLogin({ data : result}, $state);  
	  			  }
	  		  });	  		
	    } else {
	      $state.go("^.user",{userId:$scope.registration._id});	
	    }
		
	};
	
	$scope.addressNeeded = function() {
		return $scope.progress.requirements && ($scope.progress.requirements.indexOf('ADDRESS_ENTERED') >= 0  );
	};
	
	$scope.phoneNeeded = function() {
		return $scope.progress.requirements && ($scope.progress.requirements.indexOf('PHONE_ENTERED') >= 0  );
	};
	
	$scope.requestMembership = function() {
		$scope.status.doAction("requestmembership", users.requestMembership($scope.user))
		.then(function() {
		   $scope.retry();
		});
	};
				
	
	if ($stateParams.token && $state.current.data.mode) {
		$scope.confirm();
	}
	
	if (oauth.getAppname()) { $scope.isoauth = true; }
}]);

