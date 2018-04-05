'use strict';

angular.module('calendarApp')
  .controller('MainCtrl', ['$scope', '$filter', '$routeParams', '$timeout', '$location', 'midataServer', 'midataPortal', 'eventProvider', 'uiCalendarConfig',
    function ($scope, $filter, $routeParams, $timeout, $location, midataServer, midataPortal, eventProvider, uiCalendarConfig) {
      window.scope = $scope;
      var authToken = $routeParams.authToken;
      if (authToken) eventProvider.authToken = authToken;
      midataPortal.autoresize();
      
      $scope.operators = [ "<", ">" ];
      
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
    	 $scope.init();
    	 console.log(config);
     	 eventProvider.setCriteria(config);     	 
      };
	    
      $scope.load = function() {
    	  if (eventProvider.inited) {
    		  $scope.init();
    	  } else {
    		  var params = $location.search();
    		  
    		  if (params.content) {
    			  //eventProvider.setContents(params.content.split(","));
    			  $scope.init();
    		  } else {
    		  
			      midataServer.getConfig(eventProvider.authToken)
			      .then(function(result) {
			    	 if (result.data && result.data.cal) {
			    	    $scope.useConfig(result.data);
			    	 } else {
			    		$scope.init();
			    		$location.url('/cal/add'); 
			    	 }
			    	 
			      });
    		  }
    	  }
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
           { id : "Observations", events : function(start,end,timezone,callback) { eventProvider.queryObservations(eventProvider.authToken, start, end, callback); } },
           { id : "Appointments", color : "#0000a0", textColor : "#ffffff", events : function(start,end,timezone,callback) { eventProvider.queryAppointments(eventProvider.authToken, start, end, callback); } },
           { id : "MedicationStatement", color : "#a000a0", textColor : "#ffffff", events : function(start,end,timezone,callback) { eventProvider.queryMedicationStatements(eventProvider.authToken, start, end, callback); } }
      ];
            
      
      $scope.eventClick = function(event, jsEvent, view) {
    	  console.log(event);
    	  $scope.selectedEvent = event;
    	  $scope.tlb = event.tlb ? (eventProvider.getTlb(event.tlb) || { id : event.tlb })  : null;
    
          $("#details").modal("show");          
      };
      
      $scope.tlbModified = function() {
    	  $scope.tlb.modified = true;
      };
      
      $scope.checkTlb = function() {    	  
    	  if ($scope.tlb && $scope.tlb.modified) {
    		  $scope.tlb.modified = false;
    		  if ($scope.tlb.limit == "" || $scope.tlb.limit == null) { 
    			eventProvider.clearTlb($scope.tlb.id);
    		  } else {
    	        eventProvider.setTlb($scope.tlb);
    		  }
    	      midataServer.setConfig(eventProvider.authToken, eventProvider.getCriteria());
    	      console.log(uiCalendarConfig);
    	      uiCalendarConfig.calendars.calendar.fullCalendar('refetchEvents');
    	      
    	  }
      };
      
      $scope.showDetails = function(event) {
    	  midataPortal.openApp("page", event.viewer, { id : event.id, path :"/record" });
      };
      
      $scope.load();
    
      
    }])
     .controller('PreviewCtrl', ['$scope', 
    function ($scope) {;
      $scope.today = new Date();
     }]);