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
.controller('UsageStatsCtrl', ['$scope', '$state', 'status', 'server',  '$filter', 'apps', function($scope, $state, status, server, $filter, apps) {

	$scope.status = new status(true);    
	$scope.sortby="-date"; 
	var now = new Date();
	$scope.criteria = { from: new Date(), to : new Date(), days:$state.params.appId ? 30 : 7 };
	
	$scope.datePickers = {};
    $scope.dateOptions = {
       formatYear: 'yy',
       startingDay: 1
    };
	    
    $scope.recalc = function() {
    	$scope.criteria.from = new Date($scope.criteria.to);
    	$scope.criteria.from.setDate($scope.criteria.to.getDate() - $scope.criteria.days);
    	$scope.refresh();
    };
		
	$scope.loadApp = function(appId) {
		$scope.status.doBusy(apps.getApps({ "_id" : appId }, ["creator", "creatorLogin", "filename", "name", "description", "tags", "targetUserRole","i18n", "orgName", "publisher"]))
		.then(function(data) { 
			$scope.app = data.data[0];			
		})
	};
	
	$scope.refresh = function() {
		//var limit = new Date();
		//limit.setDate(limit.getDate()-30);
		var data = { "properties" : { "date" : { "$gte" : $scope.criteria.from, "$lte" : $scope.criteria.to }}};
		if ($state.params.appId) data.properties.object = $state.params.appId;
		$scope.status.doBusy(server.post(jsRoutes.controllers.admin.Administration.getUsageStats().url, JSON.stringify(data)))
		.then(function(result) {
			//$scope.result = result.data;
			
			var bykey = {};
			$scope.actions = ["REGISTRATION","LOGIN","REFRESH","INSTALL","GET","POST","PUT","DELETE"];
			var list = [];
			angular.forEach(result.data, function(r) {
				var k = r.date+r.object;
				var grp = bykey[k];
				if (!grp) {
					bykey[k] = grp = { object : r.object, date : r.date, objectName : r.objectName, actions : {} };
					list.push(grp);
				}
				grp.actions[r.action] = r;
			});
			
			$scope.result = list;
		});				
	};
	
	$scope.setSort = function(key) {	
		console.log(key);
		if ($scope.sortby==key) $scope.sortby = "-"+key;
		else { $scope.sortby = key; }
	};
	
	$scope.getSortkey = function(ac) {
		return "actions."+ac+".count";
	};
	
	if ($state.params.appId) $scope.loadApp($state.params.appId);
    $scope.recalc();

}]);