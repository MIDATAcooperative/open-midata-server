angular.module('surveys')
.controller('SurveyCtrl', ['$scope', '$location', '$filter', 'midataServer', 'midataPortal',
	function($scope, $location, $filter, midataServer, midataPortal) {
		
	    midataPortal.autoresize();
	    
		var path = $location.path().split("/");
		console.log(path[2]);
		$scope.authToken = path[1];
		$scope.edit = true; //path.length > 2 ? path[2] == "editor" : false;
		console.log($scope.edit);
		
		$scope.nav = { status : "pick" };
				
	}
])
.controller('PickSurveyCtrl', ['$scope', 'midataServer', 'currentSurvey', 'surveys',
	function($scope, midataServer, currentSurvey, surveys) {
	    $scope.loadSurveys = function() {
			midataServer.getRecords($scope.authToken, { format:"fhir/Questionnaire" }, ["name", "created", "data"])
			.then(function(results) {
			   $scope.surveys = [];
			   angular.forEach(results.data, function(d) { $scope.surveys.push(d.data); });
			   //$scope.loadAnswers();   			   		  
			});
     	};
     	
     	$scope.loadAnswers = function() {
			midataServer.getRecords($scope.authToken, { format:"fhir/QuestionnaireResponse", owner : "self" }, ["name", "created", "data"])
			.then(function(results2) {
			  var surveyIdx = {};
			  angular.forEach($scope.surveys, function(s) { surveyIdx["Questionnaire/"+s.id] = s; });
			  angular.forEach(results2.data, function(record) {
				  if (record.data.questionnaire != null) {
					  var survey = surveyIdx[record.data.questionnaire];
					  if (survey != null && (!survey.answered || survey.answered.created < record.created)) {
						  survey.answered = { created : record.created };
					  }
				  } 
			  });	   
			});	
		};
     	
     	$scope.createNew = function() {
     		currentSurvey.setSurvey(surveys.newSurvey());
     		$scope.nav.status = "editsurvey";
     	};
     	
     	$scope.startSurvey = function(survey) {
     		currentSurvey.setSurvey(survey);
     		$scope.nav.status = "editsurvey";
     	};
     	
     	$scope.loadSurveys();
    }
])
.controller('PreviewCtrl', ['$scope', 'midataServer', 'currentSurvey', 'surveys',
	function($scope, midataServer, currentSurvey, surveys) {
	    $scope.loadSurveys = function() {
			midataServer.getRecords($scope.authToken, { format:"fhir/Questionnaire" }, ["name", "created"])
			.then(function(results) {
			   $scope.surveys = results.data;			   
			   //$scope.loadAnswers();   			   		  
			});
     	};
     	     	     	
     	$scope.loadSurveys();
    }
])
.controller('EditSurveyCtrl', ['$scope', 'midataServer', 'currentSurvey', 'surveys',
     function($scope, midataServer, currentSurvey, surveys) {
	    
	    $scope.currentSurvey = currentSurvey;
	    
	    $scope.exit = function() {
	       $scope.nav.status = "pick";
	    };
	    
	    $scope.save = function() {
	    	
	    	/*angular.forEach($scope.activeSurvey.steps, function(step) {
				   step.result = undefined;
				   if (step.items != null) {
					   angular.forEach(step.items, function(item) {
						 item.result = undefined;  
					   });
				   }
				});*/
	    	currentSurvey.activeSurvey.answered = undefined;
	    	
			midataServer.createRecord($scope.authToken, { "name" : currentSurvey.activeSurvey.group.title, content :"Questionnaire", format : "fhir/Questionnaire" }, currentSurvey.activeSurvey)
			.then(function() { $scope.exit(); });
			
	    };
	    
	    $scope.test = function() {
	    	$scope.nav.status = "answer";
	    };
	    
	    $scope.addQuestion = function() {
	    	var q = surveys.newQuestion(currentSurvey.activeSurvey.group, "last");
	    	currentSurvey.setQuestion(q);
	    	$scope.nav.status = "editquestion";
	    };
	    
	    $scope.editQuestion = function(question) {
	    	currentSurvey.setQuestion(question);
	    	$scope.nav.status = "editquestion";
	    };
     }
])
.controller('EditQuestionCtrl', ['$scope', 'midataServer', 'currentSurvey', 'surveys',
     function($scope, midataServer, currentSurvey, surveys) {
	   
	    $scope.questionTypes = [
          { value : "boolean", label : "Boolean	: Answer is a yes/no answer." },
          { value : "decimal", label : "Decimal : Answer is a floating point number" },
          { value : "integer", label : "Integer	: Answer is an integer." },
          { value : "date", label : "Date :	Answer is a date." },
          { value : "string", label : "String :	Answer is a short (few words to short sentence) free-text entry." },
          { value : "text", label : "Text :	Answer is a long free-text entry." },
          { value : "choice", label : "Choice : Answer is a Coding drawn from a list of options." },
          { value : "open-choice", label : "Open Choice	: Answer is a Coding drawn from a list of options or a free-text entry." },
          { value : "quantity", label : "Quantity :	Answer is a combination of a numeric value and unit, potentially with a comparator (<, >, etc.)." }
          
	    ];
	    	    
	    $scope.currentSurvey = currentSurvey;
	   
	    
	    $scope.exit = function() {
	       $scope.nav.status = "editsurvey";
	    };
	    
	    $scope.ok = function() {
	    	$scope.nav.status = "editsurvey";
	    };
	    
	   	    	   
     }
])
.controller('AnswerSurveyCtrl', ['$scope', 'midataServer', 'currentSurvey', 'surveys',
     function($scope, midataServer, currentSurvey, surveys) {
	   	    	    	   
	    $scope.currentSurvey = currentSurvey;
	    currentSurvey.startAnswer();
	    	    
	    $scope.next = function() {
	    	if (!currentSurvey.next()) {
	    		$scope.nav.status = "editsurvey";
	    	}
	    };
	    
	    $scope.skip = function() {
	    	currentSurvey.next();
	    };
	    
	    $scope.prev = function() {
	    	if (!currentSurvey.prev()) {
	    		$scope.nav.status = "editsurvey";
	    	}
	    };
	    
	    $scope.exit = function() {
	       $scope.nav.status = "editsurvey";
	    };
	    
	    $scope.templateName = function(question) {
	    	return "question_"+question.type+".html";
	    };
	    
	    $scope.isChecked = function(question, option) {
	    	var answers = question.rQuestion.answer;
	    	for (var i=0;i<answers.length;i++) {
	    		if (answers[i].code == option.code) return true;
	    	}
	    	return false;
	    };
	    
	    $scope.changeChecked = function(question, option) {
	    	var answers = question.rQuestion.answer;
	    	for (var i=0;i<answers.length;i++) {
	    		if (answers[i].code == option.code) {
	    			answers.splice(i,1);
	    			return;
	    		}
	    	}
	    	answers.add(option);					    	
	    };
	    	    	    
	   	    	   
     }
])
.controller('FinishSurveyCtrl', ['$scope', 'midataServer', 'currentSurvey', 'surveys',
      function($scope, midataServer, currentSurvey, surveys) {
	
	$scope.success = false;
	midataServer.createRecord($scope.authToken, { name : currentSurvey.activeSurvey.group.title, content : "QuestionnaireResponse", format : "fhir/QuestionnaireResponse" }, currentSurvey.activeResponse)
	.then(function() {
		$scope.success = true;
	});
	
	$scope.back = function() {
		$scope.nav.status = "pick";
	};
}]);
