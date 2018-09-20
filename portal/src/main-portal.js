global.jQuery = global.$ = require("jquery");
require('underscore/underscore-min');
require('bootstrap/dist/js/bootstrap');

require('angular');
require('angular-ui-bootstrap/dist/ui-bootstrap');
require('angular-sanitize');
require('angular-translate');
require('angular-translate-loader-partial');
require('angular-translate-storage-cookie');
require('angular-translate-storage-local');
require('angular-cookies');
require('angular-ui-bootstrap/dist/ui-bootstrap-tpls');
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