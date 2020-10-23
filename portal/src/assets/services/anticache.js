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

angular.module('services')
.factory('preventTemplateCache', ['$injector', function($injector) {
    var ENV = $injector.get('ENV');

    return {
      'request': function(config) {
        if ((config.url.indexOf('.html') !== -1 &&
        		(config.url.indexOf("assets/") != -1 || config.url.indexOf("views/") != -1))	
        		|| config.url.indexOf('.json') !== -1) {          
          config.url += (config.url.indexOf("?") === -1 ? "?" : "&");
          config.url += 'rev=' + ENV.build;
        }

        return config;
      }
    }
 }])
.config(['$httpProvider', function($httpProvider) {
    $httpProvider.interceptors.push('preventTemplateCache');
}]);