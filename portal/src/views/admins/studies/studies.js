angular.module('portal')
.controller('AdminListStudiesCtrl', ['$scope', 'server', 'status', function($scope, server, status) {
	
	$scope.results =[];
	$scope.status = new status(true);
	
	$scope.searches = [
		{
			id : "enum.studyvalidationstatus.VALIDATION",
			properties : {
			   "validationStatus" : "VALIDATION"
			}
		},
		{
			id :"enum.participantsearchstatus.SEARCHING",
			properties : {
				"participantSearch" : "SEARCHING"
			}
		},
		{
			id : "enum.studyexecutionstatus.RUNNING",
			properties : {
				"executionStatus" : "RUNNING"
			}
		},
		{
			id : "enum.studyvalidationstatus.DRAFT",
			properties : {
				"validationStatus" : "DRAFT"
			}
		},
		{
			id : "enum.studyvalidationstatus.REJECTED",
			properties : {
				"validationStatus" : "REJECTED"
			}
		},
		{
			id : "enum.studyvalidationstatus.VALIDATED",
			properties : {
				"validationStatus" : "VALIDATED"
			}
		}
			
	];
	
	
	$scope.selection = { criteria : $scope.searches[0] };
	
	$scope.doreload = function() {
			
		$scope.status.doBusy(server.post(jsRoutes.controllers.research.Studies.listAdmin().url, JSON.stringify($scope.selection.criteria)))
		.then(function(data) { 				
				$scope.results = data.data;	
		});
	};
	
	$scope.doreload();
	
}]);