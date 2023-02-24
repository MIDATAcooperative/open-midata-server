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

package utils.csv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import utils.exceptions.BadRequestException;

public class CSVConverter {

	private JsonNode mapping;
	private JsonNode selectedMapping;
	private String selectedFile;
	private String multiSeparator = " / ";
	private String globalMissing = "";
	
	//private JsonNode field;
	private JsonNode all;
	
	private Map<String, JsonNode> _group;
	private String _keyValue;
	private StringBuilder outBuffer;
	private CSVWriter writer;
	
	public CSVConverter(JsonNode mapping, String selectedFile) {
		this.mapping = mapping;
		this.selectedFile = selectedFile;
		this._group = new HashMap<String, JsonNode>();
		this.writer = new CSVWriter();
		this.outBuffer = new StringBuilder();
	}
	
		
	public void prepareMapping() throws BadRequestException {
		if (!mapping.isArray()) throw new BadRequestException("error.invalid.csvmapping", "Not an array");
		for (JsonNode map : mapping) {
			
			boolean hasFile = map.path("file").isTextual();
			boolean hasFields = map.path("fields").isArray() && !map.path("fields").isEmpty();
			
			if (!hasFile || !hasFields) throw new BadRequestException("error.invalid.csvmapping", "No file or no fields");
			
			if (selectedFile == null || selectedFile.equals(map.path("file").asText())) {
				selectedMapping = map;
				
				if (map.hasNonNull("fieldSeparator")) writer.setSeparator(map.path("fieldSeparator").asText());
				if (map.hasNonNull("multiValueSeparator")) multiSeparator = map.path("multiValueSeparator").asText();
				if (map.hasNonNull("missingValues")) globalMissing = map.path("missingValues").asText();
				
				List<String> headers = new ArrayList<String>();
										
				for (JsonNode f : map.path("fields")) {
					if (f.has("csv")) headers.add(f.path("csv").asText());
					else headers.add(f.asText());
				}
				
				prepareNested(headers, map);
				if (map.path("bom").asBoolean(true)) {
					outBuffer.append('\uFEFF');
				}
				write(headers);
			}			
			/*var writer = csvWriter({ headers: headers });
            writer.pipe(fs.createWriteStream(this.destpath+map.file));            
            map.writer = writer;*/
		}
	}
	
	private String[] path(JsonNode node) {
		return node.path("fhir").asText().split("\\.");
	}
	
	public void prepareNested(List<String> headers, JsonNode map) throws BadRequestException {
		
		if (map.hasNonNull("fields")) {
		  if (map.path("fields").size() != headers.size()) throw new BadRequestException("error.invalid.csvmapping", "Inconsistent headers");
		  int i=0;
		  for (JsonNode p : map.path("fields")) {			  			  
			  if (p.hasNonNull("csv") && !headers.get(i).equals(p.path("csv").asText())) throw new BadRequestException("error.invalid.csvmapping", "Inconsistent headers");
			  i++;
		  }
		}
		
		if (map.hasNonNull("include")) {
			for (JsonNode inc : map.path("include")) {
				this.prepareNested(headers, inc);
			}
		}
		
		if (map.hasNonNull("group")) {			
			_group = new HashMap<String, JsonNode>();
			
			for (JsonNode group : map.path("group")) {		
	          if (!group.hasNonNull("key") || !group.path("key").hasNonNull("fhir")) throw new BadRequestException("error.invalid.csvmapping", "Missing group key definition");
	          if (!group.path("as").isTextual()) throw new BadRequestException("error.invalid.csvmapping", "Missing group 'as' definition");
		  }
		}
	}
	
	public void endMapping() {
        //for (var map of this.mapping) {		
	    flushGroup(selectedMapping);	
        //   map.writer.end();
		//}
	}
	
	public String fetch() {
		String out = outBuffer.toString();
		outBuffer = new StringBuilder();
		return out;
	}

		
	public boolean checkMatch(List<String> val, JsonNode fval) {
		for (String v : val) {
			if (checkMatch(v, fval)) return true;
		}
		return false;
	}
	
	public boolean checkMatch(String val, JsonNode fval) {
		//System.out.println("checkMatch="+val);
		if (fval.isArray()) {
			for (JsonNode n : fval) {
				if (n.asText().equals(val)) return true;
			}
			return false;
		} else if (fval.isObject()) {
			
			return val != null && val.length() > 0;
			// XXX Function support
			//return fval(val); 
			
		} else if (!fval.asText().equals(val)) return false; 
        return true;
	}
	
	public void process(JsonNode data) {
		
		JsonNode current = selectedMapping;
						
		if (current.hasNonNull("group")) {
			JsonNode group = current.path("group");
			for (int grp=0;grp<group.size();grp++) {
				preprocessMapping(current, data, group.path(grp), data, group.path(grp), true);
			}
		} else if (current.hasNonNull("include")) {
			JsonNode include = current.path("include");
			for (int inc=0;inc<include.size();inc++) {
				preprocessMapping(current, data, include.path(inc), data, include.path(inc), false);
			}
		} else preprocessMapping(current, data, current, data, current, false);
	
	}
	
	public boolean processFilter(JsonNode data, JsonNode current, JsonNode filter) {
		if (filter.hasNonNull("$find")) {
			ObjectNode f = Json.newObject();
			f.set("filter", filter);
			this.all = data;
			String exists = extract(data, filter.path("$find").asText().split("\\."), f);
	        return exists!=null && exists.length()>0;              
		} else {		
	   
		    boolean match = true;
		    Iterator<String> it = filter /*current.path("filter")*/.fieldNames();
			while (it.hasNext()) {
				String key = it.next();
				if (key.equals("$find")) continue;
				List<String> val = extractPlain(data, key.split("\\."), null);
				JsonNode fval = filter.path(key);
				if (!checkMatch(val,fval)) match = false;					
			}
			if (!match) return false;
			return true;
		}
	}
	
	public void preprocessMapping(JsonNode base, JsonNode data, JsonNode map, JsonNode subdata, JsonNode current, boolean grouping) {		
		if (current.hasNonNull("filter")) {	
			this.all = data;
			if (current.path("filter").isArray()) {
				for (JsonNode f : current.path("filter")) {
			      if (!processFilter(data, current, f)) return;
				}
			} else if (!processFilter(data, current, current.path("filter"))) return;
						
		}
		
		if (current.hasNonNull("forEach")) {
		  String forEach = current.path("forEach").asText();
		  if (subdata.path(forEach).isArray()) {
			  JsonNode repeats = subdata.path(forEach);			  
			  for (JsonNode item : repeats) {
				  ((ObjectNode) subdata).set(forEach, item);
				
				  if (current.hasNonNull("nested")) preprocessMapping(base, data, map, item, current.path("nested"), grouping);
				  else processGrouping(base, data, map, grouping);				  
			  }
			  ((ObjectNode) subdata).set(forEach, repeats);
          } else {			
		    if (!current.path("skipEmptyForEach").asBoolean() || subdata.hasNonNull(forEach)) processGrouping(base, data,map, grouping);		  
		  }
        } else {			
		  processGrouping(base, data,map, grouping);		  
		}
	}
	
	public void processGrouping(JsonNode base, JsonNode data, JsonNode map, boolean grouping) {
		if (grouping) {
		   this.all = data;		   
		   String keyValue = extract(data, path(map.path("key")), map.path("key"));      
           this.all = null;

		   JsonNode fixedData = data.deepCopy();
	       String name = map.path("as").asText();

           if (base.path("sorted").asBoolean() && !keyValue.equals(_keyValue)) {
	          flushGroup(base);
              _keyValue = keyValue;
           }           

           if (!_group.containsKey(keyValue)) _group.put(keyValue, Json.newObject());
           JsonNode group = _group.get(keyValue);
           if (!group.has(name)) ((ObjectNode) group).set(name, Json.newArray());
           ((ArrayNode) group.path(name)).add(fixedData);
                      
		} else {		
		   this.processMapping(base, data,map);
		}
	}
	
	public boolean hasData() {
		return !_group.isEmpty();
	}
	
	public void flushGroup(JsonNode base) {
		if (!_group.isEmpty()) {
			for (JsonNode v : _group.values()) {
			//System.out.println("---------------GROUP:"+v.toString());
			   preprocessMapping(base, v, base, v, base, false);                                  
			}
			_group.clear();
		}
	}
	
	private void write(List<String> out) {		
		writer.writeToCsvBuffer(out, outBuffer);
		
	}
	
	public void processMapping(JsonNode base, JsonNode data, JsonNode map) {
		//System.out.println("mapping="+data.toString());
		List<String> out = new ArrayList<String>();
		for (JsonNode field : map.path("fields")) {
			this.all = data;			
			String value = this.extract(data, path(field), field);			
			this.all = null;
			
						
			out.add(value);
		}
		write(out);
	}
	
	public String extract(JsonNode data, String[] path, JsonNode field) {
		//System.out.println("extract="+path+" data="+data);		
		return extractFromList(extract(data, path, 0, field), field);
	}
	
	public List<String> extractPlain(JsonNode data, String[] path, JsonNode field) {
		//System.out.println("extractPlain="+path+" data="+data);		
		return extract(data, path, 0, field);
	}
	
	public List<String> extract(JsonNode data, String[] path, int idx, JsonNode field) {		
		//System.out.println("extract pathlen="+path.length+" idx="+idx);
		if (idx < path.length) {
			JsonNode dataold = data;
			//System.out.println("datapath="+path[idx]);
			data = data.path(path[idx]);						
			return handleArrays(data, path, idx, field, dataold, path[idx]);			
		} else return handleArraysFinal(data, field, null, null);
	}
	
	public List<String> handleArrays(JsonNode data, String[] path, int idx, JsonNode field, JsonNode setter, String name) {
		if (data == null) return null;
		if (data.isArray()) {
			if (data.size() == 0) return null;
			if (data.size() == 1) return extract(data.path(0), path, idx+1, field);
			List<String> dat = new ArrayList<String>();
			for (int i=0;i<data.size();i++) {
				if (setter!=null) ((ObjectNode) setter).set(name, data.path(i));
				List<String> v = extract(data.path(i), path, idx+1, field);
				if (v!=null) dat.addAll(v);
			}
			if (setter != null) ((ObjectNode) setter).set(name, data);
			return dat;				
		} else return extract(data, path, idx+1, field);
	}
	
	public List<String> handleArraysFinal(JsonNode data, JsonNode field, JsonNode setter, String name) {
		if (data == null) return null;
		if (data.isArray()) {
			if (data.size() == 0) return null;
			if (data.size() == 1) return Collections.singletonList(extractFinal(data.get(0), field));
			List<String> dat = new ArrayList<String>();
			for (int i=0;i<data.size();i++) {
				if (setter!=null) ((ObjectNode) setter).set(name, data.path(i));
				String v = extractFinal(data.path(i), field);
				if (v!=null) dat.add(v);
			}
			if (setter != null) ((ObjectNode) setter).set(name, data);
			return dat;				
		} else return Collections.singletonList(extractFinal(data, field));
	}
		
	
	public String extractFromList(List<String> input, JsonNode field) {
		String separator = multiSeparator;
		String result = null;
		
		
		if (field != null) {			
			if (input.size()>0 && field.path("onlyFirst").asBoolean()) result = input.get(0);
			if (field.hasNonNull("separator")) separator = field.path("separator").asText();
		}
		
		if (input.size() == 1) result = input.get(0);
		
		if (result == null && !input.isEmpty()) {
			StringBuffer out = new StringBuffer();
			boolean first = true;
			for (String r : input) {
				if (r != null) {
					if (first) first = false; else out.append(separator);
					out.append(r);
				}
			}
			result = out.toString();
		}
		
		if (result == null || result.length() == 0) {
			if (field != null && field.hasNonNull("missing")) return field.path("missing").asText();
			return globalMissing;
		}
		
		return result;
	}
	
	public String extractFinal(JsonNode data, JsonNode field) {
		
		//System.out.println("extractFinal="+data.toString());
		if (field != null && field.hasNonNull("filter")) {
			JsonNode current = field;			
			field = null;
			
			boolean match = true;
			Iterator<String> it = current.path("filter").fieldNames();
			while (it.hasNext()) {
			    String key = it.next();
                if (key.equals("$find")) continue;
				List<String> val = extractPlain(this.all, key.split("\\."), null);
				if (!checkMatch(val,current.path("filter").path(key))) match = false;				
			}
			field = current;
			if (!match) return null;
		}
		
		if (field != null && field.hasNonNull("use")) return useFunction(data, field.path("use"));
		
		if (data.hasNonNull("coding")) {
			String v = extractFromList(handleArraysFinal(data.path("coding"), null, null, null), field);
			if (v!=null) return v;
		}
		
		if (data.hasNonNull("system") && data.hasNonNull("code")) return data.path("system").asText()+"|"+data.path("code").asText();
		if (data.hasNonNull("system") && data.hasNonNull("value")) return data.path("system").asText()+"|"+data.path("value").asText();
		if (data.hasNonNull("code")) return data.path("code").asText();
		if (data.hasNonNull("reference")) return data.path("reference").asText();
		if (data.hasNonNull("display")) return data.path("display").asText();
		if (data.hasNonNull("text")) return data.path("text").asText();
		if (data.hasNonNull("value") && data.hasNonNull("unit")) return data.path("value").asText()+" "+data.path("unit").asText();
		if (data.hasNonNull("value")) return data.path("value").asText();
		if (data.isObject() || data.isArray()) return "true";
		return data.asText();
	}
	
	public String useFunction(JsonNode data, JsonNode use) {
		String name = use.path("$function").asText();
		if (name == null) name = "map";
		switch(name) {
		case "dateOnly" : return data.isTextual() ? data.asText().split("T")[0] : null;
		case "map" : 
			if (!(data.isValueNode()) || data.isNull()) return null;
			String v = use.path(data.asText()).asText();
			if (v != null) return v; else return data.asText();
		case "idFromRef" :
			if (!(data.isValueNode()) || data.isNull()) return null;
			String[] splitted = data.asText().split("/");
			if (splitted.length>1) return splitted[1];
			return data.asText();
		case "exists" :
			String falseValue = null;
			String trueValue = "true";
			if (use.hasNonNull("true")) trueValue = use.path("true").asText();
			if (use.hasNonNull("false")) falseValue = use.path("false").asText();
			if (use.path("value").asBoolean(true)) {
			    return (data.isMissingNode() || data.isNull()) ? falseValue : trueValue;
			} else {
				return (data.isMissingNode() || data.isNull()) ? trueValue : falseValue;
			}		
		case "equals" :
			String val = extract(all, path(use), use);
			return (data.asText().equals(val)) ? "true" : null;
		}
		return null;
	}

}
