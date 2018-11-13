angular.module('portal')
.controller('PwRecoverCtrl', ['$scope', '$state', 'views', 'status', 'users', 'server', 'paginationService', 'session', '$window', 'crypto', function($scope, $state, views, status, users, server, paginationService, session, $window, crypto) {

	$scope.status = new status(true);
	$scope.page = { nr : 1 };
	$scope.criteria = { me : "ak" };
	
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
		if (Object.keys(user.shares).length == 2) {
			var rec = JSON.parse(JSON.stringify(user.shares));
			rec.encrypted = user.encShares.encrypted;
			rec.iv = user.encShares.iv;
			var response = crypto.dorecover(rec, user.challenge);
			server.post(jsRoutes.controllers.PWRecovery.finishRecovery().url, JSON.stringify({ _id : user._id, session : response }));
		} else {
		   server.post(jsRoutes.controllers.PWRecovery.storeRecoveryShare().url, JSON.stringify(user));
		}
	};	
	
	
	$scope.reload(undefined, true);

}]);