angular.module('portal')
.directive('auditlog', ['views', function (views) {
    return {
      templateUrl: 'assets/directives/auditlog.html',
      restrict: 'E',
      transclude: false, 
      scope : {
    	"patient" : "@",
    	"entity" : "@",
    	"all" : "@"
      },
      controller : ['$scope', 'status', 'fhir', 'paginationService', function($scope, status, fhir, paginationService) {

    		$scope.status = new status(true);        	
    		
    		$scope.page = { nr : 1 };
    		
    	    
    		$scope.reload = function() {
    			var crit = {};
    			if ($scope.patient) crit.patient = $scope.patient;
    			if ($scope.entity) crit.entity = $scope.entity;
    			console.log(crit);
    			$scope.status.doBusy(fhir.search("AuditEvent", crit))
    			.then(function(log) {
    				//if (!comeback) paginationService.setCurrentPage("membertable", 1); // Reset view to first page
    				$scope.log = log;
    				console.log($scope.log);
    			});
    			
    			
    		};    		    			    	
    		
    		$scope.$watch('patient', function(p) { if (p) $scope.reload(); });
    		$scope.$watch('entity', function(p) { if (p) $scope.reload(); });
    		if ($scope.all) $scope.reload();

    	}]
    };
}]);
