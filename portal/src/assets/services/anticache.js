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