var planCreator = angular.module('planCreator', []);
planCreator.factory('server', [ '$http', function($http) {
	
	var service = {};
	
	service.createRecord = function(authToken, name, description, format, data) {
		// construct json
		var data = {
			"authToken": authToken,
			"data": angular.toJson(data),
			"name": name,
			"format" : format,
			"description": description
		};
		
		// submit to server
		return $http.post("https://" + window.location.hostname + ":9000/api/visualizations/create", data);
	};
	
	service.getRecords = function(authToken, properties,fields) {
		 var data = { "authToken" : authToken, "properties" : properties, fields : fields };		
		 return $http.post("https://" + window.location.hostname + ":9000/api/visualizations/records", data);
	};
	
	return service;	
}]);
planCreator.controller('PlanEditorCtrl', ['$scope', '$http', '$location', '$filter', 'server',
	function($scope, $http, $location, $filter, server) {
		
		// init
		$scope.loading = true;
		$scope.error = null;
		
		$scope.authToken = $location.path().split("/")[1];
		$scope.records = [];
		$scope.skipped = { wrongFormat : 0, outdated : 0};
		
		$scope.activePlan = null;
		$scope.availablePlans = {};
		$scope.saving = 0;
				
		$scope.initNewPlan = function() {
			var d = new Date();
			$scope.newPlan = { name:"" , data : { startDate : $filter('date')(d, "yyyy-MM-dd") } };
		};
		
		$scope.initNewPlan();
		
		$scope.createNewPlan = function() {
			var d = new Date($scope.newPlan.data.startDate);
			$scope.newPlan.data.days = [];
			for (var day = 0; day < 7; day++) {
				var newday = { changed: true, date:new Date(d.getFullYear(), d.getMonth(), d.getDay() + day), location:"", actions:[ {} ]};
				$scope.newPlan.data.days.push(newday);
			}
			
			$scope.activePlan = $scope.availablePlans[$scope.newPlan.name] = $scope.newPlan;
			$scope.initNewPlan();
			$scope.createMode = false;
		};
		
		$scope.checkAction = function(day, index) {
			// day.changed = true;
			$scope.activePlan.changed = true;
			if (day.actions[index].name == "" && day.actions.length > 1) day.actions.splice(index, 1);
			else if (day.actions.length == index + 1) day.actions.push({});
		};
		
								
		$scope.saveAll = function() {			
			angular.forEach($scope.availablePlans, function(plan,name) {
				if (plan.changed) {
					plan.changed = false;
					$scope.save(plan);
				}
			});
		};
		
		$scope.save = function(plan) {	
			$scope.saving++;
			server.createRecord($scope.authToken, plan.name, plan.description, "trainingplan", plan.data)
			.then(function() { $scope.saving--; });
		};
		
		$scope.style = function(action) {
			var obj = { "font-weight" : "bold", "margin-top" : "15px" };
			if (action == $scope.selectedAction) return null;
			if (!action.count) return obj;
			return null;
		};
		
		$scope.select = function(action) {
			$scope.selectedAction = action;
		};
						
		$scope.load = function() {			
			server.getRecords($scope.authToken, { "format" : "trainingplan" }, ["name", "description", "created", "data"])
			.then(function(results) {				
				
				angular.forEach(results.data, function(rec) {
					var old = $scope.availablePlans[rec.name];
					if (!old || old.created < rec.created) {
						$scope.availablePlans[rec.name] = rec;
						$scope.activePlan = rec;
					}
				});
				
				if ($scope.activePlan == null) { $scope.createMode = true; }
			});
		};
		

		$scope.load();
				
	}
]);
