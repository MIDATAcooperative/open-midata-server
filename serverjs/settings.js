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
		key: fs.readFileSync("/home/alexander/projects/hdc/ssl-certificate/server.pem"),
		cert: fs.readFileSync("/home/alexander/projects/hdc/ssl-certificate/server.pem")
}

// export settings
exports.localhost = localhost;
exports.sslOptions = sslOptions;
exports.nodePort = nodePort;
