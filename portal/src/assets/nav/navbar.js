angular.module('portal')
.controller('NavbarCtrl', ['$scope', '$state', '$translate', '$translatePartialLoader', 'server', 'session', 'ENV', 'spaces', function($scope, $state, $translate, $translatePartialLoader, server, session, ENV, spaces) {
	
	// init
	$scope.user = { subroles:[] };	
	$scope.beta = ENV.beta;	
	
	$translatePartialLoader.addPart($state.current.data.locales);
	
	session.viewHeight = "600px";	
	session.login($state.current.data.role);
	
	
	$scope.updateNav = function() {
		// get current user
		session.currentUser.then(function(userId) {			
			$scope.user = session.user;	
			$scope.userId = userId;
			
			spaces.getSpacesOfUserContext($scope.userId, "menu")
	    	.then(function(results) {
	    		$scope.me_menu = results.data;
	    	});
	    
		});	
	};
	
	$scope.updateNav();
			
	$scope.logout = function() {		
		server.get('/logout')
		.then(function() { session.logout(); document.location.href="/#/public/login"; });
	};
		
	$scope.changeLanguage = function(lang) {
		$translate.use(lang);
	};	
	
	$scope.hasSubRole = function(subRole) {	
		return $scope.user.subroles.indexOf(subRole) >= 0;
	};
	
	$scope.showSpace = function(space) {
		$state.go('^.spaces', { spaceId : space._id });
	};
	
	$scope.showApp = function(app) {
		spaces.openAppLink($state, $scope.userId, { app : app });
	};
	
}])
.controller('PublicNavbarCtrl', ['$scope', '$state', '$translate', '$translatePartialLoader', 'session', 'ENV', function($scope, $state, $translate, $translatePartialLoader, session, ENV) {
	$scope.notPublic = ENV.instanceType == "prod";
	
	if (!$state.current.data || !$state.current.data.keep) session.logout();
	$translatePartialLoader.addPart($state.current.data.locales);	
	$scope.changeLanguage = function(lang) {
		$translate.use(lang);
	};
}]);