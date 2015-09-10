angular.module('views')
.controller('HelpCtrl', ['$scope', '$state', 'server', function($scope, $state, server) {
		
}])
.controller('HelpSetupCtrl', ['$scope', '$state', 'portal', function($scope, $state, portal) {
	
	$scope.init = function() {
		portal.getConfig()
		.then(function(data) {
			console.log(data);
			if (data.data && data.data.questions) {
				$scope.config = data.data;
				$scope.questions = $scope.config.questions;			
			} else {
				$scope.config = 
				    { 
						questions : {
							general : {},
							qself : {},
							devices : {},
							share : {}
						} 
				    };
				$scope.questions = $scope.config.questions;
			}
		});
	};
	
	var add = function(id, dashboard, cond) {
		if (cond) {
			if ($scope.config.dashboards[dashboard] == null) {
				$scope.config.dashboards[dashboard] = { add : [], remove : [] };
			}
			if ($scope.config.dashboards[dashboard].add.indexOf(id) < 0) {
			  $scope.config.dashboards[dashboard].add.push(id);
			}
		} else {
			if ($scope.config.dashboards[dashboard] == null) return;
			var p = $scope.config.dashboards[dashboard].add.indexOf(id);
			if (p >= 0) $scope.config.dashboards[dashboard].add.splice(p, 1);
		}
	};
	
	var remove = function(id, dashboard, cond) {
		if (cond) {
			if ($scope.config.dashboards[dashboard] == null) {
				$scope.config.dashboards[dashboard] = { add : [], remove : [] };
			}
			if ($scope.config.dashboards[dashboard].remove.indexOf(id) < 0) {
			  $scope.config.dashboards[dashboard].remove.push(id);
			}
		} else {
			if ($scope.config.dashboards[dashboard] == null) return;
			var p = $scope.config.dashboards[dashboard].remove.indexOf(id);
			if (p >= 0) $scope.config.dashboards[dashboard].remove.splice(p, 1);
		}
	};
	
	$scope.setConfig = function() {
		if ($scope.config.dashboards == null) {
			$scope.config.dashboards = {};
		}
		
		var q = $scope.config.questions;
		
		add('charts','me', true);
		add('entry','me', q.qself.weight || q.qself.sleep);
		
		add('jawboneupimport','mydata', q.devices.jawbone);
		add('clock','me', q.devices.jawbone && (q.qself.steps || q.qself.sleep));
		add('meal','me', q.devices.jawbone && q.qself.nutrition);
		//add('credentials_store','me', true);
		//add('credentials','me', true);
		add('energy-meter','me', q.devices.jawbone && (q.qself.steps || q.qself.nutrition));
		add('fitbit','mydata', q.devices.fitbit);
		add('water-meter','me', q.devices.fitbit && q.qself.water);
		add('weight-watcher','me', q.devices.fitbit && q.qself.weight);
		add('fileupload','mydata', q.general.manage || q.general.protocol);
		add('trainingeditor','me', q.general.planning);
		add('trainingdiary','me', q.general.planning);
		//add('cdaimport','me', true);
		//add('cdaviewer','me', q.general.manage);
		
		//add('surveys','me', true); 
		
		remove('help_welcome', 'me', true);
		
		portal.setConfig($scope.config)
		.then(function() {
			$state.go("^.dashboard", { dashId : 'me' });
		});
	};
	
	$scope.init();
}]);