angular.module('portal')
.controller('PassphraseCtrl', ['$scope', '$state', 'server', function($scope, $state, server) {
	
	// init
	$scope.passphrase = {};
	$scope.error = null;
		
	// submit
	$scope.submit = function() {
		// check user input
		if (!$scope.passphrase.passphrase) {
			$scope.error = { code : "error.missing.passphrase" };
			return;
		}
		
		$scope.error = null;
		
		// send the request
		var data = { "passphrase": $scope.passphrase.passphrase, "role" : $state.current.data.role };
		server.post(jsRoutes.controllers.Application.providePassphrase().url, JSON.stringify(data)).
			success(function() { 
				switch ($state.current.data.role) {
				case "member": $state.go('member.overview');break;
				case "hpuser": $state.go('member.overview');break;
				case "research": $state.go('research.studies');break;
				case "developer": $state.go('developer.yourapps');break;	
				case "admin" : $state.go('admin.members');break;
				}
			}).
			error(function(err) { $scope.error = err; });
	};
			
}]);

