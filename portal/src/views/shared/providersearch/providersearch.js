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
.controller('ProviderSearchResultsCtrl', ['$scope', 'server', 'hc', '$state', 'status', 'views', 'studies', function($scope, server, hc, $state, status, views, studies) {
		
    $scope.status = new status(true);
    $scope.criteria = { name : $state.params.name, city : $state.params.city, onlymine : false };
    $scope.view = $scope.def ? views.getView($scope.def.id) : views.getView("providersearch");
    $scope.role = $state.current.data.role;
    console.log($scope.role);
    var dosearch = function(crit) {
    	$scope.status.doBusy(hc.search(crit, ["firstname", "lastname", "city", "zip", "address1", "role"]))
    	.then(function(data) {
    		$scope.providers = data.data;
    	});
    };
    
    $scope.search = function() {
    	var crit = {};
    	if ($scope.criteria.city !== "") crit.city = $scope.criteria.city;
    	if ($scope.criteria.name !== "") crit.name = $scope.criteria.name;
    	if ($scope.criteria.onlymine) {
    		$scope.status.doBusy(hc.list()).then(function(data) {
    			var ids = [];
    			angular.forEach(data.data, function(x) {     			
    				angular.forEach(x.authorized, function(a) { ids.push(a); });
    			});    			
    			crit._id = ids;
    			dosearch(crit);
    		});
    	} else {
    		dosearch(crit);	
    	}
    	    	
    };
    
    $scope.addConsent = function(prov) {
    	if ($scope.view.setup && $scope.view.setup.studyId) {
    	   studies.updateParticipation($scope.view.setup.studyId, { add : { providers : [ prov._id ]}})
    	   .then(function() {
    	     views.changed($scope.view.id);
    	     views.disableView($scope.view.id);
    	   });
    	} else if ( $scope.view.setup && $scope.view.setup.callback ) {
    		$scope.view.setup.callback(prov);
    		views.disableView($scope.view.id);
    	} else {
    	   $state.go("^.newconsent", { authorize : prov._id });
    	}
    };
    
    if ($scope.view.active) $scope.search(); else { $scope.status.isBusy = false; }
    
		
}]);