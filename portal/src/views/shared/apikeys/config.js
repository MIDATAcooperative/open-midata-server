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
	    .state('member.servicekeys', {
	      url: '/servicekeys',
	      templateUrl: 'views/shared/apikeys/apikeys.html'
        })
        .state('provider.servicekeys', {
            url: '/servicekeys',
            templateUrl: 'views/shared/apikeys/apikeys.html'
        })
        .state('research.servicekeys', {
            url: '/servicekeys?studyId',
            templateUrl: 'views/shared/apikeys/apikeys.html'
        })
        .state('developer.servicekeys', {
            url: '/servicekeys',
            templateUrl: 'views/shared/apikeys/apikeys.html'
        })
        .state('admin.servicekeys', {
            url: '/servicekeys',
            templateUrl: 'views/shared/apikeys/apikeys.html'
        });
	    
}]);