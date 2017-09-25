angular.module('portal')
.controller('AuditLogCtrl', ['$scope', '$state', 'views', 'status', 'fhir', 'administration', 'paginationService', 'session', function($scope, $state, views, status, fhir, administration, paginationService, session) {

	$scope.status = new status(true);    

	$scope.auditlog = {};
	var now = new Date();
	$scope.criteria = { from: new Date(), to : new Date(), days:2 };	
	$scope.page = { nr : 1 };
	
	$scope.datePickers = {};
    $scope.dateOptions = {
       formatYear: 'yy',
       startingDay: 1
    };
	
    $scope.refresh = function() {
    	$scope.auditlog.reload();
    };	
    
    $scope.recalc = function() {
    	$scope.criteria.from = new Date($scope.criteria.to);
    	$scope.criteria.from.setDate($scope.criteria.to.getDate() - $scope.criteria.days);
    };
	
    $scope.recalc();

}]);