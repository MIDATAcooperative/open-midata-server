/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('portal')
.controller('DeveloperRegistrationCtrl', ['$scope', '$state', 'server', 'status', 'session', '$translate', 'languages', '$document', 'crypto', 'views', function($scope, $state, server, status, session, $translate, languages, $document, crypto, views) {
	
	$scope.registration = { language : $translate.use() };
	$scope.languages = languages.all;
	$scope.error = null;
	$scope.status = new status(false, $scope);
	
	$scope.offline = (window.jsRoutes === undefined) || (window.jsRoutes.controllers === undefined);
	$scope.view = views.getView("terms");
	
	server.get(jsRoutes.controllers.Terms.currentTerms().url).then(function(result) { $scope.currentTerms = result.data; });

	
	// register new user
	$scope.register = function() {		
		
		var pwvalid = crypto.isValidPassword($scope.registration.password1); 
        $scope.myform.password.$setValidity('tooshort', pwvalid);
        if (!pwvalid) {
        	$scope.myform.password.$invalid = true;
        	$scope.myform.password.$error = { 'tooshort' : true };
        }
		
        $scope.myform.password.$setValidity('compare', $scope.registration.password1 ==  $scope.registration.password2);
		
        $scope.myform.agb.$setValidity('mustaccept', $scope.registration.agb);        
        if (!$scope.registration.agb) {
        	
        	$scope.myform.agb.$invalid = true;
        	$scope.myform.agb.$error = { 'mustaccept' : true };
        }

		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) {
			var elem = $document[0].querySelector('input.ng-invalid');
			if (elem && elem.focus) elem.focus();
			return;
		}
		
		if ($scope.registration.password1 !=  $scope.registration.password2) {
			$scope.error = { code : "error.invalid.password_repetition" };
			return;
		}
		        
        $scope.registration.birthday = $scope.registration.birthdayYear + "-" + 
                                       $scope.registration.birthdayMonth + "-" +
                                       $scope.registration.birthdayDay;
		
		// send the request
		var data = $scope.registration;		
		
        crypto.generateKeys($scope.registration.password1).then(function(keys) {
			
			$scope.registration.password = keys.pw_hash;
			$scope.registration.pub = keys.pub;
			$scope.registration.priv_pw = keys.priv_pw;
			$scope.registration.recovery = keys.recovery;
				
		    $scope.status.doAction("register", server.post(jsRoutes.controllers.Developers.register().url, JSON.stringify(data))).
		    then(function(data) { session.postLogin(data, $state); });
        });
	};
	
	$scope.changeLanguage = function(lang) {
		$translate.use(lang);
	};
	
	$scope.terms = function(def) {		
		views.setView("terms", def, "Terms");
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