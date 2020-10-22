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