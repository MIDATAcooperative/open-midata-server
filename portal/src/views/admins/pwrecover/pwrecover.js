angular.module('portal')
.controller('PwRecoverCtrl', ['$scope', '$state', 'views', 'status', 'users', 'server', 'paginationService', 'session', '$window', 'crypto', function($scope, $state, views, status, users, server, paginationService, session, $window, crypto) {

	$scope.status = new status(true);
	$scope.page = { nr : 1 };
	$scope.criteria = { me : "" };
	
	$scope.reload = function(comeback) {	
		$scope.status.doBusy(server.get(jsRoutes.controllers.PWRecovery.getUnfinished().url))
		.then(function(result) {
			if (!comeback) paginationService.setCurrentPage("recovertable", 1);
			$scope.members = result.data;
		});
	};
	
	$scope.copyToClip = function(elem) {
		elem = elem.currentTarget;
		elem.focus();
		elem.select();
		
		$window.document.execCommand("copy");
	};
	
	
	$scope.commit = function(user) {
		console.log(Object.keys(user.shares).length);
		user.success = "[...]";
		if (Object.keys(user.shares).length == crypto.keysNeeded()) {
			var rec = JSON.parse(JSON.stringify(user.shares));
			rec.encrypted = user.encShares.encrypted;
			rec.iv = user.encShares.iv;
			try {
				var response = crypto.dorecover(rec, user.challenge);
				server.post(jsRoutes.controllers.PWRecovery.finishRecovery().url, JSON.stringify({ _id : user._id, session : response }))
				.then(function() {
					user.success = "["+Object.keys(user.shares).length+"/"+crypto.keysNeeded()+"]";
				});
			} catch (e) {
				console.log(e);
				user.success = null;
				user.fail = e.message;
			}
		} else {
			try {
			   server.post(jsRoutes.controllers.PWRecovery.storeRecoveryShare().url, JSON.stringify(user))
			   .then(function() {
				   user.success = "["+Object.keys(user.shares).length+"/"+crypto.keysNeeded()+"]";
			   });
			} catch (e) {
				console.log(e);
				user.success = null;
				user.fail = e.message;
			}
		   
		}
	};	
	
	
	$scope.reload(undefined, true);

}]);