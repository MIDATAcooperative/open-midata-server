angular.module('portal')
.controller('NavbarCtrl', ['$scope', '$state', '$translate', '$translatePartialLoader', 'server', 'session', 'ENV', 'spaces', 'circles', function($scope, $state, $translate, $translatePartialLoader, server, session, ENV, spaces, circles) {
	
	// init
	$scope.user = { subroles:[] };	
	$scope.beta = ENV.beta;	
	
	$translatePartialLoader.addPart($state.current.data.locales);
	
	session.viewHeight = "600px";	
	session.login($state.current.data.role);
	
	
	$scope.updateNav = function() {
		$scope.circles = circles;
		circles.unconfirmed = 0;
		// get current user
		session.currentUser.then(function(userId) {			
			$scope.user = session.user;	
			$scope.userId = userId;
			
			spaces.getSpacesOfUserContext($scope.userId, "menu")
	    	.then(function(results) {
	    		$scope.me_menu = results.data;
	    	});
			
			circles.listConsents({  }, ["type", "status"])
			.then(function(results) {
				var l = results.data.length;
				circles.apps = 0;
				circles.studies = 0;
				circles.unconfirmed = 0;
				for (var i=0;i<l;i++) {
					var c = results.data[i];
					if (c.type == "EXTERNALSERVICE") circles.apps++;
					else if (c.type == "STUDYPARTICIPATION") circles.studies++;
					else if (c.status == "UNCONFIRMED") circles.unconfirmed++;
				}
				
				//circles.unconfirmed = results.data.length;
			});
	    
		});	
	};
	
	$scope.updateNav();
			
	$scope.logout = function() {		
		server.get('/api/logout')
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