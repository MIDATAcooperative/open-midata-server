package utils.sandbox;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.F;
import play.libs.F.Promise;
import play.libs.Json;
import utils.exceptions.AppException;

public class FitbitImport {

	public void run(final MidataServer midataServer) throws AppException {
		Date fromDate = new Date();		
		Date toDate = new Date();
		
		SimpleDateFormat dateFormat = 
	            new SimpleDateFormat("yyyy-MM-dd");
		
		String formattedFromDate = dateFormat.format(fromDate);
		String formattedEndDate = dateFormat.format(toDate);
		
		
		
		/*
		if (fromDate > toDate) return;
		
				
						
									"name": "Activities - Steps",
					"title": "Fitbit activities (steps) {date}",
					"endpoint": ,
					"content" : "activities/steps",
					"unit" : "steps"
									
											 */
		String baseUrl = "https://api.fitbit.com";
		String endpoint = "/1/user/-/activities/steps/date/{date}/1d.json";
		final String content = "activities/steps";
		final String format = "measurements";
		final String unit = "steps";
		final JsonNodeFactory factory = JsonNodeFactory.instance;
		
		Promise<JsonNode> promise = midataServer.oauth2Request(baseUrl + endpoint.replace("{date}", formattedFromDate).replace("1d", formattedEndDate));
		promise.onRedeem(new F.Callback<JsonNode>() {
		    @Override
		    public void invoke(JsonNode result) throws Throwable {
		        if (result.has("errors")) {
		        	
		        } else {
                  Iterator<Entry<String,JsonNode>> it = result.fields();
                  while (it.hasNext()) {
                	                  	  
                	  Map<String, ArrayNode> grouped = new HashMap<String, ArrayNode>();
                	  
                	  Entry<String, JsonNode> entry =it.next();
                	  String typeName = entry.getKey();
                	  for (JsonNode child : entry.getValue()) {
                		  if (child.has("value")) {
                			  JsonNode date = child.get("dateTime");
                			  if (date == null) date = child.get("date");
                			  if (!child.has("unit")) ((ObjectNode) child).put("unit", unit);
                			  String dt = date.asText();
                			  if (!grouped.containsKey(dt)) {
                				  grouped.put(dt, factory.arrayNode());
                			  }
                			  grouped.get(dt).add(child);
                		  }
                		  
                	  }
                	  
                	  for (String date : grouped.keySet()) {
                		  ObjectNode recordData = Json.newObject();
                		  recordData.put(typeName, grouped.get(date));
  						
                		  midataServer.createRecord("Steps", "", content, format, Json.stringify(recordData));
  						                		 
                	  }
                  }       	  
		        }
		    }
		});
		/*
				console.log(response);
				angular.forEach(response, function(v,dataName) {
					var grouped = {};
					
					angular.forEach(v, function(itm) {
					  var val = itm.value || itm.amount;
					  if (val != 0) {
						  var recDate = itm.dateTime || itm.date;
						  if (measure.unit != null && itm.unit == null) itm.unit = measure.unit;
						  if (grouped[recDate] == null) grouped[recDate] = [];
						  grouped[recDate].push(itm);
					  }
					});						
					
					angular.forEach(grouped, function(itms, date) {
						var rec = {};
						rec[dataName] = itms;

						saveRecord(measure.title, measure.content, date, rec);							
					});
				});				
			}
		

		}).*/
	}
}
