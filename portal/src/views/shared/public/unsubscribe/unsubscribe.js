angular.module('portal')
.controller('UnsubscribeCtrl', ['$scope', 'server', '$location', function($scope, server, $location) {
	
	$scope.error = null;    
	var data = { token : $location.search().token };
	server.post(jsRoutes.controllers.BulkMails.unsubscribe().url, JSON.stringify(data))
	.then(function() { $scope.success = true; }, function(err) { $scope.error = err.data; });
				
}]);