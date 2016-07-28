// load-angular.js
var benv = require('benv');
benv.setup(function () { 
  benv.expose({
    angular: benv.require('./angular.min.js', 'angular'),
    http : require('http')
  });
  require('./bower_components/angular-translate/angular-translate.min.js');
  require('./bower_components/midata/js/midata.js');
  require('./src/i18n.js');
  require('./src/app.js');  
  var injector = angular.injector(['ng', 'fitbit']);  
  var importer = injector.get('importer');
  console.log("prerun");
  importer.automatic(process.argv[2], process.argv[3])
  .then(function() {
	  console.log("success");
  });
});
