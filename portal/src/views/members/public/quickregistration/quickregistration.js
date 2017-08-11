angular.module('portal')
.controller('QuickRegistrationCtrl', ['$scope', '$state', 'server', 'status', 'session', '$translate', 'languages', '$document', function($scope, $state, server, status, session, $translate, languages, $document) {
	
	$scope.registration = { language : $translate.use() };
	$scope.languages = languages.all;
	$scope.error = null;
	$scope.status = new status(false, $scope);
	
	$scope.offline = (window.jsRoutes === undefined) || (window.jsRoutes.controllers === undefined);
	
	// register new user
	$scope.register = function() {		
		
        $scope.myform.password.$setValidity('compare', $scope.registration.password ==  $scope.registration.password2);
		
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		
	    $scope.myform.birthday.$setValidity('day', $scope.registration.birthdayDay > 0 && $scope.registration.birthdayDay < 32);
        $scope.myform.birthday.$setValidity('month', $scope.registration.birthdayMonth > 0);
		$scope.myform.birthday.$setValidity('year', $scope.registration.birthdayYear >= 1900 && $scope.registration.birthdayYear <= new Date().getFullYear());
	
		
		$scope.error = null;
		if (! $scope.myform.$valid) {
			var elem = $document[0].querySelector('input.ng-invalid');
			if (elem && elem.focus) elem.focus();
			return;
		}
		
		if ($scope.registration.password !=  $scope.registration.password2) {
			$scope.error = { code : "error.invalid.password_repetition" };
			return;
		}
		        
        var pad = function(n){
		    return ("0" + n).slice(-2);
		};
		
        $scope.registration.birthday = $scope.registration.birthdayYear + "-" + 
                                       pad($scope.registration.birthdayMonth) + "-" +
                                       pad($scope.registration.birthdayDay);		
	
		// send the request
		var data = $scope.registration;		
		$scope.status.doAction("register", server.post(jsRoutes.controllers.QuickRegistration.register().url, JSON.stringify(data))).
		then(function(data) { session.postLogin(data, $state); });
	};
	
	$scope.changeLanguage = function(lang) {
		$translate.use(lang);
	};

	$scope.months = [];
	var i = 0;
	for (i=1;i <= 9; i++ ) { $scope.months.push("0"+i); }
	for (i=10;i <= 12; i++ ) $scope.months.push(""+i);
	
}]);