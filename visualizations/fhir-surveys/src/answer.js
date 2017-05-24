angular.module('surveys')
.controller('AnswerCtrl', ['$scope', '$state', 'answer', 'midataServer', 'midataPortal', 
	function($scope, $state, answer, midataServer, midataPortal) {
		
		$scope.init = function(qid, rid, page) {
			if (page === undefined) {
			   answer.startQuestionnaire(qid, rid).then(function() {
				  $scope.page = 0;
				  $scope.item = answer.getPage($scope.page);
				  console.log("LOADED");
				  console.log($scope.item);
			   });	
			} else {
			   $scope.page = Number(page);
			   $scope.item = answer.getPage(page);
			   console.log($scope.item);
			}	
			
		};
		
		$scope.getHeadTemplate = function(item) {
			return "display_head.html";
		};
		
		$scope.getBodyTemplate = function(item) {
			if (!item) return null;
			return item.type+"_body.html";
		};
		
		$scope.prev = function() {
			var pn = answer.prev($scope.page);
			$state.go('^.answer', { questionnaire : $state.params.questionnaire, response : $state.params.response, page : pn} );
		};
		
		$scope.next = function() {
			var pn = answer.next($scope.page);
			if (pn >= 0) {
				$state.go('^.answer', { questionnaire : $state.params.questionnaire, response : $state.params.response, page : pn} );
			} else {
				$scope.save();
			}
		};
		
		$scope.cancel = function() {
			answer.end();
			$state.go('^.pick');
		};
		
		$scope.save = function() {
			answer.save().then(function() {
			  answer.end();
			  $state.go('^.pick');
			});
		};
		
		$scope.isChecked = function(item, choice) {
			return item._answer.indexOf(choice) >= 0;
		};
		
		$scope.changeChecked = function(item, choice) {
			var i = item._answer.indexOf(choice);
			if (i >= 0) {
				item._answer.splice(i,1);
			} else item._answer.push(choice);			
		};
			
		$scope.init($state.params.questionnaire, $state.params.response, $state.params.page);
	}
]);