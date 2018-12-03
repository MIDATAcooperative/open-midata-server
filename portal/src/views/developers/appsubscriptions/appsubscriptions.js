angular.module('portal')
.controller('AppSubscriptionsCtrl', ['$scope', '$state', 'server', 'apps', 'status', 'ENV', '$http', '$window', '$translatePartialLoader',function($scope, $state, server, apps, status, ENV, $http, $window, $translatePartialLoader) {
	
	// init
	$scope.error = null;
	
	$scope.triggers = ["fhir_Consent", "fhir_MessageHeader", "fhir_Resource", "time", "init"];
	$scope.actions = ["rest-hook", "email", "nodejs"];
	
	$scope.status = new status(false, $scope);
			
	$scope.loadApp = function(appId) {
		$scope.status.doBusy(apps.getApps({ "_id" : appId }, ["version", "creator", "filename", "name", "description", "defaultSubscriptions" ]))
		.then(function(data) { 
			$scope.app = data.data[0];
			var subscriptions = $scope.app.defaultSubscriptions;
			
			if (subscriptions) {
				angular.forEach(subscriptions, function(subscription) {
					if (subscription.format === "fhir/MessageHeader") {
						subscription.trigger = "fhir_MessageHeader";
					} else if (subscription.format === "fhir/Consent") {
						subscription.trigger = "fhir_Consent";
					} else if (subscription.format === "time") {
						subscription.trigger = "time";
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
    			criteria = "MessageHeader";
    			break;
    		case "fhir_Resource":
                criteria = subscription.criteria;
                break;
    		case "time":
    			criteria = "time";
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
    		}
    	});
    	
    	 $scope.status.doAction("submit", server.post(jsRoutes.controllers.Market.updateDefaultSubscriptions($scope.app._id).url, JSON.stringify($scope.app)))
    	 .then(function() {
    		$state.go("^.manageapp", { appId : $scope.app._id }); 
    	 });
    };
		
	$translatePartialLoader.addPart("developers");	
	$scope.loadApp($state.params.appId);	
}]);