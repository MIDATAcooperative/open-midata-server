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

angular.module('portal')
.controller('ManageMailsCtrl', ['$scope', '$state', 'server', 'news', 'status', 'languages', 'studies', 'apps', function($scope, $state, server, news, status, languages, studies, apps) {
	
	// init
	$scope.error = null;
	$scope.mailItem = { status : "DRAFT", studyName : "", title:{}, content:{} };
	$scope.status = new status(false, $scope);
	$scope.allowDelete = $state.current.allowDelete;
	$scope.allowSend = false;
	$scope.editable = true;
	$scope.languages = languages.array;	
	$scope.sel = { lang: "int" };
	$scope.types = ["MARKETING", "PROJECT","APP"];
	$scope.countries = languages.countries;
      
		
    $scope.languages = [];
    for (var i=0;i<languages.array.length;i++) $scope.languages.push(languages.array[i]);
    $scope.languages.push("int");
        
    
	$scope.loadMail = function(mailId) {
		$scope.status.doBusy(server.post(jsRoutes.controllers.BulkMails.get().url, JSON.stringify({ properties : { "_id" : mailId }, fields:["creator", "creatorName", "created", "started", "finished", "name", "status", "title", "content", "studyId", "studyName", "studyCode", "studyGroup", "progressId", "progressCount", "progressFailed"]})))
		.then(function(data) { 
			$scope.mailItem = data.data[0];	
			if ($scope.mailItem.status == "DRAFT" || $scope.mailItem.status == "PAUSED") {
				$scope.allowSend = true;	
			}
            if ($scope.mailItem.status != "DRAFT") {
            	if ($scope.mailItem.status != "FINSIHED" || $scope.mailItem.progressCount > 0) $scope.allowDelete = false;
            	$scope.editable = false;
            }
		});
	};
	
	$scope.change = function() {
		$scope.allowSend = false;
	};
	
	// register app
	$scope.updateMail = function() {
        $scope.submitted = true;
		
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		
		if (! $scope.myform.$valid) return;
		
		if ($scope.mailItem._id == null) {
			$scope.status.doAction('submit', server.post(jsRoutes.controllers.BulkMails.add().url, JSON.stringify($scope.mailItem)))
			.then(function(result) { $scope.loadMail(result.data._id); });
		} else {			
		    $scope.status.doAction('submit', server.post(jsRoutes.controllers.BulkMails.update().url, JSON.stringify($scope.mailItem)))
		    .then(function() { $scope.loadMail($scope.mailItem._id); });
		}
	};
	
	
	$scope.doDelete = function() {
		$scope.status.doAction('delete', server.post(jsRoutes.controllers.BulkMails.delete($scope.mailItem._id).url))
		.then(function(data) { $state.go("^.mails"); });
	};
	
	if ($state.params.mailId != null) { $scope.loadMail($state.params.mailId); }
	else { $scope.status.isBusy = false; }
	
	$scope.studyselection = function(study) {
		  console.log(study);		  
		   $scope.status.doBusy(studies.search({ code : study }, ["_id", "code", "name" ]))
			.then(function(data) {
				if (data.data && data.data.length == 1) {
				  $scope.mailItem.studyId = data.data[0]._id;
				  $scope.mailItem.studyCode = data.data[0].code;
				  $scope.mailItem.studyName = data.data[0].name;
				}
			});
	};
	
	$scope.appselection = function(app) {		 
		   $scope.status.doBusy(apps.getApps({ name : app }, ["_id", "filename", "name", "orgName", "type", "targetUserRole"]))
			.then(function(data) {
				if (data.data && data.data.length == 1) {
				  $scope.mailItem.appId = data.data[0]._id;
				  $scope.mailItem.appName = data.data[0].name;
				  $scope.app = data.data[0];	
				}
			});
	};
		
	
	$scope.status.doBusy(studies.search({ validationStatus : "VALIDATED" }, ["_id", "code", "name" ]))
	.then(function(data) {
		$scope.studies = data.data;
	});
	
	$scope.status.doBusy(apps.getApps({  }, ["creator", "developerTeam", "filename", "name", "description", "type", "targetUserRole" ]))
	.then(function(data) { 
		$scope.apps = data.data;			
	});
	
	$scope.send = function() {
		$scope.status.doAction('send', server.post(jsRoutes.controllers.BulkMails.send($scope.mailItem._id).url))
		.then(function(data) { $state.go("^.mails"); });
	};
	
	$scope.test = function() {
		console.log("DO IT!");
		$scope.status.doAction('send', server.post(jsRoutes.controllers.BulkMails.test($scope.mailItem._id).url));		
	};
	
}]);