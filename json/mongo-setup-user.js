// User DB
db.loinc.createIndex({ "SHORTNAME" : "text", "LONG_COMMON_NAME" : "text", "LOINC_NUM" : "text" });
db.plugins.createIndex({ "filename" : 1 });
db.studies.createIndex({ "owner" : 1 });
db.users.createIndex({ "role" : 1 , "email" : 1});
db.users.createIndex({ "midataID" : 1 });



