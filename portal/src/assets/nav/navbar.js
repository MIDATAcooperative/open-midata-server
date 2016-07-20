angular.module('portal')
.controller('NavbarCtrl', ['$scope', '$state', '$translate', '$translatePartialLoader', 'server', 'session', 'ENV', function($scope, $state, $translate, $translatePartialLoader, server, session, ENV) {
	
	// init
	$scope.user = { subroles:[] };	
	$scope.beta = ENV.beta;
	
	$translatePartialLoader.addPart($state.current.data.locales);
	
	session.viewHeight = "600px";	
	session.login($state.current.data.role);
	
	// get current user
	session.currentUser.then(function(userId) {
		console.log("DONE NAV");
		$scope.user = session.user;		
	});
			
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
	
}])
.controller('PublicNavbarCtrl', ['$scope', '$state', '$translate', '$translatePartialLoader', 'session', function($scope, $state, $translate, $translatePartialLoader, session) {	
	session.logout();
	$translatePartialLoader.addPart($state.current.data.locales);	
	$scope.changeLanguage = function(lang) {
		$translate.use(lang);
	};
}]);