angular.module('portal')
.controller('UserCtrl', ['$scope', '$state', '$translate', 'ENV', 'users', 'status', 'session', 'server', 'languages', 'crypto', function($scope, $state, $translate, ENV, users, status, session, server, languages, crypto) {
	// init
	$scope.status = new status(false);
	$scope.user = {};
	$scope.msg = null;
	$scope.beta = ENV.instanceType == "test" || ENV.instanceType == "local";
	$scope.error = null;
	
	$scope.languages = languages.all;
	
	$scope.confirmation = { code : "" };
	
	// parse user id (format: /users/:id) and load the user details
	var userId = $state.params.userId;	
	$scope.reqRole = $state.params.role;
	
	$scope.init = function() {
		$scope.status.doBusy(users.getMembers({"_id": userId}, ["name", "email", "searchable", "language", "address1", "address2", "zip", "city", "country", "firstname", "lastname", "mobile", "phone", "emailStatus", "agbStatus", "contractStatus", "role", "subroles", "confirmedAt", "birthday", "midataID", "status", "gender"]))
		.then(function(results) {
			$scope.user = results.data[0];
		});
	};
	
	
	session.currentUser.then(function(myUserId) { 		
		
		userId = userId || myUserId;
		$scope.isSelf = myUserId == userId;
		
        $scope.init();
	});
	
	$scope.fixAccount = function() {
		$scope.msg = "Please wait...";
		server.post(jsRoutes.controllers.Records.fixAccount().url)
		.then(function() { $scope.msg = "user.account_repaired"; });
	};
	
	$scope.resetSpaces = function() {
		$scope.msg = "Please wait...";
		server.delete(jsRoutes.controllers.Spaces.reset().url)
		.then(function() { $scope.msg = "user.spaces_resetted";$state.reload(); });
	};
	
	$scope.updateSettings = function() {
		if ($scope.locked) $scope.user.searchable = false;
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
	
	$scope.exportAccount = function() {
		$scope.status.doAction("download", server.token())
		.then(function(response) {
		  document.location.href = ENV.apiurl + jsRoutes.controllers.Records.downloadAccountData().url + "?token=" + encodeURIComponent(response.data.token);
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
		
	
	$scope.accountWipe = function() {
		if (!$scope.user.password) {
			$scope.error = { code : "accountwipe.error" };
			return;
		}
		$scope.user.passwordHash = crypto.getHash($scope.user.password);
		$scope.status.doAction("wipe", server.post("/api/shared/users/wipe", JSON.stringify($scope.user))).then(function() {
		  document.location.href="/#/public/login"; 
	  },function(err) { $scope.error = err.data; });	  
	};
	
	$scope.getHello = function(label) {
		if ($scope.user.gender === "FEMALE") return label+"_w";
		if ($scope.user.gender === "MALE") return label+"_m";
		return label;
	};
	
}]);