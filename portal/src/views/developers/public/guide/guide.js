angular.module('portal')
.controller('GuideCtrl', ['$scope', '$state', 'server', function($scope, $state, server) {

	$scope.formats = [];
	$scope.groups = [];
	$scope.contents = [];
	
	server.get(jsRoutes.controllers.FormatAPI.listFormats().url)
	.then(function(data) { $scope.formats = data.data; });
	
	server.get(jsRoutes.controllers.FormatAPI.listContents().url)
	.then(function(data) { $scope.contents = data.data; });
	
	server.get(jsRoutes.controllers.FormatAPI.listGroups().url)
	.then(function(data) { $scope.groups = data.data; });
		
}]);