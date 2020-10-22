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
.controller('AppMessagesCtrl', ['$scope', '$state', 'server', 'apps', 'status', 'languages', function($scope, $state, server, apps, status, languages) {
	
	// init
	$scope.error = null;
	
	$scope.status = new status(false, $scope);
	
	$scope.languages = languages.array;
    $scope.reasons = ['REGISTRATION', 'REGISTRATION_BY_OTHER_PERSON', 'FIRSTUSE_ANYUSER', 'FIRSTUSE_EXISTINGUSER', 'LOGIN', 'ACCOUNT_UNLOCK', 'CONSENT_REQUEST_OWNER_INVITED', 'CONSENT_REQUEST_OWNER_EXISTING', 'CONSENT_REQUEST_AUTHORIZED_INVITED', 'CONSENT_REQUEST_AUTHORIZED_EXISTING', 'CONSENT_CONFIRM_OWNER', 'CONSENT_CONFIRM_AUTHORIZED', 'CONSENT_REJECT_OWNER', 'CONSENT_REJECT_AUTHORIZED', 'EMAIL_CHANGED_OLDADDRESS', 'EMAIL_CHANGED_NEWADDRESS', 'PASSWORD_FORGOTTEN', 'USER_PRIVATE_KEY_RECOVERED', 'RESOURCE_CHANGE', 'PROCESS_MESSAGE' ];
	$scope.sel = { lang : 'en' };
    $scope.messages = [];
    $scope.tags = {
    	'REGISTRATION': ["site", "confirm-url", "reject-url", "token", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
    	'REGISTRATION_BY_OTHER_PERSON': ["site", "confirm-url", "reject-url", "token", "firstname", "lastname", "email", "executor-firstname", "executor-lastname", "executor-email", "plugin-name", "midata-portal-url"],
    	'FIRSTUSE_ANYUSER' : ["firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
    	'LOGIN' : ["firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
    	'FIRSTUSE_EXISTINGUSER' : ["firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
    	'EMAIL_CHANGED_OLDADDRESS' : ["firstname", "lastname", "old-email", "new-email", "midata-portal-url", "reject-url"],
    	'EMAIL_CHANGED_NEWADDRESS' : ["firstname", "lastname", "old-email", "new-email", "midata-portal-url", "confirm-url"],
    	'ACCOUNT_UNLOCK' : ["firstname", "lastname", "email", "midata-portal-url"],
    	'CONSENT_REQUEST_OWNER_EXISTING' : ["executor-firstname", "executor-lastname", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-email", "consent-name", "firstname", "lastname", "email", "plugin-name", "midata-portal-url", "confirm-url"],
    	'CONSENT_REQUEST_AUTHORIZED_EXISTING' : ["executor-firstname", "executor-lastname", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-email", "consent-name","firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
    	'CONSENT_CONFIRM_OWNER' : ["executor-firstname", "executor-lastname", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-email", "consent-name", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
    	'CONSENT_CONFIRM_AUTHORIZED' : ["executor-firstname", "executor-lastname", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-email", "consent-name", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
    	'CONSENT_REJECT_OWNER' : ["executor-firstname", "executor-lastname", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-email", "consent-name","firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
    	'CONSENT_REJECT_AUTHORIZED' : ["executor-firstname", "executor-lastname", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-email", "consent-name", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
    	'CONSENT_REQUEST_OWNER_INVITED' : ["executor-firstname", "executor-lastname", "executor-email", "grantor-email", "consent-name", "email", "plugin-name", "midata-portal-url", "confirm-url"],
    	'CONSENT_REQUEST_AUTHORIZED_INVITED' : ["executor-firstname", "executor-lastname", "executor-email", "grantor-firstname", "grantor-lastname", "grantor-email", "consent-name", "email", "plugin-name", "midata-portal-url"],
    	'PASSWORD_FORGOTTEN' : [ "site", "password-link", "firstname", "lastname", "email" ],
    	'RESOURCE_CHANGE' : [ "midata-portal-url", "plugin-name", "firstname", "lastname", "email" ],
    	'PROCESS_MESSAGE' : [ "midata-portal-url", "plugin-name", "firstname", "lastname", "email" ],
    	'USER_PRIVATE_KEY_RECOVERED' : [ "firstname", "lastname", "email", "site" ]
    };
	
			
	$scope.loadApp = function(appId) {
		$scope.status.doBusy(apps.getApps({ "_id" : appId }, ["creator", "developerTeam", "filename", "name", "description", "tags", "targetUserRole", "spotlighted", "type","accessTokenUrl", "authorizationUrl", "consumerKey", "consumerSecret", "defaultQuery", "defaultSpaceContext", "defaultSpaceName", "previewUrl", "recommendedPlugins", "requestTokenUrl", "scopeParameters","secret","redirectUri", "url","developmentServer","version","i18n","status", "resharesData", "allowsUserSearch", "predefinedMessages", "requirements", "termsOfUse", "orgName", "publisher", "unlockCode", "writes"]))
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
		$scope.app.msgOnly = true;				
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