// Access DB
db.consents.createIndex({ "owner" : 1 });
db.consents.createIndex({ "authorized" : 1, "dataupdate" : 1 });
db.consents.createIndex({ "externalOwner" : 1 });
db.consents.createIndex({ "externalAuthorized" : 1 });
db.consents.createIndex({ "type" : 1, "study" : 1, "pstatus" : 1, "group" : 1 });
db.groupmember.createIndex({ "member" : 1 });
db.groupmember.createIndex({ "userGroup" : 1 });
db.auditevents.createIndex({ "authorized" : 1, "event" : 1 });
db.auditevents.createIndex({ "about" : 1, "authorized" : 1 });
db.auditevents.createIndex({ "timestamp" : 1, "event" : 1 });
db.subscriptions.createIndex({ "owner" : 1, "format" : 1 });