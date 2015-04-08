var news = angular.module('news', ['navbar', 'date', 'services', 'views']);
news.controller('NewsCtrl', ['$scope', '$http', 'dateService', 'currentUser', 'users', 'views', function($scope, $http, dateService, currentUser, users, views) {
	
	// init
	$scope.error = null;
	$scope.loading = true;
	$scope.userId = null;
	$scope.lastLogin = null;
	$scope.news = [];
	$scope.pushed = [];
	$scope.shared = [];
	$scope.newsItems = {};
	$scope.users = {};
	
	// get current user
	currentUser.then(function(userId) {
		$scope.userId = userId;
		getNews(userId);
		views.setView("1", { aps : userId.$oid, properties : { } , fields : [ "ownerName", "created", "id", "name" ]});
		views.setView("2", { properties : { }, fields : ["name"] });
	});
	
	// get user's news
	getNews = function(userId) {
		users.getMembers({"_id": userId}, ["login", "news", "pushed", "shared", "apps", "visualizations"]).		
		then(function(result) {
			var user = result.data[0];
				$scope.lastLogin = user.login.split(" ")[0];
				$scope.news = user.news;
				$scope.pushed = user.pushed;
				$scope.shared = user.shared;
				$scope.showAppTeaser = user.apps.length == 0;
				$scope.showVisualizationsTeaser = user.visualizations.length == 0;
				getNewsItems($scope.news);
		},function(err) {
				$scope.error = "Failed to load your news: " + err;
				$scope.loading = false;
		});
	}
	
	// get the news items
	getNewsItems = function(newsItemIds) {
		var properties = {"_id": newsItemIds};
		var fields = ["creator", "created", "title", "content"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.News.get().url, JSON.stringify(data)).
			success(function(newsItems) {
				_.each(newsItems, function(newsItem) { $scope.newsItems[newsItem._id.$oid] = newsItem; });
				var creatorIds = _.pluck(newsItems, "creator");
				creatorIds = _.uniq(creatorIds, false, function(id) { return id.$oid; });
				getUserNames(creatorIds);
			}).
			error(function(err) {
				$scope.error = "Failed to load news items: " + err;
				$scope.loading = false;
			});
	}
	
	// get the user names
	getUserNames = function(userIds) {
		var properties = {"_id": userIds};
		var fields = ["name"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) {
				_.each(users, function(user) { $scope.users[user._id.$oid] = user; });
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.error = "Failed to load user names: " + err;
				$scope.loading = false;
			});
	}
	
	// show new pushed records
	$scope.showPushed = function() {
		$scope.clearAll();
		var ownerFilter = "owner/is/" + $scope.userId.$oid;
		var createdFilter = "created/" + $scope.lastLogin + "/" + dateService.toString(dateService.now());
		window.location.href = jsRoutes.controllers.Records.filter(ownerFilter + "/" + createdFilter).url;
	}
	
	// show new shared records
	$scope.showShared = function() {
		$scope.clearAll();
		var ownerFilter = "owner/isnt/" + $scope.userId.$oid;
		var createdFilter = "created/" + $scope.lastLogin + "/" + dateService.toString(dateService.now());
		window.location.href = jsRoutes.controllers.Records.filter(ownerFilter + "/" + createdFilter).url;
	}
	
	// show new records
	$scope.showAll = function() {
		$scope.clearAll();
		var createdFilter = "created/" + $scope.lastLogin + "/" + dateService.toString(dateService.now());
		window.location.href = jsRoutes.controllers.Records.filter(createdFilter).url;
	}
	
	// mark all records as seen
	$scope.clearAll = function() {
		$http(jsRoutes.controllers.Users.clearPushed()).
			success(function() { $scope.pushed = []; }).
			error(function(err) { $scope.error = "Failed to mark pushed records as seen: " + err; });
		$http(jsRoutes.controllers.Users.clearShared()).
			success(function() { $scope.shared = []; }).
			error(function(err) { $scope.error = "Failed to mark shared records as seen: " + err; });
	}
	
	// 
	
	// hide news item
	$scope.hide = function(newsItemId) {
		$http(jsRoutes.controllers.News.hide(newsItemId.$oid)).
			success(function() {
				$scope.news.splice($scope.news.indexOf(newsItemId), 1);
				delete $scope.newsItems[newsItemId.$oid];
			}).
			error(function(err) { $scope.error = "Failed to hide this news item: " + err; });
	}
	
}]);
