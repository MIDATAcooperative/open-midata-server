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
.controller('RequiredInformationCtrl', ['$scope', '$state', 'server', 'status', function($scope, $state, server, status) {
   $scope.information = {};
   $scope.studyid = $state.params.studyId;
   $scope.status = new status(true);
   
   
   $scope.reload = function() {
	   
	   $scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.getRequiredInformationSetup($scope.studyid).url))
		.then(function(data) { 								
			$scope.information = data.data;	
			
			//if ($scope.information.anonymous) $scope.information.identity="ANONYMOUS";
		});
	   
	   $scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url))
	    .then(function(data) { 				
			$scope.study = data.data;
			$scope.study.recordQuery = undefined;
		});
   };
   
   $scope.setRequiredInformation = function() {
	   var params = JSON.parse(JSON.stringify($scope.information));
	   /*if (params.identity == "ANONYMOUS") {
		   params.identity = "NONE";
		   params.anonymous = true;
	   } else {
		   params.anonymous = false;
	   }*/
	   		   
	   server.post(jsRoutes.controllers.research.Studies.setRequiredInformationSetup($scope.studyid).url, params).
		then(function(data) { 				
		    $scope.reload();
		    $scope.requiredDataOk = true;
		}, function(err) {
			$scope.error = err.data;			
		});  
   };
   
   $scope.requiredDataChange = function() {
	   if ($scope.information.identity == "DEMOGRAPHIC") {
		   $scope.information.anonymous = false;		  
	   }
	   $scope.requiredDataOk = false;
   };
   
   $scope.groupChange = function() {
	 $scope.groupSaveOk = false;  
   };
   
   $scope.addGroup = function() {
	   $scope.groupSaveOk = false;
	   if (!$scope.study.groups) { $scope.study.groups = []; }
	   
	   $scope.study.groups.push({ name:"", description:"" });
   };
   
   $scope.deleteGroup = function(group) {
	   $scope.groupSaveOk = false;
	   $scope.study.groups.splice($scope.study.groups.indexOf(group), 1);
	   $scope.groupschanged = true;
   };
   
   $scope.saveGroups = function() {
	 $scope.groupschanged = false;
	 $scope.status.doAction("groups", server.put(jsRoutes.controllers.research.Studies.update($scope.studyid).url, JSON.stringify({ "groups" : $scope.study.groups })))
	 .then(function() { $scope.groupSaveOk = true; });	 
   };
   
   $scope.studyLocked = function() {
		 return (!$scope.study) || ($scope.study.validationStatus !== "DRAFT" && $scope.study.validationStatus !== "REJECTED") || !$scope.study.myRole.setup;
   };
   
   $scope.reload();
}]);
