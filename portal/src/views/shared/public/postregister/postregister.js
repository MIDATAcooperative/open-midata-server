angular.module('portal')
.controller('PostRegisterCtrl', ['$scope', '$state', '$stateParams', 'status', 'server', function($scope, $state, $stateParams, status, server) {
	
	// init
	$scope.passphrase = {};
	$scope.error = null;
	$scope.progress = $stateParams.progress;
	$scope.status = new status(false, $scope);
	$scope.status.isBusy = false;
	$scope.mailSuccess = false;
	$scope.codeSuccess = false;
	$scope.resentSuccess = false;
		
	$scope.resend = function() {
		$scope.resentSuccess = $scope.codeSuccess = $scope.mailSuccess = false;
	    $scope.status.doAction('resent', server.post(jsRoutes.controllers.Application.requestWelcomeMail().url))
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
			switch (result.data.role) {
			case "member": $state.go('member.overview');break;
			case "hpuser": $state.go('member.overview');break;
			case "research": $state.go('research.studies');break;
			case "developer": $state.go('developer.yourapps');break;	
			case "admin" : $state.go('admin.members');break;
			}
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
	    });	    
	};
	
				
	console.log($state.current.data.mode);
	if ($stateParams.token && $state.current.data.mode) {
		$scope.confirm();
	}
}]);

