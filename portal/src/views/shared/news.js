var news = angular.module('news', ['navbar', 'date', 'services', 'views', 'dashboards']);
news.controller('NewsCtrl', ['$scope', 'server', '$attrs', 'currentUser', 'users', 'views', 'status', function($scope, server, $attrs, currentUser, users, views, status) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);
	// init
	
	$scope.userId = null;	
	$scope.news = [];	
	$scope.newsItems = {};
	$scope.users = {};
	
	// get current user
	currentUser.then(function(userId) {
		$scope.userId = userId;
		$scope.reload();
		
	});
	
	// get user's news
	$scope.reload = function() {
		if (!$scope.view.active || !$scope.userId) return;
		
		$scope.status.doBusy(users.getDashboardInfo($scope.userId)).		
		then(function(result) {
			var user = result.data[0];			
			$scope.news = user.news;				
			getNewsItems($scope.news);
		});
	};
	
	// get the news items
	getNewsItems = function(newsItemIds) {
		var properties = {"_id": newsItemIds};
		var fields = ["creator", "created", "title", "content"];
		var data = {"properties": properties, "fields": fields};
		$scope.status.doBusy(server.post(jsRoutes.controllers.News.get().url, JSON.stringify(data))).
		then(function(results) {
			    var newsItems = results.data;
				_.each(newsItems, function(newsItem) { $scope.newsItems[newsItem._id.$oid] = newsItem; });
				var creatorIds = _.pluck(newsItems, "creator");
				creatorIds = _.uniq(creatorIds, false, function(id) { return id.$oid; });
				getUserNames(creatorIds);
		});
	};
	
	// get the user names
	getUserNames = function(userIds) {
		var properties = {"_id": userIds};
		var fields = ["name"];
		var data = {"properties": properties, "fields": fields};
		$scope.status.doSilent(server.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data))).
		then(function(results) {
				_.each(results.data, function(user) { $scope.users[user._id.$oid] = user; });				
		});
	};	
	
	// hide news item
	$scope.hide = function(newsItemId) {
		$scope.status.doSilent(server.post(jsRoutes.controllers.News.hide(newsItemId.$oid).url)).
		then(function() {
				$scope.news.splice($scope.news.indexOf(newsItemId), 1);
				delete $scope.newsItems[newsItemId.$oid];
		});
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);
news.controller('TeaserCtrl', ['$scope', 'server', '$attrs', 'dateService', 'currentUser', 'users', 'views', 'status', function($scope, server, $attrs, dateService, currentUser, users, views, status) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);
	
	// init
	$scope.userId = null;
	$scope.lastLogin = null;
	$scope.pushed = [];
	$scope.shared = [];
	
	// get current user
	currentUser.then(function(userId) {
		$scope.userId = userId;
		$scope.reload();	
	});
		
	$scope.reload = function() {
		if (!$scope.view.active || !$scope.userId) return;
		
		$scope.status.doBusy(users.getDashboardInfo($scope.userId)).		
		then(function(result) {
			var user = result.data[0];
				$scope.lastLogin = user.login;			
				$scope.pushed = user.pushed;
				$scope.shared = user.shared;
				$scope.apps = user.apps;
				$scope.visualizations = user.visualizations;
			switch ($scope.view.setup.type) {
			case "noapps" : $scope.view.active = $scope.apps.length === 0;break;
			case "novisualizations" : $scope.view.active = $scope.visualizations.length === 0;break;
			case "news" : $scope.view.active = ($scope.pushed.length > 0 || $scope.shared.length > 0);break;
			}
		});
	};
	
	// show new pushed records
	$scope.showPushed = function() {
		$scope.clearAll();
		var ownerFilter = "owner/is/" + $scope.userId.$oid;
		var createdFilter = "created/" + $scope.lastLogin + "/" + dateService.toString(dateService.now());
		window.location.href = jsRoutes.controllers.Records.filter(ownerFilter + "/" + createdFilter).url;
	};
	
	// show new shared records
	$scope.showShared = function() {
		$scope.clearAll();
		var ownerFilter = "owner/isnt/" + $scope.userId.$oid;
		var createdFilter = "created/" + $scope.lastLogin + "/" + dateService.toString(dateService.now());
		window.location.href = jsRoutes.controllers.Records.filter(ownerFilter + "/" + createdFilter).url;
	};
	
	// show new records
	$scope.showAll = function() {
		$scope.clearAll();
		var createdFilter = "created/" + $scope.lastLogin + "/" + dateService.toString(dateService.now());
		window.location.href = jsRoutes.controllers.Records.filter(createdFilter).url;
	};
	
	// mark all records as seen
	$scope.clearAll = function() {
		$scope.status.doSilent(server.post(jsRoutes.controllers.Users.clearPushed().url)).
		then(function() { $scope.pushed = []; });
		$scope.status.doSIlent(server.post(jsRoutes.controllers.Users.clearShared().url)).
		then(function() { $scope.shared = []; });			
	};
		
	$scope.$watch('view.setup', function() { $scope.reload(); });
}]);