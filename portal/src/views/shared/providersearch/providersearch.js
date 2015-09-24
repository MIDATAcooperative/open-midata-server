angular.module('views')
.controller('ProviderSearchResultsCtrl', ['$scope', 'server', 'hc', '$state', 'status', 'views', 'studies', function($scope, server, hc, $state, status, views, studies) {
		
    $scope.status = new status(true);
    $scope.criteria = { name : $state.params.name, city : $state.params.city, onlymine : false };
    $scope.view = $scope.def ? views.getView($scope.def.id) : views.getView("providersearch");
    
    var dosearch = function(crit) {
    	$scope.status.doBusy(hc.search(crit, ["firstname", "lastname", "city", "zip", "address1"]))
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
    	   studies.updateParticipation($scope.view.setup.studyId, { add : { providers : [ prov._id.$oid ]}})
    	   .then(function() {
    	     views.changed($scope.view.id);
    	     views.disableView($scope.view.id);
    	   });
    	} else if ( $scope.view.setup && $scope.view.setup.callback ) {
    		$scope.view.setup.callback(prov);
    		views.disableView($scope.view.id);
    	} else {
    	   $state.go("^.newconsent", { authorize : prov._id.$oid });
    	}
    };
    
    if ($scope.view.active) $scope.search(); else { $scope.status.isBusy = false; }
    
		
}]);