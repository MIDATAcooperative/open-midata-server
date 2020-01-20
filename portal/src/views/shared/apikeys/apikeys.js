angular.module('portal')
.controller('ApiKeysCtrl', ['$scope', '$state', 'session', 'status', 'services', 'views', function($scope, $state, session, status, services, views) {

	$scope.status = new status(true);
			
	loadServices = function(userId, studyId) {	
        
        if (studyId) {
            $scope.status.doBusy(services.listByStudy(studyId))
		    .then(function(data) {
		        $scope.services = data.data;						
		    });
        } else {
            $scope.status.doBusy(services.list())
		    .then(function(data) {
		        $scope.services = data.data;						
		    });
        }
				
    };
    
    $scope.addKey = function(service) {
        $scope.status.doAction("add", services.addApiKey(service._id))
        .then(function(result) {
            $scope.showkey = result.data;

            views.setView("apikey", { key : result.data }, "API Keys");
        });
    };
			
	session.currentUser.then(function(userId) { loadServices(userId, $state.params.studyId); });
	

}]);