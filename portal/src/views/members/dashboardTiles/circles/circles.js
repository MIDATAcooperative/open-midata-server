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
.controller('SmallCirclesCtrl', ['$scope', '$state', 'server', '$attrs', 'views', 'circles', 'status', function($scope, $state, server, $attrs, views, circles, status) {
	
	$scope.circles = [];	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);
	$scope.limit = 4;
	$scope.alreadyadded = false;
	$scope.form = { newCircleName : "" };
	$scope.errors = {};
	
	$scope.reload = function() {		
		if (!$scope.view.active) return;
		$scope.limit = $scope.view.position == "small" ? 4 : 20;
		$scope.status.doBusy(circles.get($scope.view.setup.properties, $scope.view.setup.fields)).
		then(function (result) { 
			$scope.circles = result.data;
			
			if ($scope.alreadyadded || !$scope.view.setup.instances) return;
			$scope.alreadyadded = true;
						
			_.each($scope.circles, function(circle) {
				var circledef =
	   		     {
	   		    	   id : "circle"+circle._id,
	   		    	   template : "/views/shared/dashboardTiles/flexiblerecords/flexiblerecords.html",
	   		    	   title : circle.ownerName ? circle.ownerName : circle.name,
	   		    	   active : true,
	   		    	   position : "small",
	   		    	   setup : { aps : circle._id, properties : { "max-age" : 86400 * 31 } , fields : [ "ownerName", "created", "id", "name" ] }
	   		     };
	   		     views.layout.small.push(views.def(circledef)); 
			});
		});
	};
	
	
	$scope.createCircle = function() {		
		if ($scope.form.newCircleName.trim() === "") {
			$scope.errors.newCircleName = "Please enter a valid name";
			return;
		} else { $scope.errors.newCircleName = null; }
		
		circles.createNew({ name : $scope.form.newCircleName, type : "CIRCLE" }).
		then(function(results) {
			$state.go('^.circles', { circleId : results.data._id });
		});		
	};
		
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);