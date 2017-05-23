angular.module('surveys')
.controller('EditSurveyCtrl', ['$scope', '$state', 'editor', 'midataServer', 'midataPortal', 
	function($scope, $state, editor, midataServer, midataPortal) {
			    
	   $scope.init = function(id) {
		   console.log(id);
		   var start;
		   if (!id) {
			   start = editor.createQuestionnaire();
		   } else {
			   start = editor.selectQuestionnaire(id);
		   }
		   start.then(function() {
		     $scope.survey = editor.getQuestionnaire();
		   });
	   };
	   
	   $scope.save = function() {
		   editor.saveQuestionnaire($scope.survey).then(function() {		   
		     $state.go("^.previewsurvey", { questionnaire : $scope.survey.id });
		   });
	   };
	   
	   $scope.cancel = function() {
		  $state.go("^.pick");  
	   };
	   	   
	   $scope.init($state.params.questionnaire);
				
	}
]);