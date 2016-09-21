angular.module('portal')
.controller('AdminNewsCtrl', ['$scope', '$state', 'views', 'status', 'news', function($scope, $state, views, status, news) {

	$scope.status = new status(true);
		
	$scope.init = function(userId) {	
		$scope.status.doBusy(news.get({ }, ["content", "created", "title", "studyId", "url", "expires", "language"]))
    	.then(function(results) {
		  $scope.news = results.data;
    	});
	};
	
	$scope.deleteNews = function(newsItem) {
		$scope.status.doAction('delete', news.delete(newsItem._id))
		.then(function(data) { $scope.init(); });
	};
			
	$scope.init();
	
}]);