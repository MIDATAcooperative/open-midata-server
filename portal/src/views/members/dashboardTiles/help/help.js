/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('views')
.controller('HelpCtrl', ['$scope', '$state', 'portal', 'spaces', 'session', function($scope, $state, portal, spaces, session) {
	$scope.alreadyAnswered = false;
	$scope.init = function() {
		
		session.currentUser.then(function(userId) { $scope.userId = userId; });
		/*portal.getConfig()
		.then(function(data) {			
			if (data.data && data.data.questions) {
				$scope.alreadyAnswered = true;								
			} else {
				$scope.alreadyAnswered = false;				
			}
		});*/
	};
	$scope.init();
	
	$scope.use = function(view) {
		spaces.openAppLink($state, $scope.userId, { app : view});
	};
		
}])
.controller('HelpSetupCtrl', ['$scope', '$state', 'portal', 'apps', 'spaces', 'session', function($scope, $state, portal, apps, spaces, session) {
	
	$scope.isInstalled = {};
	
	
	$scope.init = function() {
		portal.getConfig()
		.then(function(data) {
			
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
		
		session.currentUser.then(function(userId) {
		spaces.getSpacesOfUser(userId)
		.then(function(data) {
			$scope.isInstalled = {};
			angular.forEach(data.data, function(space) {
			  $scope.isInstalled[space.name] = true;
			});
		});
		});
	};

	var obsPlugin = function(type) {
		return { 
			plugin : "fhir-observation",
			name : "Observations",
			//name : "measure."+type.replace("/","_"), 
			query : { format : "fhir/Observation"  }, 
			config : { measures : [ type ], owner : "self" } 		
		};
	};
	$scope.measures = [
      { id : "body_weight", install : 
    	[ 
           obsPlugin("body/weight")
        ] 
      },
      { id : "activities_steps", install : 
    	[
    	   obsPlugin("activities/steps")
    	]
      },    	  
      { id : "diary", install : 
    	[ 
    	  { plugin : "record-list", name : "measure.diary" } 
    	] 
      },
      { id : "subjective_condition", install : 
    	[ 
    	 
    	] 
      },
      { id : "heart", install : 
    	[
          obsPlugin("activities/heartrate"),
          obsPlugin("body/bloodpressure"),
    	]
      },
      { id : "documents", install : 
    	[ 
			{ plugin : "fhir-docref", name : "measure.files" }
        ] 
      },
      { id : "appointments", install : 
    	[ 
    	] 
      },
      { id : "sleep", install : 
    	[ 
    	] 
      }
	];
	
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
	
	var install = function(id, name, where, query, config) {
		if ($scope.isInstalled[name]) return;
		var options = { spaceName : name, applyRules : true, context : where };
		if (query) options.query = query;
		if (config) options.config = config;
		return apps.installPlugin(id, options);			
	};
	
	$scope.setConfig = function() {
		if ($scope.config.dashboards == null) {
			$scope.config.dashboards = {};
		}
		
		var q = $scope.config.questions;
		var obs = null;
		angular.forEach($scope.measures, function(measure) {
		  if (q.measures[measure.id]) {
			 angular.forEach(measure.install, function(toInstall) {
				
				if (toInstall.plugin === "fhir-observation") {
				  if (obs==null) obs = toInstall; else obs.config.measures = obs.config.measures.concat(toInstall.config.measures);	
				} else {
				   install(toInstall.plugin, toInstall.name, toInstall.dashId || "me", toInstall.query, toInstall.config);
				}
			 });
		  }
		});
		
		if (obs != null) {
			install(obs.plugin, obs.name, obs.dashId || "me", obs.query, obs.config);
		}
		
		if (q.devices.fitbit) install("fitbit", "config.fitbit", "config");
		if (q.devices.withings) install("witings", "config.withings", "config");
				
		remove('help_welcome', 'me', true);
		
		portal.setConfig($scope.config)
		.then(function() {
			$state.go("^.dashboard", { dashId : 'config' });
		});
	};
	
	$scope.init();
}]);