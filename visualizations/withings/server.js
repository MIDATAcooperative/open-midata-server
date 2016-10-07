// load-angular.js
var targetServer = process.argv.length > 4 ? process.argv[4] : "http://localhost:9001";
var benv = require('benv');
benv.setup(function () {
	
  // Load used libraries
  benv.expose({
    angular: benv.require('./bower_components/angular/angular.min.js', 'angular'),
    http : require('http')
  });
  require('./bower_components/angular-translate/angular-translate.min.js');
  require('./bower_components/midata/js/midata.js');
  require('./src/i18n.js');
  require('./src/app.js');
  
  window._baseurl = targetServer;
  
  console.log("start init of application");
  
  // Instantiate importer
  var injector = angular.injector(['ng', 'withings']);  
  var importer = injector.get('importer');
  
  // Run automatic import (passes authToken and language as parameters)
  console.log("successful initialization");
  importer.automatic(process.argv[2], process.argv[3])  
  /*.then(function() {
	  console.log("success");
  })*/;
}, {url: targetServer } );
