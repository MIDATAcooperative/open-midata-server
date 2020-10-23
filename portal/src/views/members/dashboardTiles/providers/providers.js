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

angular.module('views')
.controller('ListHealthProviderCtrl', ['$scope', 'server', '$attrs', 'views', 'hc', 'status', function($scope, server, $attrs, views, hc, status) {
	
	$scope.results =[];
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
	
	$scope.reload = function() {
			
		$scope.status.doBusy(hc.list()).
		then(function(results) { 				
			$scope.results = results.data;				
			$scope.showNewHCRecords();
		});
	};
	
	$scope.confirm = function(memberKey) {
		hc.confirm(memberKey._id).then(function() { $scope.reload(); });		
	};
	
	$scope.reject = function(memberKey) {
		hc.reject(memberKey._id).then(function() { $scope.reload(); });
	};
	
	$scope.mayReject = $scope.mayConfirm = function(memberKey) {
		return memberKey.status == "UNCONFIRMED";
	};
	
	
	$scope.showNewHCRecords = function() {
		var creators = [];
		var aps = null;
		_.each($scope.results, function(hc) {		
			if (hc.provider) {
				creators.push(hc.provider);
				aps = hc.member;
			}
		});
		
		if (aps != null) {
		  views.setView("hcrecords", { aps : aps, properties: { "max-age" : 60*60*24*31, "creator" : creators }, fields : [ "creatorName", "created", "id", "name" ]});
		} else {
		  views.disableView("hcrecords");
		}
	};
	
	$scope.showRecords = function(mk) {
		views.setView("records", { aps : mk._id, properties: {}, fields : [ "ownerName", "created", "id", "name" ], allowAdd : true, type:"memberkeys"}, mk.name);
	};
	
	$scope.reload();
	
}]);