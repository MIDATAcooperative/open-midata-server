/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

// Access DB
db.consents.createIndex({ "owner" : 1, "type" : 1 });
db.consents.createIndex({ "authorized" : 1, "dataupdate" : 1 });
db.consents.createIndex({ "externalOwner" : 1 });
db.consents.createIndex({ "externalAuthorized" : 1 });
db.consents.createIndex({ "type" : 1, "study" : 1, "pstatus" : 1, "group" : 1 });
db.consents.createIndex({ "serviceId" : 1 });
db.consents.createIndex({ "observers" : 1 });
db.vconsents.createIndex({"_id._id" : 1, "_id.version" : 1});
db.groupmember.createIndex({ "member" : 1 });
db.groupmember.createIndex({ "userGroup" : 1 });
db.auditevents.createIndex({ "authorized" : 1, "event" : 1 });
db.auditevents.createIndex({ "about" : 1, "authorized" : 1 });
db.auditevents.createIndex({ "timestamp" : 1, "event" : 1 });
db.auditevents.createIndex({ "timestamp" : 1 });
db.subscriptions.createIndex({ "owner" : 1, "format" : 1 });
