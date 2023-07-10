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

// User DB
db.loinc.createIndex({ "SHORTNAME" : "text", "LONG_COMMON_NAME" : "text", "LOINC_NUM" : "text" });
db.plugins.createIndex({ "filename" : 1 });
db.pluginicons.createIndex({ "plugin" : 1, "use" : 1});
db.studies.createIndex({ "owner" : 1 });
db.users.createIndex({ "role" : 1 , "emailLC" : 1});
db.users.createIndex({ "midataID" : 1 });
db.instancestats.createIndex({ "date" : 1 });
db.studyapplink.createIndex({ "studyId" : 1 });
db.studyapplink.createIndex({ "appId" : 1 });

db.usagestats.dropIndexes();
db.usagestats.createIndex({ "date" : 1, "object" : 1, "detail" : 1, "action" : 1 }, { "unique" : true });

db.users.update({ emailLC : "development@midata.coop", role : "DEVELOPER" }, { $set : { email : "developers@midata.coop", emailLC : "developers@midata.coop" }})
db.plugins.find({ creator : ObjectId("55eff624e4b0b767e88f92b9") }).forEach(function(e) { db.plugins.update({ _id : e._id }, { $set : { creatorLogin : "developers@midata.coop" }})});

// XXXX Remove after next update
db.users.find({ subroles : { $in : ["TRIALUSER", "NONMEMBERUSER" ] }}).forEach(function(e) { db.users.update({ _id : e._id }, { $set : { subroles : [] }})});
//db.users.find({ agbStatus : { $exists : false }}).forEach(function(e) { db.users.update({ _id : e._id }, { $set : { agbStatus : "NEW" }})});

db.plugins.find({ status : "DELETED", filename : { $exists : true } }).forEach(function(e) { db.plugins.update({ _id : e._id }, { $unset : { filename : 1 }})});
db.plugins.find({ sendReports : { $exists : false } }).forEach(function(e) { db.plugins.update({ _id : e._id }, { $set : { sendReports : true }})});

db.devstats.createIndex({ "plugin" : 1, "action" : 1, "params" : 1});
db.studies.find({ type : { $exists : false}}).forEach(function(e) { db.studies.update({ _id : e._id }, { $set : { type : "CLINICAL", joinMethods : ["API", "APP", "PORTAL", "RESEARCHER"] } }); });

db.plugins.find({ linkedStudy : { $ne : null } }).forEach(function(e) { db.studyapplink.insert({ studyId : e.linkedStudy, appId : e._id, type : (e.mustParticipateInStudy ? ["OFFER_P","REQUIRE_P"] : ["OFFER_P"] ), usePeriod:["RUNNING","FINISHED","PRE"], validationDeveloper : "VALIDATED", validationResearch : "VALIDATED", active : true }); db.plugins.update({ _id : e._id }, { $unset : { linkedStudy : 1, mustParticipateInStudy : 1 } }); });

db.studyapplink.find({ linkTargetType : { $exists : false } }).forEach(function(e) { db.studyapplink.update({ _id : e._id}, { $set : { linkTargetType : "STUDY" } }); });

db.providers.find({ status : { $exists : false } }).forEach(function(e) { db.providers.update({ _id : e._id}, { $set : { status : "NEW" } }); });