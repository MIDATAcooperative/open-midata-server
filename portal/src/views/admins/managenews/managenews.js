angular.module('portal')
.controller('ManageNewsCtrl', ['$scope', '$state', 'server', 'news', 'status', function($scope, $state, server, news, status) {
	
	// init
	$scope.error = null;
	$scope.newsItem = {  };
	$scope.status = new status(true);
	$scope.allowDelete = $state.current.allowDelete;
	$scope.languages = ['en', 'de', 'fr', 'it'];
	$scope.datePickers = {};
    $scope.dateOptions = {
       formatYear: 'yy',
       startingDay: 1
    };
			
	$scope.loadNews = function(newsId) {
		$scope.status.doBusy(news.get({ "_id" : newsId }, ["content", "created", "creator", "expires", "language", "studyId", "title", "url"]))
		.then(function(data) { 
			$scope.newsItem = data.data[0];			
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
}]);