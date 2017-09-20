angular.module('portal')
.directive('auditlog', ['views', function (views) {
    return {
      templateUrl: 'assets/directives/auditlog.html',
      restrict: 'E',
      transclude: false, 
      scope : {
    	"patient" : "@",
    	"entity" : "@",
    	"all" : "@",
    	"from" : "=",
    	"to" : "=",
    	"api" : "="
      },
      controller : ['$scope', 'status', 'fhir', 'paginationService', function($scope, status, fhir, paginationService) {

    		$scope.status = new status(true);        	
    		
    		$scope.page = { nr : 1 };
    		
    	    
    		$scope.reload = function() {
    			var crit = {};
    			if ($scope.patient) crit.patient = $scope.patient;
    			if ($scope.entity) crit.entity = $scope.entity;
    			if ($scope.from && $scope.to) crit.date = ["sa"+$scope.from.toISOString(), "eb"+$scope.to.toISOString()];
    			console.log(crit);
    			$scope.status.doBusy(fhir.search("AuditEvent", crit))
    			.then(function(log) {
    				//if (!comeback) paginationService.setCurrentPage("membertable", 1); // Reset view to first page
    				$scope.log = log;
    				console.log($scope.log);
    			});
    			
    			
    		};    		    			    	
    		
    		var api = $scope.api || {};
    		api.reload = $scope.reload;
    		
    		$scope.$watch('patient', function(p) { if (p) $scope.reload(); });
    		$scope.$watch('entity', function(p) { if (p) $scope.reload(); });
    		$scope.$watch('from', function(p) { if (p) $scope.reload(); });
    		$scope.$watch('to', function(p) { if (p) $scope.reload(); });
    		if ($scope.all) $scope.reload();

    	}]
    };
}]);
