'use strict';
 
module.exports = 
{
  "config": {
	"log_path": "jsonapitest-results.json",
    "defaults": {
      "api_call": {
        "request": {
          "base_url": "https://demo.midata.coop:9000",
          "headers" : {        	 
        	  "Accept" : "application/json, text/plain, */*"        	
          }
        }
      }
    },
    "modules": {
        "http_client": "./http_clients/request"
    }
    
  }
}
  