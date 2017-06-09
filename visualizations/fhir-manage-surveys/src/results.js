angular.module('surveys')
.controller('ResultsCtrl', ['$scope', '$state', 'processResponses', 'response', 'midataServer', 'midataPortal', 
	function($scope, $state, processResponses, response, midataServer, midataPortal) {
			    
	   $scope.init = function(id) {
		   console.log(id);
		   
		   response.searchByQuestionnaire(id).then(function(responses) {
			  $scope.results = processResponses.process(responses); 
		   });
	   };
	   
	   	   
	   $scope.cancel = function() {
		  $state.go("^.pick");  
	   };
	   	   
	   $scope.init($state.params.questionnaire);
				
	}
]);