'use strict';

angular.module('calendarApp')
  .controller('MainCtrl', ['$scope', '$filter', '$routeParams', '$timeout', '$location', 'midataServer', 'midataPortal', 'eventProvider',
    function ($scope, $filter, $routeParams, $timeout, $location, midataServer, midataPortal, eventProvider) {
      window.scope = $scope;
      var authToken = $routeParams.authToken;
      if (authToken) eventProvider.authToken = authToken;
      midataPortal.autoresize();
      
      $scope.init = function() {
	      /* config object */
	      $scope.uiConfig = {
	        calendar:{
	          // height: 450,
	          editable: true,
	          header:{
	            left: 'month agendaWeek agendaDay',
	            center: 'title',
	            right: 'today prev,next'
	          },
	          eventClick: $scope.eventClick,
	          eventDrop: $scope.alertOnDrop,
	          eventResize: $scope.alertOnResize,
	          viewRender: function(view, element) {
	              console.log("View Changed: ", view.visStart, view.visEnd, view.start, view.end);
	          },
	          lang : midataPortal.language
	        }
	      };
      };
      
      $scope.useConfig = function(config) {
    	 var add = [];
 		 angular.forEach(config.sources, function(source) { 
     		 if (source.selected) { 
     			 add.push(source.id);
     			 if (source.tlb) eventProvider.setTlb(source); else eventProvider.clearTlb(source.id);
     		 }
     	  });
     	 eventProvider.setContents(add);
     	 $scope.init();
      };
	    
      $scope.load = function() {
    	  if (eventProvider.inited) {
    		  $scope.init();
    	  } else {
    		  var params = $location.search();
    		  
    		  if (params.content) {
    			  eventProvider.setContents(params.content.split(","));
    			  $scope.init();
    		  } else {
    		  
			      midataServer.getConfig(eventProvider.authToken)
			      .then(function(result) {
			    	 if (result.data && result.data.sources) {
			    	    $scope.useConfig(result.data);
			    	 } else {
			    		$scope.init();
			    		$location.url('/cal/add'); 
			    	 }
			    	 
			      });
    		  }
    	  }
      };
      
      $scope.loadTest = function( start, end, timezone, callback ) { 
     	 console.log(start.toDate());
     	 console.log(end.toDate());
     	 console.log(timezone);
     	 console.log(callback);
     	 
     	 eventProvider.query(eventProvider.authToken, start, end, callback);     	     	
       }; 
      
      $scope.eventSources = [
   /*       [                   
    { id : 1,
      title : "test",
      allDay : true, 	
      start : "2016-01-28",
      end : undefined, 	
      editable : true, 	
      color : "#ff0000", 	
      textColor : "#ffffff"
    }
           ], */
           $scope.loadTest
      ];
            
      
      $scope.eventClick = function(event, jsEvent, view) {
    	  console.log(event);
    	  $scope.selectedEvent = event;
          $("#details").modal("show");          
      };
      
      $scope.showDetails = function(event) {
    	  midataPortal.openApp("page", "fhir-observation", { id : event.id, path :"/record" });
      };
      
      $scope.load();
    
      
    }])
     .controller('PreviewCtrl', ['$scope', 
    function ($scope) {;
      $scope.today = new Date();
     }]);