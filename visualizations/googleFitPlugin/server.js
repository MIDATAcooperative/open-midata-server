// load-angular.js
var targetServer = process.argv.length > 4 ? process.argv[4] : "http://localhost:9004";
var benv = require('benv');
benv.setup(function () {
  benv.expose({
    angular: benv.require('./bower_components/angular/angular.js', 'angular'),
    http : require('http')
  });
  require('./bower_components/angular-translate/angular-translate.min.js');
  require('./bower_components/midata/js/midata.js');
    require('./src/i18n.js');
  require('./src/app.js');  
  
  window._baseurl = targetServer;
	  console.log('for');
  var injector = angular.injector(['ng', 'googleFit']);  
  var importer = injector.get('importer');
  console.log("prerun");
  importer.automatic(process.argv[2]);
}, {url: targetServer } );
