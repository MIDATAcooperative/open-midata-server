'use strict';

angular.module('calendarApp')
  .factory('eventProvider', ['midataServer', function(midataServer) {
	  var scope = {};
	  var criteria = { content : [] };
	  var tlb = {};
	  scope.setOwners = function(owners) {
		criteria.owner = owners;  
	  };
	  
	  scope.setContents = function(contents) {
		criteria.content = contents;
	  };
	  
	  scope.setTlb = function(def) {
		  tlb[def.id] = def;
	  };
	  
	  scope.clearTlb = function(id) {
		  tlb[id] = undefined;
	  };
	  
	  scope.getTlb = function(id) {
		  return tlb[id];
	  };
	  
	  scope.hasContent = function(id) {
		  return criteria.content.indexOf(id) >= 0;
	  };
	  
	  scope.query = function(authToken, start, end, callback) {
		  console.log(start);
		  console.log(end);
		  var shorten = function(title) {
			  if (title.length > 15) {
				  var t = title.split(' ');
				  title = t[t.length-1];
			  }
			  return title;
		  };
		  var shortStr = function(title) {
			  if (title.length > 15) {
				 return title.substr(0,12)+"...";
			  }
			  return title;
		  };
		  
		  if (criteria.content.length > 0) {
		  midataServer.getRecords(authToken, { "format" : ["fhir/Observation"], content: criteria.content, "index" : { "effectiveDateTime" : { "$gt" : new Date(start.calendar()), "$lt" : new Date(end.calendar()) } } }, ["created", "data", "content" ])
	     	 .then(function(results) {
	     		var entries = [];
	     		var idx = 0;
	     		 
	     		angular.forEach(results.data, function(record) {
	                var cdate = new Date(record.created).toISOString();
	                if (record.data.resourceType == "Observation") {
	              	  var q = null;
	              	  if (record.data.valueQuantity) q = record.data.valueQuantity.value;
	              	  if (record.data.valueString) q = shortStr(record.data.valueString); 
	              	  if (record.data.valueCodeableConcept) q = shortStr(record.data.valueCodeableConcept.text || record.data.valueCodeableConcept.coding[0].display);
	              	  var dateTime = record.data.effectiveDateTime || cdate;
	              	  var title = record.data.code.text || record.data.code.coding[0].display; 
	              	  var color = "#707070";
	              	  var tlbdef = tlb[record.content];
	              	  if (tlbdef) color = (tlbdef.operator) == ">" ? 
	              			      ( (q > tlbdef.limit) ? "#00a000" : "#a00000" ) :
	              			      ( (q < tlbdef.limit) ? "#00a000" : "#a00000" );
	              	  
	              	  var e = {
	              		  id : record._id.$oid,
	              	      title : shorten(title)+((q != null) ? (":"+q) : ""),
	              	      allDay : true, 	
	              	      start : dateTime,	              	       
	              	      editable : false, 	
	              	      color : color, 	
	              	      textColor : "#ffffff"	                            
	                  };
	                  entries[idx++] = e;   
	                } 
	            });
	     		console.log(entries);   		      		      		      		 
	     		callback(entries); 
	     	 });
		  } else { callback([]); }
	  };
	  
	  
	  return scope;	  	  
  }]); 
