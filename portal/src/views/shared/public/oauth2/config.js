angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public.oauth2', {
	      url: '/oauth2?action&language&email&login&client_id&redirect_uri&state&code_challenge&code_challenge_method&device_id&role&given&family&gender&country&birthdate&joincode&isnew&project',
	      templateUrl: 'views/shared/public/oauth2/oauth2.html' 
		})
		.state('public.oauthconfirm', {
			url: '/oauthconfirm?nostudies&action&language&email&login&client_id&redirect_uri&state&code_challenge&code_challenge_method&device_id&role&given&family&gender&country&birthdate&joincode&isnew&project',
			templateUrl: 'views/shared/public/oauth2/confirm.html' 
		});
}]);