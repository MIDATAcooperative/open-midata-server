angular.module('views')
.controller('ProviderSearchResultsCtrl', ['$scope', 'server', 'hc', '$state', 'status', function($scope, server, hc, $state, status) {
		
    $scope.status = new status(true);
    $scope.criteria = { name : $state.params.name, city : $state.params.city };
            
    $scope.search = function() {
    	var crit = {};
    	hc.search(crit, ["firstname", "sirname", "city", "zip", "address1"])
    	.then(function(data) {
    		$scope.providers = data.data;
    	});
    };
    
    $scope.addConsent = function(prov) {
    	$state.go("^.newconsent", { authorize : prov._id.$oid });
    };
    
    $scope.search();
    
		
}]);