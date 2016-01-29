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
	          dayClick: $scope.dayClick,
	          eventDrop: $scope.alertOnDrop,
	          eventResize: $scope.alertOnResize,
	          viewRender: function(view, element) {
	              console.log("View Changed: ", view.visStart, view.visEnd, view.start, view.end);
	          }
	        }
	      };
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
            
      
      $scope.dayClick = function(date, jsEvent, view) {

          alert('Clicked on: ' + date.format());

          alert('Coordinates: ' + jsEvent.pageX + ',' + jsEvent.pageY);

          alert('Current view: ' + view.name);

          // change the day's background color just for fun
          //$(this).css('background-color', 'red');
      };
      
      $scope.init();
    
      
    }]);
