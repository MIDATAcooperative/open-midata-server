angular.module('portal')
.controller('ManageMailsCtrl', ['$scope', '$state', 'server', 'news', 'status', 'languages', 'studies', 'apps', function($scope, $state, server, news, status, languages, studies, apps) {
	
	// init
	$scope.error = null;
	$scope.mailItem = { status : "DRAFT", studyName : "", title:{}, content:{} };
	$scope.status = new status(false, $scope);
	$scope.allowDelete = $state.current.allowDelete;
	$scope.allowSend = false;
	$scope.editable = true;
	$scope.languages = languages.array;	
	$scope.sel = { lang: "int" };
      
		
    $scope.languages = [];
    for (var i=0;i<languages.array.length;i++) $scope.languages.push(languages.array[i]);
    $scope.languages.push("int");
        
    
	$scope.loadMail = function(mailId) {
		$scope.status.doBusy(server.post(jsRoutes.controllers.BulkMails.get().url, JSON.stringify({ properties : { "_id" : mailId }, fields:["creator", "creatorName", "created", "started", "finished", "name", "status", "title", "content", "studyId", "studyName", "studyCode", "studyGroup", "progressId", "progressCount"]})))
		.then(function(data) { 
			$scope.mailItem = data.data[0];	
			if ($scope.mailItem.status == "DRAFT" || $scope.mailItem.status == "PAUSED") {
				$scope.allowSend = true;	
			}
            if ($scope.mailItem.status != "DRAFT") {
            	if ($scope.mailItem.status != "FINSIHED" || $scope.mailItem.progressCount > 0) $scope.allowDelete = false;
            	$scope.editable = false;
            }
		});
	};
	
	$scope.change = function() {
		$scope.allowSend = false;
	};
	
	// register app
	$scope.updateMail = function() {
        $scope.submitted = true;
		
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		
		if (! $scope.myform.$valid) return;
		
		if ($scope.mailItem._id == null) {
			$scope.status.doAction('submit', server.post(jsRoutes.controllers.BulkMails.add().url, JSON.stringify($scope.mailItem)))
			.then(function(result) { $scope.loadMail(result.data._id); });
		} else {			
		    $scope.status.doAction('submit', server.post(jsRoutes.controllers.BulkMails.update().url, JSON.stringify($scope.mailItem)))
		    .then(function() { $scope.loadMail($scope.mailItem._id); });
		}
	};
	
	
	$scope.doDelete = function() {
		$scope.status.doAction('delete', server.post(jsRoutes.controllers.BulkMails.delete($scope.mailItem._id).url))
		.then(function(data) { $state.go("^.mails"); });
	};
	
	if ($state.params.mailId != null) { $scope.loadMail($state.params.mailId); }
	else { $scope.status.isBusy = false; }
	
	$scope.studyselection = function(study) {
		  console.log(study);		  
		   $scope.status.doBusy(studies.search({ code : study }, ["_id", "code", "name" ]))
			.then(function(data) {
				if (data.data && data.data.length == 1) {
				  $scope.mailItem.studyId = data.data[0]._id;
				  $scope.mailItem.studyCode = data.data[0].code;
				  $scope.mailItem.studyName = data.data[0].name;
				}
			});
	};
		
	
	$scope.status.doBusy(studies.search({ validationStatus : "VALIDATED" }, ["_id", "code", "name" ]))
	.then(function(data) {
		$scope.studies = data.data;
	});
	
	$scope.send = function() {
		$scope.status.doAction('send', server.post(jsRoutes.controllers.BulkMails.send($scope.mailItem._id).url))
		.then(function(data) { $state.go("^.mails"); });
	};
	
}]);