// User DB
db.loinc.createIndex({ "SHORTNAME" : "text", "LONG_COMMON_NAME" : "text", "LOINC_NUM" : "text" });
db.plugins.createIndex({ "filename" : 1 });
db.studies.createIndex({ "owner" : 1 });
db.users.createIndex({ "role" : 1 , "emailLC" : 1});
// XXXX Remove after next update
db.users.find({ emailLC : { $exists : false } }).forEach(function(e) { db.users.update({ _id : e._id }, { $set : { emailLC : e.email.toLowerCase() }}); });

db.users.createIndex({ "midataID" : 1 });



