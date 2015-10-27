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
                    "params" : { "email" : "test1@example.com", "password" : "secret" }
                },
                "status": 200,
                "save" : {
                	"session" : "headers.set-cookie"
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
                    	   "Cookie" : "{{session}}"
                      }
                  },
                  "status": 200,
                  "save" : {
                	  "member.id" : "body.$oid"
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
                  	   "Cookie" : "{{session}}"
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
                    	   "Cookie" : "{{session}}"
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
                   	   "Cookie" : "{{session}}"
                     },
                    "params" : {
                    	"properties": { "owner":{"$oid":"{{member.id}}"}, "context":"me"},
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