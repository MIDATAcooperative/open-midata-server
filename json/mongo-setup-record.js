// Record DB
db.records.createIndex({ "stream" : 1, "time" : 1 });

db.indexes.drop(); 
db.indexes.createIndex({ "owner" : 1, "format" : 1 });

db.vrecords.createIndex({"_id._id" : 1, "_id.version" : 1});