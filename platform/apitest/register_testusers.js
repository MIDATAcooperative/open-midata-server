'use strict';
 
module.exports = 
{
		"suites": [
			{
				"name": "Register Testusers",
				"tests": [
					{
						"name": "Login as Developer",
						"description": "Login Developer Account",
						"api_calls": [
							{
								"request": {
									"method": "POST",
									"path": "/developers/api/login",
									"params" : { "email" : "{{developer.email}}", "password" : "{{developer.password}}" }
								},
								"status": 200,						
								"save" : {
									"developersession" : "body.sessionToken"
								}

							},
							{
								"request": {
									"method": "GET",
									"path": "/shared/api/users/current",
									"headers" : {        	                   	 
										"X-Session-Token" : "{{developersession}}"
									}
								},
								"status": 200,						
								"save" : {
									"developer.id" : "body.user"
								}

							}
							]
					},
					{     

						"name": "Register Account Holder",
						"description": "Register new account holder",
						"api_calls": [
							{
								"request": {
									"method": "POST",
									"path": "/members/api/registration",
									"params" : 
									{
										"language":"en",
										"email":"{{member.email}}",
										"password":"{{member.password}}",
										"password2":"{{member.password}}",
										"firstname":"Test",
										"lastname":"User",
										"gender":"MALE",
										"birthdayDay":"01",
										"birthdayMonth":"01",
										"birthdayYear":"2000",
										"address1":"Teststrasse 1",
										"city":"Teststadt",
										"zip":"12345",
										"country":"CH",
										"agb":true,
										"birthday":"2000-01-01",
										"developer" : "{{developer.id}}"
									},
									"headers" : {        	                   	 
										"X-Session-Token" : "{{developersession}}"
									}
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
											"subroles" : { "type" : "array" },
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

						"name": "Double Register Check",
						"description": "Double Register Check",
						"api_calls": [
							
							
							
							
							{
								"request": {
									"method": "POST",
									"path": "/members/api/registration",
									"params" : 
									{
										"language":"en",
										"email":"{{member.email}}",
										"password":"{{member.password}}",
										"password2":"{{member.password}}",
										"firstname":"Test",
										"lastname":"User",
										"gender":"MALE",
										"birthdayDay":"01",
										"birthdayMonth":"01",
										"birthdayYear":"2000",
										"address1":"Teststrasse 1",
										"city":"Teststadt",
										"zip":"12345",
										"country":"CH",
										"agb":true,
										"birthday":"2000-01-01"
									}                    	                    
								},
								"status": 400,
								"assert" : {
									"equal_keys": {
										"code": "error.exists.user"
									}
								}                  

							}
							]
					},
					{     

						"name": "Register Researcher",
						"description": "Register new researcher",
						"api_calls": [
							{
								"request": {
									"method": "POST",
									"path": "/research/api/register",
									"params" : 
									{
										"name" : "Tests Research",
									    "description" : "A test research institute",
										
										"language":"en",
										"email":"{{researcher.email}}",
										"password":"{{researcher.password}}",
										"password2":"{{researcher.password}}",
										"firstname":"Resi",
										"lastname":"Research",
										"gender":"MALE",
										"address1":"Teststrasse 1",
										"city":"Teststadt",
										"zip":"12345",
										"country":"CH",
										"agb":true,
										"developer" : "{{developer.id}}"
									},
									"headers" : {        	                   	 
										"X-Session-Token" : "{{developersession}}"
									}
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
											"subroles" : { "type" : "array" },
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

						"name": "Double Register Check",
						"description": "Double Register Check",
						"api_calls": [
							
							
							
							
							{
								"request": {
									"method": "POST",
									"path": "/research/api/register",
									"params" : 
									{
										"name" : "Tests Research",
									    "description" : "A test research institute",
										
										"language":"en",
										"email":"{{researcher.email}}",
										"password":"{{researcher.password}}",
										"password2":"{{researcher.password}}",
										"firstname":"Resi",
										"lastname":"Research",
										"gender":"MALE",
										"address1":"Teststrasse 1",
										"city":"Teststadt",
										"zip":"12345",
										"country":"CH",
										"agb":true,
										"developer" : "{{developer.id}}"
									},             	                    
								},
								"status": 400,
								"assert" : {
									"equal_keys": {
										"field": "name",
										"type" : "exists"
									}
								}                  

							}
							]
					},
					{     

						"name": "Register Provider",
						"description": "Register new provider",
						"api_calls": [
							{
								"request": {
									"method": "POST",
									"path": "/providers/api/register",
									"params" : 
									{
										"name" : "Tests Provider",									    
										
										"language":"en",
										"email":"{{provider.email}}",
										"password":"{{provider.password}}",
										"password2":"{{provider.password}}",
										"firstname":"Doctor",
										"lastname":"Test",
										"gender":"MALE",
										"address1":"Teststrasse 1",
										"city":"Teststadt",
										"zip":"12345",
										"country":"CH",
										"agb":true,
										"developer" : "{{developer.id}}"
									},
									"headers" : {        	                   	 
										"X-Session-Token" : "{{developersession}}"
									}
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
											"subroles" : { "type" : "array" },
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

						"name": "Double Register Check",
						"description": "Double Register Check",
						"api_calls": [
							
							
							
							
							{
								"request": {
									"method": "POST",
									"path": "/providers/api/register",
									"params" : 
									{
										"name" : "Tests Provider",									    
										
										"language":"en",
										"email":"{{provider.email}}",
										"password":"{{provider.password}}",
										"password2":"{{provider.password}}",
										"firstname":"Doctor",
										"lastname":"Test",
										"gender":"MALE",
										"address1":"Teststrasse 1",
										"city":"Teststadt",
										"zip":"12345",
										"country":"CH",
										"agb":true,
										"developer" : "{{developer.id}}"
									},
									"headers" : {        	                   	 
										"X-Session-Token" : "{{developersession}}"
									}	                    
								},
								"status": 400,
								"assert" : {
									"equal_keys": {
										"field": "name",
										"type" : "exists"
									}
								}                  

							}
							]
					}					


					]
			}
			]
}