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
	    .state('public.service', {
	      url: '/service?app&consent&project&login&callback&isnew&email&given&family&gender&country&birthdate&language',
	      templateUrl: 'views/shared/public/service/service.html' 
	    })
	    .state('goplugin', {
	      url: '/apps/:pluginName?login&isnew&email&given&family&gender&country&birthdate&language',
	      templateUrl: 'views/shared/public/service/service.html' 
	    })
	    .state('member.service2', {
	      url: '/service2?app&consent&login&callback&action',
	      templateUrl: 'views/shared/public/service/empty.html' 
	    })
	    .state('member.serviceleave', {
	      url: '/serviceleave?app&callback',
	      templateUrl: 'views/shared/public/service/serviceleave.html' 
	    })
	    .state('provider.serviceleave', {
	      url: '/serviceleave?app&callback',
	      templateUrl: 'views/shared/public/service/serviceleave.html' 
	    })
	    .state('research.serviceleave', {
	      url: '/serviceleave?app&callback',
	      templateUrl: 'views/shared/public/service/serviceleave.html' 
	    });
}]);