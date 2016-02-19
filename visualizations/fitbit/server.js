// load-angular.js
var benv = require('benv');
benv.setup(function () { 
  benv.expose({
    angular: benv.require('js/angular.min.js', 'angular'),
    http : require('http')
  });
  require('./js/midata.js');
  require('./js/controller.js');  
  var injector = angular.injector(['ng', 'fitbit']);
  var importer = injector.get('importer');
  console.log("prerun");
  importer.automatic(process.argv[2])
  .then(function() {
	  console.log("success");
  });
});
