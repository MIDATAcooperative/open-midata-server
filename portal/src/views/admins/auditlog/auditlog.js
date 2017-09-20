angular.module('portal')
.controller('AuditLogCtrl', ['$scope', '$state', 'views', 'status', 'fhir', 'administration', 'paginationService', 'session', function($scope, $state, views, status, fhir, administration, paginationService, session) {

	$scope.status = new status(true);    

	$scope.auditlog = {};
	$scope.criteria = { from: new Date(), to : new Date() };
	$scope.page = { nr : 1 };
	
	$scope.datePickers = {};
    $scope.dateOptions = {
       formatYear: 'yy',
       startingDay: 1
    };
	
    $scope.refresh = function() {
    	$scope.auditlog.reload();
    };	
	

}]);