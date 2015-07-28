var surveys = angular.module('surveys', [ 'midata']);
surveys.controller('SurveyCtrl', ['$scope', '$http', '$location', '$filter', 'midataServer',
	function($scope, $http, $location, $filter, midataServer) {
		
		var path = $location.path().split("/");
		$scope.authToken = path[1];
		$scope.edit = path.length > 2 ? path[2] == "editor" : false;;
		
		$scope.types = [
           { id : "survey", label:"Survey" },
		   { id : "instructions", label:"Instructions" },
		   { id : "question", label:"Question" },
		   { id : "form", label:"Form with multiple questions" },
		   { id : "formitem", label:"Form Item" }
		];
		
		$scope.answerTypes = [
		   { id : "boolean", label : "Yes/No"},
		   { id : "textChoice", label : "Text Choices (Choose single choice)"},
		   { id : "multiTextChoice", label : "Text Choices (Select multiple choices)"},
		   { id : "integerAnswerWithUnit", label:"Integer number with unit" },
		   { id : "textUnlimited", label : "Text Answer (unlimited length)"},
		   { id : "textLimited", label : "Text Answer (short)"},
		];
		
		$scope.frequencies = [
		   { id : "once" , label : "Fill out once" },
		   { id : "daily" , label : "Fill out every day" },
		   { id : "weekly" , label : "Fill out every week" },
		   { id : "monthly" , label : "Fill out every month" },
		   { id : "yearly" , label : "Fill out every year" },
		   { id : "whenneeded" , label : "Fill out when needed" },
        ];
		
		$scope.surveys = [
		  {
			  type : "survey",
			  id : "S1",
			  title : "Test Survey",
			  steps : [
			           {
			        	   type : "instructions",
			        	   id : "I1",
			        	   title : "Selection Survey",
			        	   text : "This survey can help us understand your eligibility for the fitness study"
			           },
			           {
			        	   type : "question",
			        	   id : "Q1",
			        	   skipable : true,
			        	   title : "How old are you?",
			        	   answer : {
			        		   type : "integerAnswerWithUnit",
			        		   min : 18,
			        		   max : 90,
			        		   unit : "years"
			        	   }
			           }
			  ]
		  }                  
		];
		
		$scope.editor = {};
		
		$scope.activeSurvey = null;
		$scope.activeStep = null;
		$scope.activeStepIdx = -1;		
		
		$scope.okClick = function() {
			$scope.extractResult($scope.activeItems);
			$scope.showNext();
		};
		
		$scope.skip = function() {
			$scope.showNext();
		};
		
		
		
		$scope.showNext = function() {
			$scope.activeStepIdx++;
			
			if ($scope.activeStepIdx >= $scope.activeSurvey.steps.length) {
				$scope.finishSurvey();
				return;
			}
			
			$scope.showCurrent();
		};
		
		$scope.showCurrent = function() {
			$scope.activeStep = $scope.active = $scope.editor.astep = $scope.activeSurvey.steps[$scope.activeStepIdx];			
			if ($scope.active.type == "form") {
			   $scope.activeItems = $scope.active.items;
			} else {
			   $scope.activeItems = [ $scope.active ];			   
			} 			
			$scope.selectedItem = $scope.active;//Step.items[0];
			$scope.initStep($scope.activeStep);
		};
		
		$scope.goBack = function() {
			if ($scope.activeStepIdx == 0) {
				$scope.cancel();
			} else {
				$scope.activeStepIdx -= 2;
				$scope.showNext();
			}
		};
		
		$scope.cancel = function() {
			$scope.activeStep = null;
			$scope.activeSurvey = null;
		};
		
		$scope.initStep = function(step) {
			angular.forEach(step.items, function(item) {
		    	item.result = { started : new Date() };
		    });
		};
		
		$scope.extractResult = function(items) {
			console.log("extract");
			console.log(items);
		    angular.forEach(items, function(item) {
		    	$scope.surveyResult.results[item.id] = item.result;
		    });
		    console.log($scope.surveyResult);
		};
		
		$scope.startSurvey = function(survey) {
			$scope.activeSurvey = survey;
			$scope.activeStepIdx = -1;
			$scope.surveyResult = { survey : $scope.activeSurvey.id, results:{} };
			$scope.showNext();
			if ($scope.edit) {
				$scope.buildSteps();
				$scope.selectedItem = $scope.active;
			}
		};
		
		$scope.finishSurvey = function() {
			if ($scope.edit) {
				
			} else {
			  $scope.saveResults($scope.activeSurvey, $scope.surveyResult);
			  $scope.activeSurvey = null;
			}
			$scope.activeStep = null;
			console.log($scope.surveyResult);
		};
		
		
		
		$scope.loadSurveys = function() {
			midataServer.getRecords($scope.authToken, { format:"survey/questions" }, ["name", "created", "data"])
			.then(function(results) {
			   $scope.surveys = [];
			   angular.forEach(results.data, function(d) { $scope.surveys.push(d.data); });
			   $scope.loadAnswers();   			   		  
			});
		};
		
		$scope.loadAnswers = function() {
			midataServer.getRecords($scope.authToken, { format:"survey/answers", owner : "self" }, ["name", "created", "data"])
			.then(function(results2) {
			  var surveyIdx = {};
			  angular.forEach($scope.surveys, function(s) { surveyIdx[s.id] = s; });
			  angular.forEach(results2.data, function(record) {
				  if (record.data.survey != null) {
					  var survey = surveyIdx[record.data.survey];
					  if (survey != null && (!survey.answered || survey.answered.created < record.created)) {
						  survey.answered = { created : record.created };
					  }
				  } 
			  });	   
			});	
		};
		
		$scope.saveResults = function(survey, result) {
			midataServer.createRecord($scope.authToken, survey.title, "Answers to survey", "survey", "survey/answers", result);
		};
		
		$scope.isChecked = function(vals, val) {
			if (!vals.result || vals.result.length == null) vals.result = [];
			return vals.result.indexOf(val) >= 0;
		};
		
		$scope.changeCheckbox = function(vals, val) {
			if (vals.result.indexOf(val) >= 0) {
				vals.result.splice(vals.result.indexOf(val), 1);
			} else {
				vals.result.add(val);
			}
		};
		
		/*$scope.isAnswered = function(survey) {
			if (!survey.answered || !survey.answered.created) return false;
			var last = new Date(survey.answered.created);
			var now = new Date();
			switch (survey.frequency) {
			case "once" : return true;
			case "daily" : { id : "daily" , label : "Fill out every day" },
			case "weekly" : , label : "Fill out every week" },
			case "monthly" :, label : "Fill out every month" },
			case "yearly" :, label : "Fill out every year" },
			case "whenneeded" : return false; 
			}
		}*/
		
		$scope.loadSurveys();
		
		//-------------------
		
		$scope.name = function(step) {
			var spaces = step.type == "survey" ? "" : step.isItem ? "----" : "--";
			return spaces + step.id + " [" + step.type + "] :" + step.title;
		};
		
		$scope.selectStep = function(step) {
			  $scope.selectedItem = step;
			  if (!step.isItem && step.type != "survey") {
			    $scope.activeStepIdx = $scope.activeSurvey.steps.indexOf(step);
			    $scope.showCurrent();
			  }
		};
		
		$scope.insertStep = function(what, makeitem) {
		   var newstep = {				   
		      type : what,
		      id : "",
		      title : "",
		      text : ""
		              
		   };
		   $scope.reformat(newstep);
		   if ($scope.selectedItem.type == "survey") {
			   $scope.activeSurvey.steps.splice(0,0,newstep);
			   $scope.activeStepIdx = 0;
		   } else  if ($scope.selectedItem.isItem && makeitem) {
			   $scope.activeStep.items.splice($scope.activeStep.items.indexOf($scope.selectedItem)+1,0,newstep);
		   } else if (makeitem) {
			   $scope.active.items.splice(0, 0, newstep);
		   } else {
			   $scope.activeSurvey.steps.splice($scope.activeSurvey.steps.indexOf($scope.active)+1, 0, newstep);
			   $scope.activeStepIdx++;
		   }
		   $scope.buildSteps();		   		   		   
		   $scope.showCurrent();
		   $scope.editor.astep = $scope.selectedItem = newstep;
		   
		};
		
		$scope.deleteStep = function() {
			if ($scope.selectedItem.isItem) {
				$scope.activeStep.items.splice($scope.activeStep.items.indexOf($scope.selectedItem), 1);
			} else {			
				$scope.activeSurvey.steps.splice($scope.activeStepIdx,1);
				if ($scope.activeStepIdx > 0) $scope.activeStepIdx--
				$scope.showCurrent();
			}
			$scope.buildSteps();
		};
		
		
		$scope.reformat = function(itm) {
			if (itm.type == "form") {
				if (itm.items == null) {
					itm.items = [
					 
					];
				}
			} else itm.items = undefined;
			if (itm.type == 'question' || itm.type=='formitem' ) {
				itm.answer = {};
			} else itm.answer = undefined;
		};
		
		$scope.reformatAnswer = function(itm) {
			switch (itm.answer.type) {
			case "boolean": itm.answer = { type : "boolean" }; break;
			case "textChoice": itm.answer = { type : "textChoice", choices:[{ value:"C1", text:"Auswahl 1"}, { value:"C2", text:"Auswahl 2"}] }; break;
			case "multiTextChoice": itm.answer = { type : "multiTextChoice", choices:[{ value:"C1", text:"Auswahl 1"}, { value:"C2", text:"Auswahl 2"}] }; break;
			case "integerAnswerWithUnit": itm.answer = { type : "integerAnswerWithUnit", min:0, max:10 };break;
			case "textLimited": itm.answer = { type : "textLimited" };break;
			case "textUnlimited": itm.answer = { type : "textUnlimited" };break;
			}
		};
		
		$scope.saveSurvey = function() {
			$scope.activeSurvey.answered = undefined;
			midataServer.createRecord($scope.authToken, $scope.activeSurvey.title, "Survey", "templates/survey", "survey/questions", $scope.activeSurvey)
			.then(function() { $scope.cancel; });
		};
		
		$scope.createNew = function() {
			$scope.activeSurvey = {
				type : "survey",
				id : "S"+new Date().getTime(),
				title : "",
				steps : []	
			};
			$scope.buildSteps();
			$scope.editor.astep = $scope.selectedItem = $scope.activeSurvey;
			$scope.surveyResult = { survey : $scope.activeSurvey.id, results:{} };
		};
		
		$scope.buildSteps = function() {
			var s = $scope.editor.steps = [];
			s.push($scope.activeSurvey);
			angular.forEach($scope.activeSurvey.steps, function(step) {
				s.push(step);
				if (step.items != null) {
				  angular.forEach(step.items, function(itm) {
					 itm.isItem = true;
					 s.push(itm);  
				  });
				}
			});
			
		};
		
		$scope.insertChoice = function(idx) {
		   $scope.activeStep.answer.choices.splice(idx + 1,0,{ value : "", title : "", text:"" });
		};
		
		$scope.deleteChoice = function(idx) {
		   $scope.activeStep.answer.choices.splice(idx,1);
		};
		
	}
]);
