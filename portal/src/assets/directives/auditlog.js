angular.module('portal')
.directive('auditlog', ['views', function (views) {
    return {
      templateUrl: 'assets/directives/auditlog.html',
      restrict: 'E',
      transclude: false, 
      scope : {
    	"patient" : "@",
    	"entity" : "@",
    	"altentity" : "@",
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
    			if ($scope.entity && $scope.altentity) {
    				crit.entity = [$scope.entity, $scope.altentity];
    			} else if ($scope.entity) crit.entity = $scope.entity;
    			if ($scope.from && $scope.to) crit.date = ["sa"+$scope.from.toISOString(), "eb"+$scope.to.toISOString()];
    			
    			
    			if (!$scope.all && !$scope.patient && !$scope.entity && !$scope.from && !$scope.to) return;
    			
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
    		
    		$scope.$watchGroup(['patient','entity','altentity', 'from','to'], function() { $scope.reload(); });    		
    		if ($scope.all) $scope.reload();

    	}]
    };
}]);
