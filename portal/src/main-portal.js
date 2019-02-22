global.jQuery = global.$ = require("jquery");
global._ = require('underscore/underscore-min');
require('bootstrap/dist/js/bootstrap.bundle');
require('angular');
//require('angular-ui-bootstrap-4/dist/ui-bootstrap-3.0.0-5');
//require('angular-ui-bootstrap-4/dist/ui-bootstrap-tpls-3.0.0-5');
require('angular-ui-bootstrap/dist/ui-bootstrap');
require('angular-ui-bootstrap/dist/ui-bootstrap-tpls');
require('angular-sanitize');
require('angular-translate');
require('angular-translate-loader-partial');
require('angular-translate-storage-cookie');
require('angular-translate-storage-local');
require('angular-cookies');
require('popper.js');
require('@uirouter/angularjs/release/angular-ui-router');
require('angular-post-message');
require('angular-utils-pagination/dirPagination');

require('./app');
require('./config');
function importAll (r) {
    r.keys().forEach(r);
  }
importAll(require.context('./assets', true, /^((?!(scss\/)).)*\.js$/));
importAll(require.context('./views', true, /\.js$/));