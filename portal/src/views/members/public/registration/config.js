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
	    .state('public.registration', {
	      url: '/registration?action&login&given&family&gender&country&birthdate&language&joincode',
	      templateUrl: 'views/members/public/registration/registration.html' 
	    })
	    .state('public.registration_new', {
	      url: '/registration2?action&language&email&login&client_id&redirect_uri&state&code_challenge&code_challenge_method&device_id&role&given&family&gender&country&birthdate&joincode&city&street&zip&phone&mobile',
	      templateUrl: 'views/members/public/registration/registration_new.html' 
	    })
	    .state('developer.member_registration', {
	      url: '/register_member?developer',
	      templateUrl: 'views/members/public/registration/registration.html'
	    })
	    .state('admin.member_registration', {
	      url: '/register_member?developer',
	      templateUrl: 'views/members/public/registration/registration.html'
	    });
}]);