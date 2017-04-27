angular.module('portal')
.controller('AppMessagesCtrl', ['$scope', '$state', 'server', 'apps', 'status', function($scope, $state, server, apps, status) {
	
	// init
	$scope.error = null;
	
	$scope.status = new status(false, $scope);
	
	$scope.languages = ['en', 'de', 'fr', 'it'];
    $scope.reasons = ['REGISTRATION', 'FIRSTUSE_ANYUSER', 'FIRSTUSE_EXISTINGUSER', 'CONSENT_REQUEST_OWNER_INVITED', 'CONSENT_REQUEST_OWNER_EXISTING', 'CONSENT_REQUEST_AUTHORIZED_INVITED', 'CONSENT_REQUEST_AUTHORIZED_EXISTING', 'CONSENT_CONFIRM_OWNER', 'CONSENT_CONFIRM_AUTHORIZED', 'CONSENT_REJECT_OWNER', 'CONSENT_REJECT_AUTHORIZED' ];
	$scope.sel = { lang : 'en' };
    $scope.messages = [];
    $scope.tags = {
    	'REGISTRATION': ["site", "confirm-url", "reject-url", "token", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
    	'FIRSTUSE_ANYUSER' : ["firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
    	'FIRSTUSE_EXISTINGUSER' : ["firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
    	'CONSENT_REQUEST_OWNER_EXISTING' : ["executor-firstname", "executor-lastname", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-email", "consent-name", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
    	'CONSENT_REQUEST_AUTHORIZED_EXISTING' : ["executor-firstname", "executor-lastname", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-email", "consent-name","firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
    	'CONSENT_CONFIRM_OWNER' : ["executor-firstname", "executor-lastname", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-email", "consent-name", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
    	'CONSENT_CONFIRM_AUTHORIZED' : ["executor-firstname", "executor-lastname", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-email", "consent-name", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
    	'CONSENT_REJECT_OWNER' : ["executor-firstname", "executor-lastname", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-email", "consent-name","firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
    	'CONSENT_REJECT_AUTHORIZED' : ["executor-firstname", "executor-lastname", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-email", "consent-name", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
    	'CONSENT_REQUEST_OWNER_INVITED' : ["executor-firstname", "executor-lastname", "executor-email", "grantor-email", "consent-name", "email", "plugin-name", "midata-portal-url"],
    	'CONSENT_REQUEST_AUTHORIZED_INVITED' : ["executor-firstname", "executor-lastname", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-email", "consent-name", "email", "plugin-name", "midata-portal-url"]
    };
	
			
	$scope.loadApp = function(appId) {
		$scope.status.doBusy(apps.getApps({ "_id" : appId }, ["creator", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type","accessTokenUrl", "authorizationUrl", "consumerKey", "consumerSecret", "defaultQuery", "defaultSpaceContext", "defaultSpaceName", "previewUrl", "recommendedPlugins", "requestTokenUrl", "scopeParameters","secret","redirectUri", "url","developmentServer","version","i18n","status", "resharesData", "allowsUserSearch", "predefinedMessages"]))
		.then(function(data) { 
			$scope.app = data.data[0];	
			$scope.messages = [];			
            angular.forEach($scope.app.predefinedMessages, function(msg) { $scope.messages.push(msg); });			
		});
	};
	
	// register app
	$scope.updateApp = function() {		
		$scope.submitted = true;	
				
		$scope.app.predefinedMessages = {};
						
		angular.forEach($scope.messages, function(msg) {
			
			angular.forEach(msg.text, function(v,k) { if (v === "") delete msg.text[k]; });
			angular.forEach(msg.title, function(v,k) { if (v === "") delete msg.title[k]; });
			 
			$scope.app.predefinedMessages[msg.reason+(msg.code ? ("_"+msg.code) : "")] = msg;
		});
				
		/*
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		
		if (! $scope.myform.$valid) return;
		*/
						
		$scope.status.doAction('submit', apps.updatePlugin($scope.app))
		.then(function() { $scope.selmsg = null;$scope.submitted = false;$scope.loadApp($state.params.appId); });
		
	};
	
	$scope.addMessage = function() {
		$scope.messages.push($scope.selmsg = { title : {}, text: {} });
	};
	
	$scope.showMessage = function(msg, lang) {
		$scope.selmsg = msg;
		$scope.sel.lang = lang || $scope.sel.lang;
	};
	
	$scope.deleteMessage = function() {
		$scope.messages.splice($scope.messages.indexOf($scope.selmsg), 1);
		$scope.updateApp();
		//$scope.selmsg = null;
	};
			
	$scope.loadApp($state.params.appId);	
}]);