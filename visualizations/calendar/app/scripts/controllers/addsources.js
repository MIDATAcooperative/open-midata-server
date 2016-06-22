'use strict';

angular.module('calendarApp')
  .controller('SourcesCtrl', ['$scope', '$filter', '$routeParams', '$location', 'midataServer', 'midataPortal', 'eventProvider',
    function ($scope, $filter, $routeParams, $location, midataServer, midataPortal, eventProvider) {
	
	  $scope.sources = [];
	  $scope.persons = [];
	  $scope.operators = [ "<", ">" ];
	  $scope.valuesToLabel = { content : {} };
	  
	  $scope.reloadSummary = function() {
          var p = { "format" : ["fhir/Observation" ] };
          midataServer.getSummary(eventProvider.authToken, "SINGLE", p, ["ownerName" ])
          .then(function(results) {
              var entries = results.data;
              var aOwner = {};
              var aContent = {};
              var codes = [];
              
              angular.forEach(entries, function(entry) {                                                                        
                  angular.forEach(entry.owners, function(on) { aOwner[on] = entry.ownerNames[0]; });
                  angular.forEach(entry.contents, function(on) { aContent[on] = true; });                  
              });
              
              angular.forEach(aOwner, function(v,k) { $scope.persons.push({ label : v, id : k, selected : true }); });
              angular.forEach(aContent, function(v,k) { 
            	  var e = eventProvider.getTlb(k) || { label : k, id : k, selected : eventProvider.hasContent(k) };
            	  $scope.sources.push(e); 
            	  codes.push(k);
              });   
              
             midataServer.searchContent($scope.authToken, { content : codes }, [ "content", "label" ])
         	 .then(function(result) {
         		angular.forEach(result.data, function(d) {
         			$scope.valuesToLabel.content[d.content] = d.label[midataPortal.language] || d.label['en'];
         		});
         	 });
          });
      };
      
      $scope.mlabel = function(source) {
    	  console.log(source);
    	  if (!source) return "null";
    	 return $scope.valuesToLabel.content[source.id] || source.label;  
      };
	  
      $scope.reloadSummary();
      
      $scope.submit = function() {
    	 var add = [];
    	 angular.forEach($scope.sources, function(source) { 
    		 if (source.selected) { 
    			 add.push(source.id);
    			 if (source.tlb) eventProvider.setTlb(source); else eventProvider.clearTlb(source.id);
    		 }
    	  });
    	 eventProvider.setContents(add);
    	 midataServer.setConfig(eventProvider.authToken, { sources : $scope.sources });
    	 $location.path("/cal/display");
      };
            
  }]);