'use strict';

angular.module('calendarApp')
  .controller('SourcesCtrl', ['$scope', '$filter', '$routeParams', '$location', 'midataServer', 'midataPortal', 'eventProvider',
    function ($scope, $filter, $routeParams, $location, midataServer, midataPortal, eventProvider) {
	
	  $scope.sources = [];	
	  $scope.persons = [];
	 
	  $scope.valuesToLabel = { content : {} };
	  
	  $scope.reloadSummary = function() {		  		  		            
		  $scope.sources.push({ label : "Observations", id : "observation", selected : eventProvider.hasContent("observation") });
          $scope.sources.push({ label : "Appointments", id : "appointment", selected : eventProvider.hasContent("appointment") });
          $scope.sources.push({ label : "MedicationStatements", id : "medicationStatement", selected : eventProvider.hasContent("medicationStatement") });
      };
      
      $scope.mlabel = function(source) {
    	
    	  if (!source) return "null";
    	 return $scope.valuesToLabel.content[source.id] || source.label;  
      };
	  
      $scope.reloadSummary();
      
      $scope.submit = function() {
    	 var add = [];
    	 angular.forEach($scope.sources, function(source) { 
    		 if (source.selected) { 
    			 eventProvider.getCriteria().cal[source.id] = true;    			 
    		 } else {
    			 eventProvider.getCriteria().cal[source.id] = undefined;
    		 }
    	  });
    	 
    	 midataServer.setConfig(eventProvider.authToken, eventProvider.getCriteria());
    	 $location.path("/cal/display");
      };
            
  }]);