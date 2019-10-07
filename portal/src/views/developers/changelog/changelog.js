angular.module('portal')
.controller('ChangeLogCtrl', ['$scope', '$state', 'server', 'ENV', function($scope, $state, server, ENV) {
		
	
	server.get(jsRoutes.controllers.Market.getSoftwareChangeLog().url)
	.then(function(data) { 
		$scope.changelog = data.data;
				
	});
		
}]);