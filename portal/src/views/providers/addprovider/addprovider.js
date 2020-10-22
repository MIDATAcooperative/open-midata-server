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
.controller('AddProviderCtrl', ['$scope', '$state', 'server', 'status', 'session', '$translate', 'languages', '$document', function($scope, $state, server, status, session, $translate, languages, $document) {
	
	$scope.registration = { language : $translate.use(), subroles:[] };	
	$scope.languages = languages.all;
	$scope.countries = languages.countries;
	$scope.error = null;
	$scope.status = new status(false, $scope);
		
	
	// register new user
	$scope.register = function() {		
		        
		$scope.success = false;
		$scope.submitted = true;	
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		if (! $scope.myform.$valid) {
			var elem = $document[0].querySelector('input.ng-invalid');
			if (elem && elem.focus) elem.focus();
			return;
		}
						        
        /*$scope.registration.birthday = $scope.registration.birthdayYear + "-" + 
                                       $scope.registration.birthdayMonth + "-" +
                                       $scope.registration.birthdayDay;
		
		*/
		// send the request
		var data = $scope.registration;		
		$scope.status.doAction("register", server.post(jsRoutes.controllers.providers.Providers.registerOther().url, JSON.stringify(data))).
		then(function(data) { 
           $scope.success = true; 
		});
	};
	
	
	/*
	$scope.changeSubRole = function(subrole) {
	   var idx = $scope.registration.subroles.indexOf(subrole);
	   if (idx >= 0) $scope.registration.subroles.splice(idx,1); else $scope.registration.subroles.push(subrole);
	};
	
	$scope.hasSubRole = function(subrole) {
		return $scope.registration.subroles.indexOf(subrole) >= 0;
	};
	*/
	/*
	$scope.days = [];
	$scope.months = [];
	$scope.years = [];
	var i = 0;
	for (i=1;i <= 9; i++ ) { $scope.days.push("0"+i); $scope.months.push("0"+i); }
	for (i=10;i <= 31; i++ ) $scope.days.push(""+i);	
	for (i=10;i <= 12; i++ ) $scope.months.push(""+i);
	for (i=2015;i > 1900; i-- ) $scope.years.push(""+i);	
	*/
}]);