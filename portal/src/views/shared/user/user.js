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
.controller('UserCtrl', ['$scope', '$state', '$translate', 'ENV', 'users', 'status', 'session', 'server', 'languages', 'crypto', function($scope, $state, $translate, ENV, users, status, session, server, languages, crypto) {
	// init
	$scope.status = new status(false, $scope);
	$scope.user = {};
	$scope.msg = null;
	$scope.beta = ENV.instanceType == "test" || ENV.instanceType == "local" || ENV.instanceType == "demo";
	$scope.error = null;
	
	$scope.languages = languages.all;
	$scope.authTypes = ["NONE", "SMS"];
	$scope.notificationTypes = ["NONE", "LOGIN"];
	
	$scope.confirmation = { code : "" };
	
	// parse user id (format: /users/:id) and load the user details
	var userId = $state.params.userId;	
	$scope.reqRole = $state.params.role;
	
	$scope.init = function() {
		$scope.status.doBusy(users.getMembers({"_id": userId}, ["name", "email", "searchable", "language", "address1", "address2", "zip", "city", "country", "firstname", "lastname", "mobile", "phone", "emailStatus", "agbStatus", "contractStatus", "role", "subroles", "confirmedAt", "birthday", "midataID", "status", "gender", "authType", "notifications"]))
		.then(function(results) {
			$scope.user = results.data[0];
			$scope.user.authType = $scope.user.authType || "NONE";
			$scope.user.notifications = $scope.user.notifications || "NONE";
		});
		
		
	};
	
	$scope.metrics = function() {
		$scope.stats = {};
		server.get("/api/shared/users/stats").then(function(results) {
			   $scope.stats = results.data;
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
		.then(function(results) { $scope.msg = "user.account_repaired";$scope.repair=results.data; });
	};
	
	$scope.resetSpaces = function() {
		$scope.msg = "Please wait...";
		server.delete(jsRoutes.controllers.Spaces.reset().url)
		.then(function() { $scope.msg = "user.spaces_resetted";$state.reload(); });
	};
	
	$scope.updateSettings = function() {
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type && $scope.myform[$scope.error.field]) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) {
			var elem = $document[0].querySelector('input.ng-invalid');
			if (elem && elem.focus) elem.focus();
			return;
		}
		
		
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