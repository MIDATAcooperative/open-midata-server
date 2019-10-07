angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('developer.appreviews', {
	      url: '/app/:appId/reviews',
	      templateUrl: 'views/developers/appreviews/appreviews.html',
	      allowReview : false
	    })	    
	   .state('admin.appreviews', {
		      url: '/app/:appId/appreviews?check',
		      templateUrl: 'views/developers/appreviews/appreviews.html',
		      allowReview : true
	   });
}]);