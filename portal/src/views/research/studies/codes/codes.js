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
.controller('CodesCtrl', ['$scope', '$state', 'server', 'status', function($scope, $state, server, status) {
	
	$scope.studyid = $state.params.studyId;
	$scope.codes = null;
	$scope.newcodes = { count:1, reuseable:"true", manually:"false", group:"" };
	$scope.status = new status(false, $scope);   
	$scope.createnew = false;
	$scope.blocked = false;
	$scope.submitted = false;
	$scope.error = null;
		
	$scope.reload = function() {
			
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url))
		.then(function(data) { 				
			$scope.study = data.data;			
		});
		
		$scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.listCodes($scope.studyid).url)).
			then(function(data) { 				
				$scope.codes = data.data;
				$scope.loading = false;
				$scope.createnew = false;
				$scope.error = null;
			}, function(err) {
				$scope.error = err.data.type;
				$scope.blocked = true;
				$scope.createnew = false;
			});
	};
	
	$scope.generate = function() {
		$scope.submitted = true;	
		if ($scope.newcodes.error && $scope.newcodes.error.field && $scope.newcodes.error.type) $scope.myform[$scope.newcodes.error.field].$setValidity($scope.newcodes.error.type, true);
		$scope.newcodes.error = null;
		if (! $scope.myform.$valid) return;
		
		$scope.newcodes.error = null;
		
		// send the request
		var data = $scope.newcodes;		
		
		server.post(jsRoutes.controllers.research.Studies.generateCodes($scope.studyid).url, JSON.stringify(data)).
			then(function(url) { $scope.reload(); }, function(err) {
				$scope.newcodes.error = err.data;
				if (err.field && err.type) $scope.myform[err.field].$setValidity(err.type, false);			
			});
	};
	
	$scope.showcreatenew = function() {		
		$scope.createnew = true;
	};
	
	$scope.updateCodeCount = function() {
		if ($scope.newcodes.manually=="true") {
			if ($scope.newcodes.codes === undefined) {
				$scope.newcodes.codes = [];
				for (var i=0;i<$scope.newcodes.count;i++) $scope.newcodes.codes.push("");
			} else {
				while ($scope.newcodes.codes.length<$scope.newcodes.count) $scope.newcodes.codes.push("x");
				if ($scope.newcodes.codes.length > $scope.newcodes.count) {
					let diff = $scope.newcodes.codes.length - $scope.newcodes.count;
					$scope.newcodes.codes.splice($scope.newcodes.count, diff);
				}
			}
		} else $scope.newcodes.codes = undefined;
	};
	
	$scope.reload();
	
}]);