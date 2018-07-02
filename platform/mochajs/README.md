# Quick start

Install packages with

```sh
npm install
```

And run the test with

```sh
npm test
```

# Folder structure

```
mochajs
│   |    package.json // -> npm configurations
│
└───files
│   |   configurations.js // -> configurations for the tests
│   
└───managers
|   │   base_manager.js // -> modules to authenticate and accept consents
|
└───models
|   |   observations.js // -> Observation model (FHIR)
|
└───test
    |   scenario##.js // -> test scenarios
```