'use strict';
 
module.exports = 
{
  "suites": [
    {
        "name": "Login Tests",
        "tests": [
          {
            "name": "bad password",
            "description": "login Account User with bad password",
            "api_calls": [
              {
                "request": {
                    "method": "POST",
                    "path": "/members/api/login",
                    "params" : { "email" : "{{member.email}}", "password" : "wrongPassword" }
                },
                "status": 400,
                "assert" : {
                   "equal_keys": {
                      "code": "error.invalid.credentials"
                   }
                }
                                
              }
            ]
          }
          
      ]
    }
  ]
}