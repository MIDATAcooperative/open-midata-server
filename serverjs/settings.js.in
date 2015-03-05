/**
 * Server specific settings.
 */

// required modules
var fs = require("fs");

// settings
var localhost = "localhost";
var nodePort = 5000;

// ssl certificate
var sslOptions = {
		key: fs.readFileSync("NODE_SSL_SERVER_KEY"),
		cert: fs.readFileSync("NODE_SSL_SERVER_CERT")
}

// export settings
exports.localhost = localhost;
exports.sslOptions = sslOptions;
exports.nodePort = nodePort;
