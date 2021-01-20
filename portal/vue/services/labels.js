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
import server from './server';

	var service = {};
	
	var content_translations = {};
	var group_translations;
	var storedLang;
	
	
	service.reset = function(lang) {
		content_translations = {};
		group_translations = undefined;
		storedLang = lang;
	};
	
	service.loadGroups = function(lang) {		
		return server.get(jsRoutes.controllers.FormatAPI.listGroups().url)
		.then(function(result) {
			group_translations = {};
			for (let group of result.data) {
				group_translations[group.system+":"+group.name] = group.label[lang] || group.label.en || group.name;
			}
		});		
	};
	
		
	service.getContentLabel = function(lang, name) {
		if (lang != storedLang) service.reset(lang);
		
		var existing = content_translations[name];
		if (!existing) {
			return server.post(jsRoutes.controllers.FormatAPI.searchContents().url, { "properties" : { "content" : name } , "fields" : ["content", "label"] })
			.then(function(result) {
				console.log(result.data);
				if (! result.data || ! result.data.length) return "? ("+name+")";
				var content = result.data[0];
				content_translations[content.content] = content.label[lang] || content.label.en || content.content;
				return 	content_translations[content.content];
				
			});
		} else {
			return Promise.resolve(existing);
		}
	};
	
	service.getGroupLabel = function(lang, system, name) {
		if (lang != storedLang) service.reset(lang);
		
		if (name.startsWith("cnt:")) return service.getContentLabel(lang, name.substring(4));
		
		if (!group_translations) {
			return service.loadGroups(lang).then(function() { return group_translations[system+":"+name]; });
		}
		var existing = group_translations[system+":"+name];
		if (!existing) {			
			return undefined;
		} else return Promise.resolve(existing);
	};
	
	service.isUncriticalFormat = function(format) {
		var fmts = { "fhir/Patient" : true, "fhir/Group" : true, "fhir/Person" : true, "fhir/Practitioner" : true, "fhir/ValueSet" : true, "fhir/Questionnaire" : true };
		return fmts[format];
	};
	
	service.simplifyQuery = function(query, removeRestrictedByAppName, removeUncritical) {
	  if (query.$or) {
		  
	     var result = { content : [], group : [], format : [] };
		 for (let part of query.$or) {
			 if (service.isFiltered(part, removeRestrictedByAppName, removeUncritical)) continue;
			 
			 if (part.content) result.content.push.apply(result.content, part.content);
			 if (part.format) result.format.push.apply(result.format, part.format);
			 if (part.group) result.group.push.apply(result.group, part.group);
			 if (part["group-system"]) result["group-system"] = part["group-system"];
		 }
		 return result;
	  } else return query;
	};
	
	service.isFiltered = function(part, removeRestrictedByAppName, removeUncritical) {
	  if (part["public"]=="only") return true;
	  console.log("appn:"+removeRestrictedByAppName);
	  console.log(part);
	  if (part.app && (part.app==="self" || (part.app.length==1 && part.app[0] === removeRestrictedByAppName))) return true;
	  if (removeUncritical && part.format && service.isUncriticalFormat(part.format)) return true;
	  return false;
	};
	
	service.parseAccessQuery = function(lang, query, outerquery, rarray) {
		console.log("IN:"+JSON.stringify(query));
		var ac = function(path) {
			if (query[path] !== undefined) return query[path];
			if (outerquery && outerquery[path] !== undefined) return outerquery[path];
			return undefined;
		};
		var unwrap = function(arr, field) {
			var out = [];
			for (let elem of arr) {
				if (elem[field]) {
					if (Array.isArray(elem[field])) {						
						if (elem[field].length == 1) {
							var copy = JSON.parse(JSON.stringify(elem));
							copy[field] = copy[field][0];
							out.push(copy);
						} else {
							for (let v of elem[field]) {
								var copy1 = JSON.parse(JSON.stringify(elem));
								copy1[field] = v;
								out.push(copy1);
							}
						}
					} else out.push(elem);
				} else out.push(elem);
			}
			return out;
		};
		var noarray = function(a) {
			if (Array.isArray(a) && a.length) return a[0];
			return a;
		};
	    
		var result = rarray || [];
		
		if (query.$or) {
			for (var i = 0;i<query.$or.length;i++) service.parseAccessQuery(lang, query.$or[i], query, result);
		} else {
		    if (Object.keys(query).length === 0) return [];
			var nblock = {};
			if (ac("format")) nblock.format = ac("format");
			if (ac("content")) nblock.content = ac("content");
			if (ac("code")) nblock.code = ac("code");			
			if (ac("group")) nblock.group = ac("group");
			if (ac("group-system")) nblock.system = ac("group-system");
			if (ac("public")) nblock["public"] = ac("public");
			if (ac("created-after")) {
				nblock.timeRestrictionMode = "created-after";
				nblock.timeRestrictionDate = ac("created-after");
			}
			if (ac("updated-after")) {
				nblock.timeRestrictionMode = "updated-after";
				nblock.timeRestrictionDate = ac("updated-after");
			}
			/*if (ac("data")) {
				var p = ac("data");
				nblock.customFilterPath = Objects.keys(p)[0];
				nblock.customFilterValue = p[nblock.customFilterPath];
			}*/
			if (ac("app")) {
				nblock.app = ac("app");
			}
			if (ac("owner")) {
				nblock.owner = noarray(ac("owner"));
			}
			console.log(nblock);			
			for (r of unwrap(unwrap(unwrap(unwrap(unwrap([ nblock ],"group"),"code"),"content"),"app"),"format")) {
				if (!r.app) r.app = "all";
				else if (r.app !== "all") { r.appName = r.app; r.app = "other"; }
				if (!r.owner) r.owner = "all";
				
				var c = r.content;
				if (c === "Patient" || c === "Group" || c === "Person" || c === "Practitioner") return;	
				console.log("PRE LOOPUP:"+r);
				if (r.content) {
					service.getContentLabel(lang, r.content).then(function(v) { r.display = v; });
				} else if (r.group) {
					service.getGroupLabel(lang, r["group-system"] || "v1", r.group).then(function(v) { r.display = v; });
				} else if (r.format) {
					r.display = r.format;
				}
				result.push(r); 
			}
		}
		return result;
	};
		
	export default service;