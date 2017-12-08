
db.runCommand({enablesharding: "access"});
db.runCommand({shardcollection: "access.consents", key:{"owner":1}});

