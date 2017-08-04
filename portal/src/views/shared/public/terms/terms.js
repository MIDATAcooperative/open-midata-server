angular.module('portal')
.controller('TermsCtrl', ['$scope', 'terms', '$translate', '$stateParams', '$sce', 'views', function($scope, terms, $translate, $stateParams, $sce, views) {
  
	$scope.view = views.getView("terms");
	
	$scope.init = function(name, version, language) {
		
		terms.get(name, version,language)
		.then(function(result) {
			$scope.terms = result.data;
		});
	};
	
	$scope.close = function() {
		$scope.view.active = false;
	};
	
	var which = ($stateParams.which || ($scope.view.setup ? $scope.view.setup.which : "")).split("--");
	var lang = $stateParams.lang || $translate.use();
	if (which[0]) $scope.init(which[0], which[1], lang);
	
	$scope.$watch('view.setup', function() { 
		if (!$scope.view || !$scope.view.setup || !$scope.view.setup.which) return;
		var which = $scope.view.setup.which.split("--");
		var lang = $stateParams.lang || $translate.use();
		$scope.init(which[0], which[1], lang);
	});
}]);