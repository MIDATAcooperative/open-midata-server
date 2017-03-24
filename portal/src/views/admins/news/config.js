angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('admin.news', {
	      url: '/news',
	      templateUrl: 'views/admins/news/news.html'
	    });
}]);