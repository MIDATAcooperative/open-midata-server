/**
 * Main file starting the node server.
 */

// required modules
var http = require("http");
var settings = require("./settings");
var router = require("./router");

// create the server
http.createServer(function(request, response) {
	if (request.method === "OPTIONS") {
		// allow cross-origin requests from web application and plugin servers
		var origin = request.headers.origin; // origin looks like: 'https://localhost:9000'
		var host = origin.split(":")[1].substring(2);
		var port = origin.split(":")[2];
		if (host === settings.localhost && (port === "3000" || port === "4000" || port === "9000")) {
			response.writeHead(200, {
				"Access-Control-Allow-Origin": origin,
				"Access-Control-Allow-Methods": "GET, POST",
				"Access-Control-Allow-Headers": "Content-Type"
			});
			response.end();
		}
	} else {
		router.process(request, response);
	}
}).listen(settings.nodePort);