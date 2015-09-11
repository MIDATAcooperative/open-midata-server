angular.module('views')
.controller('CirclesCtrl', ['$scope', '$state', 'server', '$attrs', 'views', 'circles', 'status', function($scope, $state, server, $attrs, views, circles, status) {
	
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
	   		    	   id : "circle"+circle._id.$oid,
	   		    	   template : "/views/shared/dashboardTiles/flexiblerecords/flexiblerecords.html",
	   		    	   title : circle.ownerName ? circle.ownerName : circle.name,
	   		    	   active : true,
	   		    	   position : "small",
	   		    	   setup : { aps : circle._id.$oid, properties : { "max-age" : 86400 * 31 } , fields : [ "ownerName", "created", "id", "name" ] }
	   		     };
	   		     views.layout.small.push(views.def(circledef)); 
			});
		});
	};
	
	/*
	$scope.createCircle = function() {
		console.log($scope.form);
		if ($scope.form.newCircleName.trim() === "") {
			$scope.errors.newCircleName = "Please enter a valid name";
			return;
		} else { $scope.errors.newCircleName = null; }
		
		circles.createNew($scope.form.newCircleName).
		then(function(results) {
			$state.go('^.circle', { circleId : results.data._id.$oid });
		});		
	};
		*/
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);