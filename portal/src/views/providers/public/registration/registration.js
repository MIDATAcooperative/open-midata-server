angular.module('portal')
.controller('ProviderRegistrationCtrl', ['$scope', '$state', 'server', 'status' , 'session', '$translate', 'languages', '$stateParams', '$document', function($scope, $state, server, status, session, $translate, languages, $stateParams, $document) {
	
	$scope.registration = { language : $translate.use() };
	$scope.languages = languages.all;
	$scope.error = null;
	$scope.submitted = false;
	
	$scope.offline = (window.jsRoutes === undefined) || (window.jsRoutes.controllers === undefined);
	$scope.status = new status(true, $scope);
	
	// register new user
	$scope.register = function() {

		$scope.myform.password.$setValidity('compare', $scope.registration.password ==  $scope.registration.password2);
		
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) {
			var elem = $document[0].querySelector('input.ng-invalid');
			if (elem && elem.focus) elem.focus();
			return;
		}
						
		// send the request
		var data = $scope.registration;		
		
		if ($stateParams.developer) {
			data.developer = $stateParams.developer;
		}
		
		$scope.status.doAction("register", server.post(jsRoutes.controllers.providers.Providers.register().url, JSON.stringify(data)))
		.then(function(data) { session.postLogin(data, $state); });
			
	};
	
	$scope.changeLanguage = function(lang) {
		$translate.use(lang);
	};
	
}]);