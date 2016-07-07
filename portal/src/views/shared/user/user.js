angular.module('portal')
.controller('UserCtrl', ['$scope', '$state', '$translate', 'users', 'status', 'session', 'server', 'languages', function($scope, $state, $translate, users, status, session, server, languages) {
	// init
	$scope.status = new status(true);
	$scope.user = {};
	$scope.msg = null;
	
	$scope.languages = languages.all;
	
	// parse user id (format: /users/:id) and load the user details
	var userId = $state.params.userId;	
	
	$scope.status.doBusy(users.getMembers({"_id": {"$oid": userId}}, ["name", "email", "searchable", "language", "address1", "address2", "zip", "city", "country", "firstname", "lastname", "mobile", "phone"]))
	.then(function(results) {
		$scope.user = results.data[0];
	});
	
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
	
}]);