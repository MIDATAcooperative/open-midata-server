Examples for study data evaluation scripts that share aggregated data back to
the study participants.

The examples are not meant to be run as they are but to be adapted to the needs. They are not runnable without a Study beeing
set up and an application registered that is assigned as research app to the study. 

These are the provided examples:
example-total-steps : 
Calculates the sum of all steps of all study participants for one given day (Observation.effectiveDateTime)
Creates one FHIR Group that represent "all participants" of the study.
Creates one Observation containing the total step count for each day this script is run. The observations subject will be the group. 

example-weight-per-yearofbirth.js
Takes the newest body weight Observation of each participant and calculates the average weight of participants for each year of birth.
Creates one FHIR Group for each year of birth
Creates one Observation per Group containing the average weight.

To use them you need node.js installed on your computer, then run

npm install

to install required packages.

After the examples have been adapted to your needs, they can be run with

node example-total-step.js
or
node example-weight-per-yearofbirth.js

The included midata-research.js library allows you to write study evaluation code without needing to worry about:
* Complex timing due to the excessive use of promises
* Pagination for large results
* If properly written the script will never hold full query results in memory so that large results may be processed.

The following methods are offered by the library (which all need to be executed in one chain):

Application start:

midata.app("app-name", "app-secret", "device-code")
.useServer("https://server-url.ch")
.loginResearcher("username", "userpassword")

.forEachMatch(resource, { query criteria } , function(oneResult) )
This will do a FHIR search against resource "resource" using the search criteria provided (all FHIR search parameters possible)
and will call the passed callback function for each returned result tuple.
Does not load whole result into memory. Handles pagination and index build wait times.

.modifyDB( function(db) )
Executes the callback function and provides a "db" Object which has functions
db.create(resource) and db.update(resource) which will create or update the provided resource on the MIDATA server.
Both create and update return a promise which resolves with the complete resource from the server (containing the assigned id and version)

.modifyDBBulk( function(db) )
Executes the callback function and provides a "db" Object which has functions
db.create(resource) and db.update(resource) which will create or update the provided resource on the MIDATA server.
In contrast to "modifyDB" all inserts and updates are executed in one transaction. Also no promises are returned if this method is used.

.then ( function() )
Just executes the provided callback function and continues with the next statement.
