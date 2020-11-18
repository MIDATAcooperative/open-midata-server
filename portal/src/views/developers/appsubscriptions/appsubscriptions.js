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
.controller('AppSubscriptionsCtrl', ['$scope', '$state', 'server', 'apps', 'status', 'ENV', '$http', '$window', '$translatePartialLoader',function($scope, $state, server, apps, status, ENV, $http, $window, $translatePartialLoader) {
	
	// init
	$scope.error = null;
	
	$scope.triggers = ["fhir_Consent", "fhir_MessageHeader", "fhir_Resource", "time", "init","time/30m"];
	$scope.actions = ["rest-hook", "email", "sms", "nodejs", "app"];
	
	$scope.status = new status(false, $scope);
	$scope.ENV = ENV;
			
	$scope.loadApp = function(appId) {
		$scope.appId=appId;
		$scope.status.doBusy(apps.getApps({ "_id" : appId }, ["version", "creator", "filename", "name", "description", "defaultSubscriptions", "debugHandle" ]))
		.then(function(data) { 
			$scope.app = data.data[0];
			var subscriptions = $scope.app.defaultSubscriptions;
			
			if (subscriptions) {
				angular.forEach(subscriptions, function(subscription) {
					if (subscription.format === "fhir/MessageHeader") {
						subscription.trigger = "fhir_MessageHeader";
						var p = subscription.fhirSubscription.criteria.indexOf("?event=");
		    			var criteria = p > 0 ? subscription.fhirSubscription.criteria.substr(p+7) : subscription.fhirSubscription.criteria;
						subscription.criteria = criteria;
					} else if (subscription.format === "fhir/Consent") {
						subscription.trigger = "fhir_Consent";
					} else if (subscription.format === "time") {
						subscription.trigger = "time";
					} else if (subscription.format === "time/30m") {
						subscription.trigger = "time/30m";
					} else if (subscription.format === "init") {
						subscription.trigger = "init";					
					} else {
						subscription.trigger = "fhir_Resource";
						subscription.criteria = subscription.fhirSubscription.criteria;
					}
					
					if (subscription.fhirSubscription.channel.type === "email") {
						subscription.action = "email";
					} else if (subscription.fhirSubscription.channel.type === "rest-hook") {
						subscription.action = "rest-hook";
						subscription.parameter = subscription.fhirSubscription.channel.endpoint;
					} else if (subscription.fhirSubscription.channel.type === "message") {
						var endpoint = subscription.fhirSubscription.channel.endpoint;
						if (endpoint && endpoint.startsWith("node://")) {
							subscription.action = "nodejs";
							subscription.parameter = endpoint.substr("node://".length);
						}
						else if (endpoint && endpoint.startsWith("app://")) {
							subscription.action = "app";
							subscription.parameter = endpoint.substr("app://".length);
						}
					}
				});
			}
			
		});
	};
	
    $scope.add = function() {
    	if (!$scope.app.defaultSubscriptions) $scope.app.defaultSubscriptions = [];
    	$scope.app.defaultSubscriptions.push({});
    };
    
    $scope.delete = function(elem) {
    	$scope.app.defaultSubscriptions.splice($scope.app.defaultSubscriptions.indexOf(elem), 1);
    };
    
    $scope.submit = function() {
    	angular.forEach($scope.app.defaultSubscriptions, function(subscription) {
    		var criteria;
    		switch (subscription.trigger) {
    		case "fhir_Consent":
    			criteria = "Consent";    			
    			break;
    		case "fhir_MessageHeader":
    			if (subscription.criteria) {
    			    criteria = "MessageHeader?event="+subscription.criteria;
    			} else {
    				criteria = "MessageHeader";
    			}
    			break;
    		case "fhir_Resource":
                criteria = subscription.criteria;
                break;
    		case "time":
    			criteria = "time";
    			break;
    		case "time/30m":
    			criteria = "time/30m";
    			break;
    		case "init":
    			criteria = "init";
    			break;
    		}
    		subscription.fhirSubscription = {
    			"resourceType" : "Subscription",
    			"status" : "active",
    			"criteria" : criteria,
    			"channel" : {}
    		};
    		switch (subscription.action) {
    		case "rest-hook":
    			subscription.fhirSubscription.channel.type = "rest-hook";
    			subscription.fhirSubscription.channel.endpoint = subscription.parameter;
    			break;
    		case "email":
    			subscription.fhirSubscription.channel.type = "email";    			
    			break;
    		case "nodejs":
    			subscription.fhirSubscription.channel.type = "message";
    			subscription.fhirSubscription.channel.endpoint = "node://"+subscription.parameter;
    			break;
    		case "app":
    			subscription.fhirSubscription.channel.type = "message";
    			subscription.fhirSubscription.channel.endpoint = "app://"+subscription.parameter;
    			break;
    		}
    	});
    	
    	 $scope.status.doAction("submit", server.post(jsRoutes.controllers.Market.updateDefaultSubscriptions($scope.app._id).url, JSON.stringify($scope.app)))
    	 .then(function() {
    		$state.go("^.manageapp", { appId : $scope.app._id }); 
    	 });
    };
    
    $scope.startDebug = function() {
    	var data = { plugin : $scope.app._id, action : "start" };
        $scope.status.doAction("debug", server.post(jsRoutes.controllers.Market.setSubscriptionDebug().url, JSON.stringify(data)))
        .then(function(result) {
     	   $scope.app.debugHandle = result.data.debugHandle;
        });
    };
    
    $scope.stopDebug = function() {
       var data = { plugin : $scope.app._id, action : "stop" };
       $scope.status.doAction("debug", server.post(jsRoutes.controllers.Market.setSubscriptionDebug().url, JSON.stringify(data)))
       .then(function(result) {
    	   $scope.app.debugHandle = result.data.debugHandle;
       });
    };
		
	$translatePartialLoader.addPart("developers");	
	$scope.loadApp($state.params.appId);	
}]);