var glob = require('glob');
var path = require('path');
require('angular');
//require('./app')
//require('./config');

//autoload('./assets/**/*.js');
//autoload('./views/**/*.js');

//glob.sync( './src/assets/**/*.js' ).forEach( function( file ) {
//    console.log('added: ' + path.resolve( file ));
//    require( path.resolve( file ) );
//  });
//glob.sync( './views/**/*.js' ).forEach( function( file ) {
//    console.log('added: ' + path.resolve( file ));
//    require( path.resolve( file ) );
//  });

require.context('./assets', true, /\.js$/);
require.context('./views', true, /\.js$/);