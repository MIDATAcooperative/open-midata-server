angular.module('portal')
.controller('ResearchRegistrationCtrl', ['$scope', '$state', 'server', 'status', 'session', '$translate', 'languages', '$stateParams', function($scope, $state, server, status, session, $translate, languages, $stateParams) {
	
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
		if (! $scope.myform.$valid) return;
				
		
		// send the request
		var data = $scope.registration;		
		
		if ($stateParams.developer) {
			data.developer = $stateParams.developer;
		}
		
		$scope.status.doAction("submit", server.post(jsRoutes.controllers.research.Researchers.register().url, JSON.stringify(data)))
		.then(function(data) { session.postLogin(data, $state); });
	};
	
	$scope.changeLanguage = function(lang) {
		$translate.use(lang);
	};
}]);