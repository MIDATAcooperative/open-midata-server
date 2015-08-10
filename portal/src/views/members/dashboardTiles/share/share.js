angular.module('views')
.controller('ShareCtrl', ['$scope', 'server', '$attrs', 'views', 'circles', 'spaces', 'records', 'status', function($scope, server, $attrs, views, circles, spaces, records, status) {
	
	$scope.circles = [];	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);	
	$scope.form = { newCircleName : "" };
	$scope.errors = {};
	$scope.selectedCircle = null;
	
	$scope.reload = function() {		
		if (!$scope.view.active) return;
		$scope.limit = $scope.view.position == "small" ? 4 : 20;
		$scope.status.doBusy(circles.get({ owner : true }, [ "name", "order", "members" ])).
		then(function (result) { 
			$scope.circles = result.data;
            if ($scope.circles.length > 0) $scope.selectedCircle = $scope.circles[0];
            $scope.circles.push({ name : "<Create new...>", isNew : true });			
		});
		$scope.status.doBusy(spaces.get({ "_id" : { "$oid" : $scope.view.setup.space  }}, [ "rules" ]))
		.then(function(result) {
			if (result.data) $scope.rules = result.data[0].rules;
		});
	};
	
	$scope.createCircle = function() {
		console.log($scope.form);
		if ($scope.form.newCircleName.trim() === "") {
			$scope.errors.newCircleName = "Please enter a valid name";
			return;
		} else { $scope.errors.newCircleName = null; }
		
		circles.createNew($scope.form.newCircleName).
		then(function(results) {
			$scope.circles.push(results.data);
			$scope.selectedCircle = results.data;			
		});		
	};
	
	$scope.share = function() {
		records.shareSpaceWithCircle($scope.view.setup.space, $scope.selectedCircle._id.$oid)
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