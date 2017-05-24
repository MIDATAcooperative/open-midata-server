angular.module('surveys')
.controller('PickCtrl', ['$scope', '$state', 'midataServer', 'midataPortal', 
	function($scope, $state, midataServer, midataPortal) {
	    var fhir = function(record) {
	       var result = record.data;
	       result.id = record._id;
	       result.meta = { version : record.version };
	       return result;
	    };
	
	    $scope.loadSurveys = function() {
			midataServer.fhirSearch(midataServer.authToken, "Questionnaire")
			.then(function(results) {
			   $scope.surveys = [];
			   angular.forEach(results.data.entry, function(d) { $scope.surveys.push(d.resource); });
			   $scope.loadAnswers();   			   		  
			});
	   	};
	   	
	   	$scope.loadAnswers = function() {
				midataServer.fhirSearch(midataServer.authToken, "QuestionnaireResponse")
				.then(function(results2) {
				  var surveyIdx = {};
				  angular.forEach($scope.surveys, function(s) { surveyIdx["Questionnaire/"+s.id] = s;s.answerCount = 0; });
				  angular.forEach(results2.data.entry, function(record) {
					  var r = record.resource;
					  if (r.questionnaire && r.questionnaire.reference) {
						  var survey = surveyIdx[r.questionnaire.reference];
						  survey.answerCount++;
						  if (survey != null && (!survey.answered || survey.answered.created < new Date(r.authored))) {
							  survey.answered = { created : new Date(r.authored) };
						  }
					  } 
				  });	   
				});	
			};
	   	
	   	$scope.createNew = function() {
	   		$state.go("^.editsurvey");
	   	};
	   	     	     	
	   	$scope.startSurvey = function(survey) {	   		
	   		$state.go("^.previewsurvey", { questionnaire : survey.id });	   		
	   	};
	   	
	   	$scope.showResults = function(survey) {	   		
	   	  $state.go("^.results", { questionnaire : survey.id });	   		
	   	};
	   	
	   	$scope.edit = true;
	   	$scope.loadSurveys();    
				
	}
]);