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
.controller('MemberDetailsCtrl', ['$scope', '$state', 'server', 'views', 'circles', 'session', 'status', function($scope, $state, server, views, circles, session, status) {
	
	$scope.memberid = $state.params.user;
	$scope.member = {};	
	$scope.status = new status(true);
	$scope.data = { consent : null };
		
	views.reset();
	views.link("patient_records", "record", "record");
	$scope.reload = function() {
			
		$scope.status.doBusy(server.get(jsRoutes.controllers.providers.Providers.getMember($scope.memberid).url))
		.then(function(results) {
			    var data = results.data;
				$scope.member = data.member;
				$scope.consents = data.consents;
				$scope.backwards = data.backwards;
			
				$scope.activeFound = false;
				angular.forEach($scope.consents, function(consent) { if (consent.status=="ACTIVE" || consent.status=="FROZEN") $scope.activeFound = true; });
				
				if (data.memberkey) {
				  views.setView("patient_records", { aps : $scope.memberkey._id, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowAdd: false, type : "memberkeys"});
				} else {
				  views.disableView("patient_records");
				}
			});
	};
	
	$scope.selectConsent = function(consent) {
		$scope.hideAdd = false;
		$scope.consent = consent;
		
		if ($scope.consent != null && ($scope.consent.status=="ACTIVE" || $scope.consent.status=="FROZEN")) {
			views.setView("patient_records", { aps : $scope.consent._id, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowAdd : false, type : "memberkeys" });			
		} else {
			views.disableView("patient_records");
		}
	};
	
	var addDataConsent = function(backConsent) {
		$scope.data.consent = $scope.consent = null;
		$scope.hideAdd = true;
		views.setView("patient_records", { aps : backConsent._id, properties : { } , fields : [ "ownerName", "created", "id", "name" ], allowAdd : true, type : "hcrelated" });
	};
	
	$scope.addData = function() {
		if ($scope.backwards.length > 0) {
			var consent = $scope.backwards[0];
			addDataConsent(consent);
		} else {
			circles.createNew({ type : "HCRELATED", name : $scope.member.firstname+" "+$scope.member.lastname })
			.then(function(data) {
				circles.addUsers(data.data._id, [ $scope.memberid ])
				.then(function(xdata) {
					$scope.backwards.push(data.data);
					addDataConsent(data.data);
				});
			});
		}
	};
		
		
	$scope.reload();
	
	// For adding new records
	$scope.error = null;
	
	$scope.userId = null;
	$scope.apps = [];
	
	
	// get current user
	session.currentUser.then(
		function(userId) {
			$scope.userId = userId;
		});
	

	
	/*
	$scope.createOrImport = function(app) {
		if (app.type === "create") {
			$state.go("^.createpatientrecord", { memberId : $scope.member._id, appId : app._id, consentId : $scope.consent._id });			
		} else {
			$state.go("^.importrecords", { appId : app._id });			
		}
	};
	*/
	

		
	
}]);