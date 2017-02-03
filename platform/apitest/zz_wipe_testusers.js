'use strict';
 
module.exports = 
{
  "suites": [
    {
        "name": "Wipe Testusers",
        "tests": [
          {
            "name": "Wipe Account Holder",
            "description": "Wipes account holder",
            "api_calls": [
                {
                  "request": {
                      "method": "POST",
                      "path": "/members/api/login",
                      "params" : { "email" : "{{member.email}}", "password" : "{{member.password}}" }
                  },
                  "status": 200,
                  "assert": {
                  	"schema": {
                          "type": "object",
                          "properties": {
                            "sessionToken": {"type": "string"},
                            "role": {"type": "string"},
                            "lastLogin": {"type": "integer"},
                            "keyType" : {"type": "integer"},
                            "subroles" : { "type" : "array" }
                          },
                          "required": ["sessionToken", "lastLogin", "keyType", "subroles"],
                          "additionalProperties": false
                      }
                  },
                  "save" : {
                  	"session" : "body.sessionToken"
                  }
                                  
                },
                {
                    "request": {
                        "method": "DELETE",
                        "path": "/shared/api/users/wipe",
                        "headers" : {        	                   	 
                       	   "X-Session-Token" : "{{session}}"
                        }
                    },
                    "status": 200
                   
                                    
                }
                
              ]
          },
          {
              "name": "Wipe Researcher",
              "description": "Wipes researcher",
              "api_calls": [
                  {
                    "request": {
                        "method": "POST",
                        "path": "/research/api/login",
                        "params" : { "email" : "{{researcher.email}}", "password" : "{{researcher.password}}" }
                    },
                    "status": 200,
                    "assert": {
                    	"schema": {
                            "type": "object",
                            "properties": {
                              "sessionToken": {"type": "string"},
                              "role": {"type": "string"},
                              "lastLogin": {"type": "integer"},
                              "keyType" : {"type": "integer"},
                              "subroles" : { "type" : "array" }
                            },
                            "required": ["sessionToken", "lastLogin", "keyType", "subroles"],
                            "additionalProperties": false
                        }
                    },
                    "save" : {
                    	"session" : "body.sessionToken"
                    }
                                    
                  },
                  {
                      "request": {
                          "method": "DELETE",
                          "path": "/shared/api/users/wipe",
                          "headers" : {        	                   	 
                         	   "X-Session-Token" : "{{session}}"
                          }
                      },
                      "status": 200
                     
                                      
                  }
                  
                ]
            },
            {
                "name": "Wipe Provider",
                "description": "Wipes provider",
                "api_calls": [
                    {
                      "request": {
                          "method": "POST",
                          "path": "/providers/api/login",
                          "params" : { "email" : "{{provider.email}}", "password" : "{{provider.password}}" }
                      },
                      "status": 200,
                      "assert": {
                      	"schema": {
                              "type": "object",
                              "properties": {
                                "sessionToken": {"type": "string"},
                                "role": {"type": "string"},
                                "lastLogin": {"type": "integer"},
                                "keyType" : {"type": "integer"},
                                "subroles" : { "type" : "array" }
                              },
                              "required": ["sessionToken", "lastLogin", "keyType", "subroles"],
                              "additionalProperties": false
                          }
                      },
                      "save" : {
                      	"session" : "body.sessionToken"
                      }
                                      
                    },
                    {
                        "request": {
                            "method": "DELETE",
                            "path": "/shared/api/users/wipe",
                            "headers" : {        	                   	 
                           	   "X-Session-Token" : "{{session}}"
                            }
                        },
                        "status": 200
                       
                                        
                    }
                    
                  ]
              }
          
      ] // End tests
    }
  ]
}