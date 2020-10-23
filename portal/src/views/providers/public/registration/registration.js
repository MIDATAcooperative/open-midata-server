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
.controller('ProviderRegistrationCtrl', ['$scope', '$state', 'server', 'status' , 'session', '$translate', 'languages', '$stateParams', '$document', 'crypto', 'views', function($scope, $state, server, status, session, $translate, languages, $stateParams, $document, crypto, views) {
	
	$scope.registration = { language : $translate.use() };
	$scope.languages = languages.all;
	$scope.countries = languages.countries;
	$scope.error = null;
	$scope.submitted = false;
	
	$scope.offline = (window.jsRoutes === undefined) || (window.jsRoutes.controllers === undefined);
	$scope.status = new status(true, $scope);

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
		
		        $scope.status.doAction("register", server.post(jsRoutes.controllers.providers.Providers.register().url, JSON.stringify(data)))
		        .then(function(data) { session.postLogin(data, $state); });
		
		 });
			
	};
	
	$scope.changeLanguage = function(lang) {
		$translate.use(lang);
	};

	$scope.terms = function(def) {		
		views.setView("terms", def, "Terms");
	};
	
}]);