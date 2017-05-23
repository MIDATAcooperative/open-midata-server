angular.module('surveys')
.controller('EditItemCtrl', ['$scope', '$state', 'editor', 'midataServer', 'midataPortal', 
	function($scope, $state, editor, midataServer, midataPortal) {
		
	   var loc;
	   
	   $scope.types = ["group", "display", "boolean", "decimal", "integer", "date", "dateTime", "time", "string", "text" ,"url", "choice", "open-choice", "attachment", "reference", "quantity" ];
	   
	   $scope.init = function(surveyId, itemId, parentId, idx) {
		   
		   editor.selectQuestionnaire(surveyId).then(function() {
		   		   		   
			   $scope.survey = editor.getQuestionnaire();
			   
			   if (itemId) {
				   editor.getItem(surveyId, itemId).then(function(item) { $scope.item = item; });
				   loc = null;
			   } else {
				   $scope.item = editor.emptyItem();
				   loc = { parentId : parentId, idx : idx };
			   }
			   
		   });
	   };
	   
	   $scope.save = function() {
		   editor.saveItem($scope.survey.id, $scope.item, loc).then(function() {  		   
		     $state.go("^.previewsurvey", { questionnaire : $scope.survey.id });
		   });
	   };
	   
	   $scope.delete = function() {
		  editor.deleteItem($scope.survey.id, $scope.item).then(function() {
		    $state.go("^.previewsurvey", { questionnaire : $scope.survey.id });
		  });
	   };
	   
	   $scope.cancel = function() {
		   $state.go("^.previewsurvey", { questionnaire : $scope.survey.id });
	   };
	   
	   $scope.hasRepeats = function() {
		   return $scope.item.type == "choice" || $scope.item.type == "open-choice";
	   };
	   
	   $scope.hasOptions = function() {
		   return $scope.item.type == "choice" || $scope.item.type == "open-choice";
	   };
	   
	   $scope.addOption = function() {
		   console.log("XX");
		   if (!$scope.item.option) $scope.item.option = [];
		   $scope.item.option.push({ valueCoding : { display:"" , system:"", code:"" } });  
	   };
	   
	   $scope.deleteOption = function(opt) {
		   $scope.item.option.splice($scope.item.option.indexOf(opt), 1);  
	   };
	   
	   $scope.changeType = function() {
		  var t = $scope.item.type;
		  if (t === "choice" || t === "open-choice") {
			  if (!$scope.item.option) $scope.item.option = [];
		  } else {
			  $scope.item.option = undefined;
		  }
		  
	   };
	   	   
	   $scope.init($state.params.questionnaire, $state.params.item, $state.params.parent, $state.params.idx);
				
	}
]);