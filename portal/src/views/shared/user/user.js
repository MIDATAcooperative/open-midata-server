angular.module('portal')
.controller('UserCtrl', ['$scope', '$state', '$translate', 'ENV', 'users', 'status', 'session', 'server', 'languages', function($scope, $state, $translate, ENV, users, status, session, server, languages) {
	// init
	$scope.status = new status(false);
	$scope.user = {};
	$scope.msg = null;
	$scope.beta = ENV.beta;
	
	$scope.languages = languages.all;
	
	$scope.confirmation = { code : "" };
	
	// parse user id (format: /users/:id) and load the user details
	var userId = $state.params.userId;	
	
	$scope.init = function() {
		$scope.status.doBusy(users.getMembers({"_id": {"$oid": userId}}, ["name", "email", "searchable", "language", "address1", "address2", "zip", "city", "country", "firstname", "lastname", "mobile", "phone", "emailStatus", "agbStatus", "contractStatus", "role", "subroles", "confirmedAt"]))
		.then(function(results) {
			$scope.user = results.data[0];
		});
	};
	$scope.init();
	
	session.currentUser.then(function(myUserId) { 
		$scope.isSelf = myUserId.$oid == userId;
		console.log(myUserId);
	});
	
	$scope.fixAccount = function() {
		$scope.msg = "Please wait...";
		server.post(jsRoutes.controllers.Records.fixAccount().url)
		.then(function() { $scope.msg = "user.account_repaired"; });
	};
	
	$scope.updateSettings = function() {
		$scope.status.doAction("changesettings", users.updateSettings($scope.user))
		.then(function() {
		  $scope.msgSettings = "user.change_settings_success";
		  $translate.use($scope.user.language);
		});
	};
	
	$scope.requestMembership = function() {
		$scope.status.doAction("requestmembership", users.requestMembership($scope.user))
		.then(function() {
		   $scope.init();
		});
	};
	
	$scope.sendCode = function() {
		//$scope.resentSuccess = $scope.codeSuccess = $scope.mailSuccess = false;
		$scope.confirmation.error = null;
		var data = { confirmationCode : $scope.confirmation.code };
		if (data.confirmationCode && data.confirmationCode.length > 0) {
	    $scope.status.doAction('code', server.post(jsRoutes.controllers.Application.confirmAccountAddress().url, JSON.stringify(data) ))
	    .then(function(result) { 
	    	$scope.init();
		}, function(error) {
			$scope.confirmation.error = error.data.code;
			
		});	    
		}
	};
	
	$scope.wipe = function() {
	  server.delete("/shared/api/users/wipe").then(function() {
		  document.location.href="/#/public/login"; 
	  });	  
	};
	
}]);