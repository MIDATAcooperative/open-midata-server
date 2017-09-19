angular.module('portal')
.controller('AuditLogCtrl', ['$scope', '$state', 'views', 'status', 'fhir', 'administration', 'paginationService', 'session', function($scope, $state, views, status, fhir, administration, paginationService, session) {

	$scope.status = new status(true);    
	
	$scope.criteria = {};
	$scope.page = { nr : 1 };
	
    
	

}]);