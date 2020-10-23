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

angular.module('views')
.controller('ShareCtrl', ['$scope', 'server', '$attrs', 'views', 'circles', 'spaces', 'records', 'status', function($scope, server, $attrs, views, circles, spaces, records, status) {
	
	$scope.circles = [];	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);	
	$scope.form = { newCircleName : "" };
	$scope.errors = {};
	$scope.selectedConsent = null;
	
	$scope.reload = function() {		
		if (!$scope.view.active) return;
		$scope.limit = $scope.view.position == "small" ? 4 : 20;
		$scope.status.doBusy(circles.listConsents({  }, [ "name", "type", "authorized" ])).
		then(function (result) { 
			$scope.consents = result.data;
            if ($scope.consents.length > 0) $scope.selectedConsent = $scope.consents[0];
            $scope.consents.push({ name : "<Create new...>", isNew : true });			
		});
		$scope.status.doBusy(spaces.get({ "_id" : $scope.view.setup.space }, [ "query" ]))
		.then(function(result) {
			if (result.data) {  
				$scope.rules = result.data[0].query || {};
				delete $scope.rules.aps;				
			}
		});
	};
	
	$scope.createCircle = function() {
		
		if ($scope.form.newCircleName.trim() === "") {
			$scope.errors.newCircleName = "Please enter a valid name";
			return;
		} else { $scope.errors.newCircleName = null; }
		
		circles.createNew({ name : $scope.form.newCircleName, type:"CIRCLE" }).
		then(function(results) {
			$scope.consents.push(results.data);
			$scope.selectedConsent = results.data;			
		});		
	};
	
	$scope.share = function() {
		records.shareSpaceWithCircle($scope.view.setup.space, $scope.selectedConsent._id)
		.then(function() {
		   $scope.success = true;
           //views.disableView($scope.view.id);			
		});
	};
	
	$scope.cancel = function() {
		views.disableView($scope.view.id);
	};
		
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);