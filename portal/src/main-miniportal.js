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

require('angular');
require('angular-sanitize');
require('angular-translate');
require('angular-translate-loader-partial');
require('angular-translate-storage-cookie');
require('angular-translate-storage-local');
require('angular-cookies');
require('@uirouter/angularjs/release/angular-ui-router');

require('./oauthapp');
require('./config');

function importAll (r) {
    r.keys().forEach(r);
  }

importAll(require.context('./assets', true, /^((?!(scss\/)).)*\.js$/));

importAll(require.context('./views/shared/public/oauth2', true, /\.js$/));
importAll(require.context('./views/shared/public/postregister', true, /\.js$/));
importAll(require.context('./views/members/public/registration', true, /\.js$/));
importAll(require.context('./views/shared/public/terms', true, /\.js$/));
importAll(require.context('./views/shared/public/lostpw', true, /\.js$/));
