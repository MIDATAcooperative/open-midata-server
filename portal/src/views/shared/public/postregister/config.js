/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public.postregister', {
	      url: '/postregister?callback&consent&action&city&street&zip&phone&mobile',
	      templateUrl: 'views/shared/public/postregister/postregister.html',
	      data : { role : "member", keep : true },
	      params : { progress : null }
	    })
	    .state('public_provider.postregister', {
	      url: '/postregister?callback&consent&action',
	      templateUrl: 'views/shared/public/postregister/postregister.html',
	      data : { role : "hpuser", keep : true },
	      params : { progress : null }
	    })
	    .state('public_research.postregister', {
	      url: '/postregister?callback&consent&action',
	      templateUrl: 'views/shared/public/postregister/postregister.html',
		  data : { role : "research", keep : true },
	      params : { progress : null }
	    })
	    .state('public_developer.postregister', {
	      url: '/postregister?callback&consent&action',
	      templateUrl: 'views/shared/public/postregister/postregister.html',
		  data : { role : "developer", keep : true },
	      params : { progress : null }
	    })
	    .state('public.confirm', {
	      url: '/confirm/:token',
	      templateUrl: 'views/shared/public/postregister/postregister.html',
	      data : { mode : "VALIDATED" }
	    })
	   .state('public.reject', {
		      url: '/reject/:token',
		      templateUrl: 'views/shared/public/postregister/postregister.html',
		      data : { mode : "REJECTED" }
		})
	   .state('member.upgrade', {
	      url: '/upgrade?role&feature&pluginId',
	      templateUrl: 'views/shared/public/postregister/postregister.html' 
	   })
	   .state('research.upgrade', {
	      url: '/upgrade?role&feature&pluginId',
	      templateUrl: 'views/shared/public/postregister/postregister.html' 
	   })
	   .state('developer.upgrade', {
	      url: '/upgrade?role&feature&pluginId',
	      templateUrl: 'views/shared/public/postregister/postregister.html' 
	   }).state('provider.upgrade', {
		      url: '/upgrade?role&feature&pluginId',
		      templateUrl: 'views/shared/public/postregister/postregister.html' 
	   }).state('public.failure', {
	      url: '/failure?reason&action&language&email&login&client_id&redirect_uri&state&code_challenge&code_challenge_method&device_id&role&given&family&gender&country&birthdate&joincode&isnew&city&street&zip&phone&mobile',
	      templateUrl: 'views/shared/public/postregister/failure.html',
	      data : { role : "member", keep : true }
	    })
	    .state('public_provider.failure', {
	      url: '/failure?reason&action&language&email&login&client_id&redirect_uri&state&code_challenge&code_challenge_method&device_id&role&given&family&gender&country&birthdate&joincode&isnew&city&street&zip&phone&mobile',
	      templateUrl: 'views/shared/public/postregister/failure.html',
	      data : { role : "hpuser", keep : true }
	    })
	    .state('public_research.failure', {
	      url: '/failure?reason&action&language&email&login&client_id&redirect_uri&state&code_challenge&code_challenge_method&device_id&role&given&family&gender&country&birthdate&joincode&isnew&city&street&zip&phone&mobile',
	      templateUrl: 'views/shared/public/postregister/failure.html',
		  data : { role : "research", keep : true }
	    })
	    .state('public_developer.failure', {
	      url: '/failure?reason&action&language&email&login&client_id&redirect_uri&state&code_challenge&code_challenge_method&device_id&role&given&family&gender&country&birthdate&joincode&isnew&city&street&zip&phone&mobile',
	      templateUrl: 'views/shared/public/postregister/failure.html',
		  data : { role : "developer", keep : true }
	    })
	   
	   ;
	   
}]);