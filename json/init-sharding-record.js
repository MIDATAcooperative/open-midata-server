db.runCommand({enablesharding: "record"});
db.runCommand({shardcollection: "record.records", key:{"stream":1,"time":1}});


