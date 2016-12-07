'use strict';
 
module.exports = 
{
  "suites": [
    {
        "name": "Member browse pages",
        "tests": [
          {
            "name": "login",
            "description": "login user",
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
              }
            ]
          },
          {
              "name": "get user id",
              "description": "try to get current user id",
              "api_calls": [
                {
                  "request": {
                  	"method" : "GET",
                  	"path" : "/shared/api/users/current",
                  	"headers" : {        	                   	 
                    	   "X-Session-Token" : "{{session}}"
                      }
                  },
                  "status": 200,
                  "assert": {
                  	"schema": {
                          "type": "object",
                          "properties": {
                            "role": {"type": "string"},
                            "user": {"type": "string"}                            
                          },
                          "required": ["role", "user" ],
                          "additionalProperties": false
                      }
                  },
                  "save" : {
                	  "member.id" : "body.user"
                  }
                }
              ]
          },                              
          {
            "name": "get config",
            "description": "try to get config",
            "api_calls": [
              {
                "request": {
                	"method" : "GET",
                	"path" : "/portal/config",
                	"headers" : {        	                   	 
                  	   "X-Session-Token" : "{{session}}"
                    }
                },
                "status": 200
              }
            ]
          },
          {
              "name": "list tasks",
              "description": "list tasks of user",
              "api_calls": [
                {
                  "request": {
                  	"method" : "POST",
                  	"path" : "/shared/api/tasks/list",
                  	"headers" : {        	                   	 
                    	   "X-Session-Token" : "{{session}}"
                      }
                  },
                  "status": 200
                }
              ]
         },
         {
             "name": "list spaces",
             "description": "list spaces of user",
             "api_calls": [
               {
                 "request": {
                 	"method" : "POST",
                 	"path" : "/members/api/spaces/get",
                 	"headers" : {        	                   	 
                   	   "X-Session-Token" : "{{session}}"
                     },
                    "params" : {
                    	"properties": { "owner":"{{member.id}}", "context":"me"},
                    	"fields": ["name","records","visualization","app","order"]
                    }
                 },
                 "status": 200
               }
             ]
        }          
          
       ]
    }
  ]
}