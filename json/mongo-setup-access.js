// Access DB
db.consents.createIndex({ "owner" : 1 });
db.consents.createIndex({ "authorized" : 1 });
db.consents.createIndex({ "externalOwner" : 1 });
db.consents.createIndex({ "externalAuthorized" : 1 });
db.groupmember.createIndex({ "member" : 1 });
db.groupmember.createIndex({ "userGroup" : 1 });
db.auditevents.createIndex({ "authorized" : 1, "event" : 1 });