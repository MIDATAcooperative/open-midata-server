angular.module('surveys')
.controller('PreviewSurveyCtrl', ['$scope', '$state', 'editor', 'midataServer', 'midataPortal', 
	function($scope, $state, editor, midataServer, midataPortal) {
		
	$scope.init = function(id) {
		console.log(id);		
		editor.selectQuestionnaire(id).then(function() {
		
			$scope.survey = editor.getQuestionnaire();
			$scope.items = editor.getItems();
		});
	};
	
	$scope.getHeadTemplate = function(item) {
		return "display_head.html";
	};
	
	$scope.getBodyTemplate = function(item) {
		return item.type+"_body.html";
	};
	
	$scope.save = function() {
		editor.saveQuestionnaire($scope.survey).then(function() {
		  editor.end();
		  $state.go('^.pick');
		});
	};
	
	$scope.editHeader = function() {
		$state.go('^.editsurvey', { questionnaire : $scope.survey.id });
	};
	
	$scope.edit = function(item) {
		$state.go('^.edititem', { questionnaire : $scope.survey.id, item : item._id });
	};
	
	$scope.delete = function(item) {
	    editor.deleteItem($scope.survey.id, item);
	};
	
	$scope.cancel = function() {
		$state.go('^.pick');
	};
	
	$scope.add = function(parent, idx) {				
		$state.go('^.edititem', { questionnaire : $scope.survey.id, parent : (parent ? parent._id : undefined), idx : idx });		
	};	
	
	$scope.init($state.params.questionnaire);
				
}]);