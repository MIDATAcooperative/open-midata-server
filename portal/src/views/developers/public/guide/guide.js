angular.module('portal')
.controller('GuideCtrl', ['$scope', '$state', 'server', 'ENV', function($scope, $state, server, ENV) {

	$scope.formats = [];
	$scope.groups = [];
	$scope.contents = [];
	$scope.ENV = ENV;
	
	var nameMap = {};
	
	server.get(jsRoutes.controllers.FormatAPI.listFormats().url)
	.then(function(data) { $scope.formats = data.data; });
	
	server.get(jsRoutes.controllers.FormatAPI.listContents().url)
	.then(function(data) { 
		$scope.contents = data.data;
		angular.forEach($scope.contents, function(content) {
			content.code = $scope.getCode(content.content);
		});
	});
	
	server.get(jsRoutes.controllers.FormatAPI.listGroups().url)
	.then(function(data) { 
		$scope.groups = data.data;
		angular.forEach($scope.groups, function(group) { nameMap[group.name] = group.label; });
	});
		
	$scope.getCode = function(code) {
		if (code.startsWith("http")) return code;
		return "http://midata.coop "+code+"/...";
	};
	
	$scope.getGroupLabel = function(group) {
		return nameMap[group] || "?";
	};
}]);