'use strict';

angular.module('calendarApp')
  .factory('eventProvider', ['midataServer', function(midataServer) {
	  var scope = {};
	  var criteria = { cal : { "appointment" : true, "observation" : true }, tlb : {} };	  
	  
	  scope.setOwners = function(owners) {
		//criteria.owner = owners;  
	  };
	  
	  scope.setCriteria = function(crit) {
		if (crit && crit.cal) {
		  criteria = crit;
		}
		scope.inited = true;
	  };
	  
	  scope.getCriteria = function() {
		  return criteria;
	  };
	  
	  scope.setTlb = function(def) {
		  criteria.tlb[def.id] = def;
	  };
	  
	  scope.clearTlb = function(id) {
		  criteria.tlb[id] = undefined;
	  };
	  
	  scope.getTlb = function(id) {
		  return criteria.tlb[id];
	  };
	  
	  scope.hasContent = function(cnt) {
		  return criteria.cal[cnt] != null;
	  };
	  	  	  	  
	  var shorten = function(title) {
		  if (title == null) return null;
		  if (title.length > 15) {
			  var t = title.split(' ');
			  title = t[t.length-1];
		  }
		  return title;
	  };
	  var shortStr = function(title) {
		  if (title == null) return null;
		  if (title.length > 15) {
			 return title.substr(0,12)+"...";
		  }
		  return title;
	  };
	  
	  var ccText = function(cc) {
		if (cc == null) return null;
		if (angular.isArray(cc)) {
			if (cc.length == 0) return null;
			return ccText(cc[0]);
		}
		if (cc.text) return cc.text;
		if (cc.coding && cc.coding.length > 0) return codingText(cc.coding[0]);
		return null;
	  };
	  
	  var codingText = function(coding) {
		if (coding == null) return null;
		if (coding.display) return coding.display;
		return coding.code;
	  };
	  
	  var refText = function(ref) {
		if (ref == null) return null;
		if (ref.display) return ref.display;
		if (ref.identifier) return ref.identifier.value;
		return null;
	  }; 
	  
	  var getCodeAndSystem = function(cc) {
		  if (cc == null) return {};
		  if (cc.code) return { code : cc.code, system : cc.system };
		  if (cc.coding) return getCodeAndSystem(cc.coding[0]);
		  if (angular.isArray(cc)) return getCodeAndSystem(cc[0]);
		  return {};
	  };
	  
	  var getCodeAndSystemStr = function(cc) {
		 var result = getCodeAndSystem(cc);
		 return result.system+"|"+result.code;
	  };
	  
	  scope.getCodeAndSystem = getCodeAndSystem;
	  scope.refText = refText;
	  
	  
	  scope.queryObservations = function(authToken, start, end, callback) {
		  				  
		  if (criteria.cal.observation) {
			  		     
			 var crit = {
			    "date" : [ "ge"+start.toISOString().split('T')[0], "le"+end.toISOString().split('T')[0] ],			 
			 };
			 //if (codes.length > 0 && codes.length < 1500) crit.code = codes.substr(1);
			 
		    midataServer.fhirSearch(authToken, "Observation", crit, true)
	     	 .then(function(results) {
	     		var entries = [];	     		
	     		var used = {};
	     		angular.forEach(results, function(record) {
	                	               
	              	  var q = null;
	              	  var u = "";
	              	  if (record.valueQuantity) { q = record.valueQuantity.value; u = record.valueQuantity.unit } 
	              	  if (record.valueString) q = shortStr(record.valueString); 
	              	  if (record.valueCodeableConcept) q = shortStr(ccText(record.valueCodeableConcept));
	              	  var start = record.effectiveDateTime;
	              	  var end = undefined;
	              	  if (record.effectivePeriod) {
	              		  start = record.effectivePeriod.start;
	              		  end = record.effectivePeriod.end;
	              	  }
	              	  var title = ccText(record.code); 
	              	  var color = "#707070";
	              	  
	              	  var tlbdef = criteria.tlb[getCodeAndSystemStr(record.code)];
	              	  if (tlbdef) color = (tlbdef.operator) == ">" ? 
	              			      ( (q > tlbdef.limit) ? "#00a000" : "#a00000" ) :
	              			      ( (q < tlbdef.limit) ? "#00a000" : "#a00000" );
	              	  if (q == "Yes") q = null;
	              	  var k = start+title;
	              	  if ((start || end) && !used[k]) {
	              		  
	              	  var e = {
	              		  id : record.id,
	              	      title : shorten(title)+((q != null) ? (":"+q) : ""),
	              	      allDay : true, 	
	              	      start : start,
	              	      end : end,
	              	      editable : false, 	
	              	      color : color,
	              	      what : title,
	              	      viewer : "fhir-observation",
	              	      who : [ refText(record.subject) ],
	              	      tlb : getCodeAndSystemStr(record.code),
	              	      details : ((q!=null) ? q : "")+" "+u,
	              	      textColor : "#ffffff"	                            
	                  };
	              	  used[k] = e; 
	                  entries.push(e);
	              	  }
	                
	            });
	     		//console.log(entries);   		      		      		      		 
	     		callback(entries); 
	     	 });
		  } else { callback([]); }
	  };
	  
	  scope.queryAppointments = function(authToken, start, end, callback) {
		  if (!criteria.cal.appointment) {			  
			  callback([]);
			  return;
		  }
		   var crit = {
				    "date" : [ "ge"+start.toISOString().split('T')[0], "le"+end.toISOString().split('T')[0] ],			 
		   };
							 
		  midataServer.fhirSearch(authToken, "Appointment", crit, true)
		     	 .then(function(results) {
		     		var entries = [];
		     				     		
		     		angular.forEach(results, function(record) {
		                	               
		              	  var people = [];		              	  
		              	  angular.forEach(record.participant, function(p) {
		              		 if (p.actor) people.push(refText(p.actor));  
		              	  });		              	  
		              	  		              	  
		              	  var title = record.description || "Appointment"; 
		              	 		              	  		              	  		           
		              	  var e = {
		              		  id : record.id,
		              	      title : shorten(title),
		              	      allDay : false, 	
		              	      start : record.start,
		              	      end : record.end,
		              	      editable : false, 	
		              	      viewer : "data-viewer",
		              	      what : title,
		              	      who : people,
		              	      details : record.description,
		              	                       
		                  };
		                  entries.push(e);		              	  
		                
		            });
		     		 		      		      		      		
		     		callback(entries); 
		     	 });			 
	   };
	   
	   scope.queryMedicationStatements = function(authToken, start, end, callback) {
			  if (!criteria.cal.medicationStatement) {			  
				  callback([]);
				  return;
			  }
			   var crit = {
					    "effective" : [ "ge"+start.toISOString().split('T')[0], "le"+end.toISOString().split('T')[0] ],			 
			   };
								 
			  midataServer.fhirSearch(authToken, "MedicationStatement", crit, true)
			     	 .then(function(results) {
			     		var entries = [];
			     				     		
			     		angular.forEach(results, function(record) {
			                	               			              				              	  
			              	  var title = ccText(record.medicationCodeableConcept) || refText(record.medicationReference) || "Medication"; 
			              	 		              	  		              	  		           
			              	  var e = {
			              		  id : record.id,
			              	      title : shorten(title),
			              	      allDay : false, 	
			              	      start : record.effectiveDateTime || (record.effectivePeriod || {}).start,
			              	      end : (record.effectivePeriod || {}).end,
			              	      editable : false, 	
			              	      viewer : "data-viewer",
			              	      what : title,
			              	      who : [ refText(record.subject) ],
			              	      details : record.description,
			              	                       
			                  };
			                  entries.push(e);		              	  
			                
			            });
			     		 		      		      		      		
			     		callback(entries); 
			     	 });			 
		   };
	  
	  
	  return scope;	  	  
  }])     
