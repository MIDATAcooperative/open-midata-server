angular.module('portal')
.controller('TermsCtrl', ['$scope', 'terms', '$translate', '$stateParams', '$sce', function($scope, terms, $translate, $stateParams, $sce) {
  
	$scope.init = function(name, version, language) {
		
		terms.get(name, version,language)
		.then(function(result) {
			$scope.terms = result.data;
		});
	};
	
	var which = $stateParams.which.split("--");
	var lang = $stateParams.lang || $translate.use();
	$scope.init(which[0], which[1], lang);
	
}]);