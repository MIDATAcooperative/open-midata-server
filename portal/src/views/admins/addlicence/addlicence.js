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
.controller('AddLicenceCtrl', ['$scope', '$state', 'server', 'status', 'languages', 'studies', 'apps', 'users', 'usergroups', function($scope, $state, server, status, languages, studies, apps, users, usergroups) {
	
	// init
	$scope.error = null;
	$scope.licence = {  };
	$scope.status = new status(false, $scope);
	$scope.allowDelete = $state.current.allowDelete;
	
	$scope.datePickers = {};
    $scope.dateOptions = {
       formatYear: 'yy',
       startingDay: 1
    };
    
    $scope.entities = ["USER","USERGROUP","ORGANIZATION"];
    $scope.roles = ["MEMBER", "PROVIDER", "RESEARCH", "DEVELOPER"];
    
    
	$scope.loadLicence = function(licenceId) {
		$scope.status.doBusy(server.post(jsRoutes.controllers.Market.searchLicenses().url,JSON.stringify({ properties : { _id : licenceId } })))
		.then(function(data) { 
			$scope.licence = data.data[0];						
		});
	};
	
	// register app
	$scope.updateLicence = function() {
		
		$scope.submitted = true;
		
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		
		if (! $scope.myform.$valid) return;
		
		if ($scope.licence._id == null) {
			$scope.status.doAction('submit', server.post(jsRoutes.controllers.Market.addLicence().url,JSON.stringify($scope.licence)))
			.then(function(data) { $state.go("^.licenses"); });
		} else {			
		    //$scope.status.doAction('submit', news.update($scope.newsItem))
		    //.then(function() { $state.go("^.news"); });
		}
	};
	
	
	
	if ($state.params.newsId != null) { $scope.loadNews($state.params.newsId); }
	else { $scope.status.isBusy = false; }
	
	/*
	$scope.studyselection = function(study, field) {
		  console.log(study);
		  console.log(field);
		   $scope.status.doBusy(studies.search({ code : study.code }, ["_id", "code", "name" ]))
			.then(function(data) {
				if (data.data && data.data.length == 1) {
				  $scope.newsItem[field] = data.data[0]._id;
				  study.code = data.data[0].code;
				  study.name = data.data[0].name;
				}
			});
	};*/
	
	$scope.appselection = function(app, field) {
		   $scope.status.doBusy(apps.getApps({ filename : app }, ["_id", "filename", "name", "orgName", "type", "targetUserRole"]))
			.then(function(data) {
				if (data.data && data.data.length == 1) {
				  $scope.licence[field] = data.data[0]._id;
				  $scope.app = data.data[0];				  
				}
			});
	};
	
	$scope.licenseeChange = function() {
		if ($scope.licence.licenseeType == "USER") {
			users.getMembers({ email : $scope.licence.licenseeName, role : $scope.licence.role }, ["_id","email","firstname","lastname"])
			.then(function(result) {
				if (result.data.length==1) {
					var user = result.data[0];				
					$scope.licence.licenseeId = user._id;
					$scope.licence.licenseeName = user.email;
					$scope.user = user;
				} else {
					$scope.licence.licenseeId = null;
					$scope.user = null;
				}
				$scope.usergroup = null;
			});
		} else if ($scope.licence.licenseeType == "USERGROUP") {
			usergroups.search({ name : $scope.licence.licenseeName, status : "ACTIVE" }, ["_id", "name"])
			.then(function(result) {
				if (result.data.length == 1) {
					$scope.usergroup = result.data[0];
					$scope.licence.licenseeId = $scope.usergroup._id;
					$scope.licence.licenseeName = $scope.usergroup.name;
				}
				$scope.user = null;
			});
		} else if ($scope.licence.licenseeType == "ORGANIZATION") {
			$scope.user = null;
			$scope.usergroup = null;
			users.getMembers({ email : $scope.licence.licenseeName, role : "PROVIDER" }, ["_id","email","firstname","lastname","provider"])
			.then(function(result) {
				if (result.data.length==1) {
					var user = result.data[0];				
					$scope.licence.licenseeId = user.provider;
					//$scope.licence.licenseeName = user.email;					
				} else {
					$scope.licence.licenseeId = null;				
				}				
			});
		}
	};

		
	
	$scope.status.doBusy(apps.getApps({  }, ["creator", "developerTeam", "filename", "name", "description", "type", "targetUserRole" ]))
	.then(function(data) { 
		$scope.apps = data.data;			
	});
}]);