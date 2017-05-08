// User DB
db.loinc.createIndex({ "SHORTNAME" : "text", "LONG_COMMON_NAME" : "text", "LOINC_NUM" : "text" });
db.plugins.createIndex({ "filename" : 1 });
db.studies.createIndex({ "owner" : 1 });
db.users.createIndex({ "role" : 1 , "emailLC" : 1});
db.users.createIndex({ "midataID" : 1 });

// XXXX Remove after next update
//db.users.find({ subroles : { $exists : false }}).forEach(function(e) { db.users.update({ _id : e._id }, { $set : { subroles : [] }})});
//db.users.find({ agbStatus : { $exists : false }}).forEach(function(e) { db.users.update({ _id : e._id }, { $set : { agbStatus : "NEW" }})});

db.plugins.find({ status : "DELETED", filename : { $exists : true } }).forEach(function(e) { db.plugins.update({ _id : e._id }, { $unset : { filename : 1 }})});

db.devstats.createIndex({ "plugin" : 1, "action" : 1, "params" : 1});

