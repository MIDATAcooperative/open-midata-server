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
