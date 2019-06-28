var midata = require('./src/midata-research');
var assert = require('assert');
var fs = require('fs'); 

const server = "https://ch.midata.coop";

var stream = fs.createWriteStream("export.json");
let counter = 0;

stream.once('open', function(fd) {
	stream.write('{ "resourceType" : "Bundle", "type" : "searchset", "entry" : [');
	
	midata.app("ally_export", "secret", "tool")
	.useServer(server)
	.loginResearcher("olivier.descloux@midata.coop", "my_super_secret_password")
	
	.forEachMatch("Observation", { code : "http://midata.coop|pollen-forecast", _count : 1000 }, function(observation) {
	   var bundleEntry = { "fullUrl" : server+"/fhir/Observation/"+observation.id , "resource" : observation };
	   if (counter > 0) stream.write(",");
	   stream.write(JSON.stringify(bundleEntry));
	   counter++;
	})
	.then(function() {
		stream.write("] }");
		stream.end();
		console.log("Total export: "+counter);
	});
});