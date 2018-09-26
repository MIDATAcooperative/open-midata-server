angular.module('portal')
.controller('ManageNewsCtrl', ['$scope', '$state', 'server', 'news', 'status', 'languages', 'studies', 'apps', function($scope, $state, server, news, status, languages, studies, apps) {
	
	// init
	$scope.error = null;
	$scope.newsItem = {  };
	$scope.status = new status(true);
	$scope.allowDelete = $state.current.allowDelete;
	$scope.languages = languages.array;
	$scope.datePickers = {};
    $scope.dateOptions = {
       formatYear: 'yy',
       startingDay: 1
    };
    
    $scope.selection = { study : {}, app : {}, onlyStudy : {}, onlyApp : {} };
		
    $scope.languages = [];
    for (var i=0;i<languages.array.length;i++) $scope.languages.push(languages.array[i]);
    $scope.languages.push("int");
    
    $scope.layouts = ["large", "wide", "high", "small"];
    
	$scope.loadNews = function(newsId) {
		$scope.status.doBusy(news.get({ "_id" : newsId }, ["content", "created", "date", "creator", "expires", "language", "studyId", "appId", /*"onlyParticipantsStudyId", "onlyUsersOfAppId",*/ "title", "url", "layout"]))
		.then(function(data) { 
			$scope.newsItem = data.data[0];	
			
			if ($scope.newsItem.studyId) {
				  $scope.status.doBusy(studies.search({ _id : $scope.newsItem.studyId }, ["_id", "code", "name" ]))
					.then(function(data) {
						if (data.data && data.data.length == 1) {						 
						  $scope.selection.study.code = data.data[0].code;
						  $scope.selection.study.name = data.data[0].name;
						}
					});
			}
			
			if ($scope.newsItem.appId) {
				$scope.status.doBusy(apps.getApps({ _id : $scope.newsItem.appId }, ["_id", "filename", "name", "orgName", "type", "targetUserRole"]))
				.then(function(data) {
					if (data.data && data.data.length == 1) {					  
						$scope.selection.app.filename = data.data[0].filename;
						$scope.selection.app.name = data.data[0].name;
						$scope.selection.app.orgName = data.data[0].orgName;
					}
				});
			}
		});
	};
	
	// register app
	$scope.updateNews = function() {
												
		if ($scope.newsItem._id == null) {
			$scope.status.doAction('submit', news.add($scope.newsItem))
			.then(function(data) { $state.go("^.news"); });
		} else {			
		    $scope.status.doAction('submit', news.update($scope.newsItem))
		    .then(function() { $state.go("^.news"); });
		}
	};
	
	
	$scope.doDelete = function() {
		$scope.status.doAction('delete', news.delete($scope.newsItem._id))
		.then(function(data) { $state.go("^.news"); });
	};
	
	if ($state.params.newsId != null) { $scope.loadNews($state.params.newsId); }
	else { $scope.status.isBusy = false; }
	
	$scope.studyselection = function(study, field) {
		  console.log(study);
		  console.log(field);
		   $scope.status.doBusy(studies.search({ code : study.code }, ["_id", "code", "name" ]))
			.then(function(data) {
				if (data.data && data.data.length == 1) {
				  $scope.newsItem[field] = data.data[0]._id;
				  study.code = data.data[0].code;
				  study.name = data.data[0].name;
				}
			});
	};
	
	$scope.appselection = function(app, field) {
		   $scope.status.doBusy(apps.getApps({ filename : app.filename }, ["_id", "filename", "name", "orgName", "type", "targetUserRole"]))
			.then(function(data) {
				if (data.data && data.data.length == 1) {
				  $scope.newsItem[field] = data.data[0]._id;
				  app.filename = data.data[0].filename;
				  app.name = data.data[0].name;
				  app.orgName = data.data[0].orgName;
				}
			});
	};
	
	
	$scope.status.doBusy(studies.search({ validationStatus : "VALIDATED" }, ["_id", "code", "name" ]))
	.then(function(data) {
		$scope.studies = data.data;
	});
	
	$scope.status.doBusy(apps.getApps({  }, ["creator", "filename", "name", "description", "type", "targetUserRole" ]))
	.then(function(data) { 
		$scope.apps = data.data;			
	});
}]);