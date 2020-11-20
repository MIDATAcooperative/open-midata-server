/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('portal')
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('public.oauth2', {
	      url: '/oauth2?action&language&email&login&client_id&redirect_uri&state&code_challenge&code_challenge_method&device_id&role&given&family&gender&country&birthdate&joincode&isnew&project&city&street&zip&phone&mobile',
	      templateUrl: 'views/shared/public/oauth2/oauth2.html' 
		})
		.state('public.oauthconfirm', {
			url: '/oauthconfirm?nostudies&action&language&email&login&client_id&redirect_uri&state&code_challenge&code_challenge_method&device_id&role&given&family&gender&country&birthdate&joincode&isnew&project&city&street&zip&phone&mobile',
			templateUrl: 'views/shared/public/oauth2/confirm.html' 
		});
}]);