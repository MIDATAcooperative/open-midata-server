angular.module('portal')
.controller('AdminRegistrationCtrl', ['$scope', '$state', 'server', 'status', 'session', '$translate', 'languages', function($scope, $state, server, status, session, $translate, languages) {
	
	$scope.registration = { language : $translate.use(), subroles:[] };
	$scope.subroles = ["SUPERADMIN", "USERADMIN", "STUDYADMIN", "CONTENTADMIN", "PLUGINADMIN", "NEWSWRITER"];
	$scope.languages = languages.all;
	$scope.error = null;
	$scope.status = new status(false, $scope);
	
	$scope.offline = (window.jsRoutes === undefined) || (window.jsRoutes.controllers === undefined);
	
	// register new user
	$scope.register = function() {		
		
        $scope.myform.password.$setValidity('compare', $scope.registration.password ==  $scope.registration.password2);
		
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
		$scope.status.doAction("register", server.post(jsRoutes.controllers.admin.Administration.register().url, JSON.stringify(data))).
		then(function(data) { session.postLogin(data, $state); });
	};
	
	
	$scope.changeSubRole = function(subrole) {
	   var idx = $scope.registration.subroles.indexOf(subrole);
	   if (idx >= 0) $scope.registration.subroles.splice(idx,1); else $scope.registration.subroles.push(subrole);
	};
	
	$scope.hasSubRole = function(subrole) {
		return $scope.registration.subroles.indexOf(subrole) >= 0;
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