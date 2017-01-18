// Access DB
db.consents.createIndex({ "owner" : 1 });
db.consents.createIndex({ "authorized" : 1 });
db.groupmember.createIndex({ "member" : 1 });
db.groupmember.createIndex({ "userGroup" : 1 });