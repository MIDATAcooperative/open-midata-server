/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('services')
.factory('studies', ['server', function(server) {
	var service = {};
	
	service.studytypes = ["CLINICAL", "CITIZENSCIENCE", "COMMUNITY"];
	service.joinmethods = ["APP", "PORTAL", "RESEARCHER", "API", "APP_CODE", "ALGORITHM", "CODE", "TRANSFER"];
	service.linktypes = ["OFFER_P", "OFFER_EXTRA_PAGE", "OFFER_INLINE_AGB", "REQUIRE_P", "RECOMMEND_A", "AUTOADD_A"];
	service.executionStati = ["PRE", "RUNNING", "FINISHED", "ABORTED"];
	
	service.roles = [
		{ id :"SPONSOR",
		  roleName : "Sponsor",
		  readData : true,
		  writeData : false,
		  unpseudo : false,
		  "export" : true,
		  changeTeam : true,
		  auditLog : true,
		  participants : true,
		  setup : true
		},
		{ id :"INVESTIGATOR",
			  roleName : "Investigator",
			  readData : true,
			  writeData : true,
			  unpseudo : true,
			  "export" : false,
			  changeTeam : false,
			  auditLog : true,
			  participants : true,
			  setup : false
		},
		{ id :"MONITOR",
			  roleName : "Monitor",
			  readData : true,
			  writeData : false,
			  unpseudo : true,
			  "export" : false,
			  changeTeam : false,
			  auditLog : true,
			  participants : false,
			  setup : false
		},
		{ id :"AUDITOR",
			  roleName : "Auditor",
			  readData : true,
			  writeData : false,
			  unpseudo : false,
			  "export" : true,
			  changeTeam : false,
			  auditLog : true,
			  participants : false,
			  setup : false
		},
		{ id :"STUDYNURSE",
			  roleName : "Study Nurse",
			  readData : false,
			  writeData : false,
			  unpseudo : true,
			  "export" : false,
			  changeTeam : false,
			  auditLog : false,
			  participants : true,
			  setup : false
		},
		{ id :"OTHER",
			  roleName : "",
			  readData : false,
			  writeData : false,
			  unpseudo : false,
			  "export" : false,
			  changeTeam : false,
			  auditLog : false,
			  participants : false,
			  setup : false
		}
		
		
	];
	
	service.search = function(properties, fields) {
		var data = {"properties": properties, "fields": fields };
		return server.post(jsRoutes.controllers.Studies.search().url, JSON.stringify(data));
	};
	
	service.updateParticipation = function(studyId, data) {
		return server.patch(jsRoutes.controllers.members.Studies.updateParticipation(studyId).url, JSON.stringify(data));
	};
	
	service.research = {
			list : function() {	
		       return server.get(jsRoutes.controllers.research.Studies.list().url);
	        }
	};
	
	return service;
}]);