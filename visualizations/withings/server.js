// Server-side import
var benv = require('benv');
benv.setup(function () {
	
  // Load used libraries
  benv.expose({
    angular: benv.require('./bower_components/angular.min.js', 'angular'),
    http : require('http')
  });
  require('./bower_components/angular-translate/angular-translate.min.js');
  require('./bower_components/midata/js/midata.js');
  require('./src/i18n.js');
  require('./src/app.js');
  
  // Instantiate importer
  var injector = angular.injector(['ng', 'demo']);  
  var importer = injector.get('importer');
  
  // Run automatic import (passes authToken and language as parameters)
  console.log("pre-run");
  importer.automatic(process.argv[2], process.argv[3])
  .then(function() {
	  console.log("success");
  });
});
