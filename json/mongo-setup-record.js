// Record DB
db.records.createIndex({ "stream" : 1, "time" : 1 });
db.indexes.createIndex({ "owner" : 1, "format" : 1 });
