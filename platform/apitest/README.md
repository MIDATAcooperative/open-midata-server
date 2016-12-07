This folder contains test cases for the portal API

You need to have nodeJS installed.  

To install the test framework:

```
sudo npm install jsonapitest -g
```

To configure which server shall be used edit "config.js" file in this directory.

To run the tests execute:

```
jsonapitest *.js
```

The test results are places in

```
jsonapitest-results.json
```

For capturing requests "Postman" (Chrome Plugin) has been used. The Postman Request Interceptor needs
to be turned on both in the Postman App as in Chrome. 
