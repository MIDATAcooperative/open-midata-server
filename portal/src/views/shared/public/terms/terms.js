angular.module('portal')
.controller('TermsCtrl', ['$scope', '$rootScope', 'terms', '$translate', '$stateParams', '$sce', 'views', 'server', '$state', function($scope, $rootScope, terms, $translate, $stateParams, $sce, views, server, $state) {
  
	$scope.view = views.getView("terms");
	
	$scope.loadTerms = function(name, version, language) {
		terms.get(name, version,language)
		.then(function(result) {			
			$scope.terms = result.data;
		}, function() {
			$scope.terms = { title : "Not found", "text" : "The requested terms and conditions are not available."};
		});
	};
	
	$scope.init = function(name, version, language) {
		$scope.name = name;
		$scope.version = version;
		
		if ($state.current.termsRole && (name == "midata-privacy-policy" || name == "midata-terms-of-use")) {
			server.get(jsRoutes.controllers.Terms.currentTerms().url).then(function(result) {
				 let w = "--";
			  	 if (name == "midata-terms-of-use") {
			  		w = result.data[$state.current.termsRole].termsOfUse.split("--");;			  		
			  	 } else if (name == "midata-privacy-policy") {
			  		w = result.data[$state.current.termsRole].privacyPolicy.split("--");;
			  	 }
			  	name = w[0];
		  		version = w[1];			  	
			  	$scope.loadTerms(name, version, language);
			});	
		} else $scope.loadTerms(name, version, language);
		
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
		
	var languageWatcher = null;
	this.$onInit = function () {
		languageWatcher = $rootScope.$on('$translateChangeStart', function(event,data) {
	        $scope.init($scope.name, $scope.version, data.language);
	    });
	};
	 
	this.$onDestroy = function () {	
		languageWatcher();
	};
}]);