angular.module('portal')
.controller('ResearchRegistrationCtrl', ['$scope', '$state', 'server', 'status', 'session', '$translate', 'languages', '$stateParams', '$document', 'crypto', function($scope, $state, server, status, session, $translate, languages, $stateParams, $document, crypto) {
	
	$scope.registration = { language : $translate.use() };
	$scope.languages = languages.all;
	$scope.countries = languages.countries;
	$scope.error = null;
	$scope.submitted = false;
	$scope.offline = (window.jsRoutes === undefined) || (window.jsRoutes.controllers === undefined);
	$scope.status = new status(true, $scope);
	
	// register new user
	$scope.register = function() {

		var pwvalid = crypto.isValidPassword($scope.registration.password1); 
        $scope.myform.password.$setValidity('tooshort', pwvalid);
        if (!pwvalid) {
        	$scope.myform.password.$invalid = true;
        	$scope.myform.password.$error = { 'tooshort' : true };
        }
        
		$scope.myform.password.$setValidity('compare', $scope.registration.password1 ==  $scope.registration.password2);
		
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
		
		crypto.generateKeys($scope.registration.password1).then(function(keys) {
				
				$scope.registration.password = keys.pw_hash;
				$scope.registration.pub = keys.pub;
				$scope.registration.priv_pw = keys.priv_pw;
				$scope.registration.recovery = keys.recovery;
		        $scope.status.doAction("submit", server.post(jsRoutes.controllers.research.Researchers.register().url, JSON.stringify(data)))
		        .then(function(data) { session.postLogin(data, $state); });
		        
		});
	};
	
	$scope.changeLanguage = function(lang) {
		$translate.use(lang);
	};
}]);