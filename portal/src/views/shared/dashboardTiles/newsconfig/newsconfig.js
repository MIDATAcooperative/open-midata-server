angular.module('views')
.controller('NewsConfigCtrl', ['$scope', '$attrs', '$sce', '$translate', 'views', 'status', 'session', 'news', function($scope, $attrs, $sce, $translate, views, status, session, news) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    $scope.done = false;
    
    session.currentUser.then(function(userId) { 
    	$scope.userId = userId;
    	$scope.reload(); 
    });
    
                      
    $scope.addNews = function(news) {
    	 var spacedef =
	     {
	    	   id : "news"+news._id,
	    	   template : "/views/shared/dashboardTiles/newsconfig/newsconfig.html",
	    	   title : news.title,
	    	   active : true,
	    	   position : $scope.view.position,
	    	   actions : {  },
	    	   setup : { text : news.content }
	     };
	     views.layout[$scope.view.position].splice(0,0,views.def(spacedef)); 
    };
    
    $scope.reload = function() {
    	if (!$scope.view.active || !$scope.userId || $scope.done || !$scope.view.setup) return;	
    	$scope.done = true;
    	if ($scope.view.setup.text) { $scope.status.isBusy = false; return; }
    	
    	$scope.status.doBusy(news.get({ "language" : $translate.use() }, ["content", "created", "title", "studyId", "url"]))
    	.then(function(results) {
    	   if (results.data.length===0) {
    		   $scope.view.active = false;
    		   return; 
    	   }
    	   for (var i=0;i<results.data.length-1;i++) {
    		   $scope.addNews(results.data[i]);
    	   }
    	   var last = results.data[results.data.length-1];
    	   $scope.view.setup = { text : last.content, title : last.title, url : last.url };
    	   
    	});
    	    	
    };
    
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);