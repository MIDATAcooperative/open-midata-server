'use strict';

angular.module('calendarApp')
  .controller('MainCtrl', ['$scope', '$filter', '$routeParams', '$timeout', 'midataServer', 'midataPortal', 'eventProvider',
    function ($scope, $filter, $routeParams, $timeout, midataServer, midataPortal, eventProvider) {
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
	          }
	        }
	      };
      };
	    
      $scope.load = function() {
	      midataServer.getConfig(eventProvider.authToken)
	      .then(function(result) {
	    	 if (result.data && result.data.sources) {
	    		 var add = [];
	    		 angular.forEach(result.data.sources, function(source) { 
	        		 if (source.selected) { 
	        			 add.push(source.id);
	        			 if (source.tlb) eventProvider.setTlb(source); else eventProvider.clearTlb(source.id);
	        		 }
	        	  });
	        	 eventProvider.setContents(add);
	    	 } 
	    	 $scope.init();
	      });
      };
      
      $scope.loadTest = function( start, end, timezone, callback ) { 
     	 console.log(start.calendar());
     	 console.log(end.calendar());
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
      
      $scope.load();
    
      
    }])
     .controller('PreviewCtrl', ['$scope', 
    function ($scope) {;
      $scope.today = new Date();
     }]);