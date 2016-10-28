angular.module('portal')
.controller('RegistrationCtrl', ['$scope', '$state', 'server', 'status', 'session', '$translate', 'languages', '$stateParams', function($scope, $state, server, status, session, $translate, languages, $stateParams) {
	
	$scope.registration = { language : $translate.use() };
	$scope.languages = languages.all;
	$scope.error = null;
	$scope.status = new status(false, $scope);
	
	$scope.offline = (window.jsRoutes === undefined) || (window.jsRoutes.controllers === undefined);
	
	// register new user
	$scope.register = function() {		
		
        $scope.myform.password.$setValidity('compare', $scope.registration.password ==  $scope.registration.password2);
        $scope.myform.agb.$setValidity('mustaccept', $scope.registration.agb);
        if (!$scope.registration.agb) {
        	console.log($scope.myform.agb);
        	$scope.myform.agb.$invalid = true;
        	$scope.myform.agb.$error = { 'mustaccept' : true };
        }
        
		
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) return;
		
		if ($scope.registration.password !=  $scope.registration.password2) {
			$scope.error = { code : "error.invalid.password_repetition" };
			return;
		}		
		        
        $scope.registration.birthday = $scope.registration.birthdayYear + "-" + 
                                       $scope.registration.birthdayMonth + "-" +
                                       $scope.registration.birthdayDay;
		
		// send the request
		var data = $scope.registration;	
		
		if ($stateParams.developer) {
			data.developer = $stateParams.developer;
		}
		
		$scope.status.doAction("register", server.post(jsRoutes.controllers.Application.register().url, JSON.stringify(data))).
		then(function(data) { session.postLogin(data, $state); });
	};
	
	$scope.changeLanguage = function(lang) {
		$translate.use(lang);
	};
	
	$scope.days = [];
	$scope.months = [];
	$scope.years = [];
	var i = 0;
	for (i=1;i <= 9; i++ ) { $scope.days.push("0"+i); $scope.months.push("0"+i); }
	for (i=10;i <= 31; i++ ) $scope.days.push(""+i);	
	for (i=10;i <= 12; i++ ) $scope.months.push(""+i);
	for (i=2015;i > 1900; i-- ) $scope.years.push(""+i);	
	
}]);