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
	    .state('public.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',
	      termsRole : "member"
	    })
	    .state('public_provider.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',
	      termsRole : "provider"
	    })
	    .state('public_research.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',
	      termsRole : "research"
	    })
	    .state('public_developer.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',
	      termsRole : "developer"
	    })
	    .state('member.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',
	      termsRole : "member"	      
	    })
	    .state('developer.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',
	      termsRole : "developer"	      
	    })
	    .state('admin.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',
	      termsRole : "admin"	      
	    })
	    .state('research.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',
	      termsRole : "research"	      
	    })
	     .state('provider.terms', {
	      url: '/terms/:which?lang',
	      templateUrl: 'views/shared/public/terms/terms.html',
	      termsRole : "provider"	      
	    });	    
	   
}]);