angular.module('portal')
.controller('PostRegisterCtrl', ['$scope', '$state', '$stateParams', 'status', 'server', 'session', 'oauth', function($scope, $state, $stateParams, status, server, session, oauth) {
	
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
	    	  oauth.login(true);	
	    }
	};
				
	
	if ($stateParams.token && $state.current.data.mode) {
		$scope.confirm();
	}
	
	if (oauth.getAppname()) { $scope.isoauth = true; }
}]);

