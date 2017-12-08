
db.runCommand({enablesharding: "mapping"});
db.runCommand({shardcollection: "mapping.aps", key:{"_id":1}});
