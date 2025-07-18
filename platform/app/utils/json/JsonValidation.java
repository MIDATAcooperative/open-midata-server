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

package utils.json;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.regex.Pattern;

import org.hazlewood.connor.bottema.emailaddress.EmailAddressParser;

import com.fasterxml.jackson.databind.JsonNode;

import models.MidataId;
import utils.exceptions.BadRequestException;

/**
 * functions for validating JSON input
 *
 */
public class JsonValidation {

	public final static int MAX_STRING_LENGTH = 10000;
	public final static int MAX_UNBOUND_STRING_LENGTH = 1024*1024*16;
	private final static int MAX_EMAIL_LENGTH = 254;
	
	/**
	 * checks provided json if all required fields are provided
	 * @param json the JsonNode to check
	 * @param requiredFields list of required fields
	 * @throws JsonValidationException if a required field is missing
	 */
	public static void validate(JsonNode json, String... requiredFields) throws JsonValidationException {
		if (json == null) {
			throw new JsonValidationException("error.missing.json", "No json found.");
		} else {
			for (String requiredField : requiredFields) {
				if (!json.has(requiredField) || (json.get(requiredField).isTextual() && json.get(requiredField).asText().isEmpty())) {
					throw new JsonValidationException("error.missing.input_field", requiredField, "required", "Request parameter '" + requiredField + "' not found.");
				}
			}
		}
	}
	
	/**
	 * returns a string field from the provided JSON
	 * @param json the JSON to access
	 * @param field the name of the field to return
	 * @return trimmed string or null if field does not exist
	 */
	public static String getString(JsonNode json, String field) throws JsonValidationException  {
		String res = json.path(field).asText();
		if (res != null) res = res.trim();
		if (res != null && res.length() > MAX_STRING_LENGTH) throw new JsonValidationException("error.toolong.field", "Request parameter '" + field + "' is too long.");
		return res;
	}
	
	public static String getUnboundString(JsonNode json, String field) throws JsonValidationException  {
		String res = json.path(field).asText();
		if (res != null) res = res.trim();
		if (res != null && res.length() > MAX_UNBOUND_STRING_LENGTH) throw new JsonValidationException("error.toolong.field", "Request parameter '" + field + "' is too long.");
		return res;
	}
		
	public static String getJsonString(JsonNode json, String field) throws JsonValidationException {
		JsonNode data = json.path(field);
		if (data.isTextual()) return data.asText().trim();
		if (data.isObject()) {
			return data.toString();
		}
		throw new JsonValidationException("error.missing.field", "Request parameter '" + field + "' does not contain JSON.");
	}
	
	public static String getStringOrNull(JsonNode json, String field) throws JsonValidationException {
		String res = json.path(field).asText();
		if (res != null) {
			res = res.trim();
			if (res.length() > MAX_STRING_LENGTH) throw new JsonValidationException("error.toolong.field", "Request parameter '" + field + "' is too long.");
		}
		if (res != null && res.length() == 0) return null;
		return res;
	}
	
	public static MidataId getMidataId(JsonNode json, String field) throws JsonValidationException {
		JsonNode n = json.path(field);		
		if (n.isObject() && n.has("$oid")) n = n.path("$oid");
		String id = n.asText();		
		if (id == null || id.trim().equals("") || id.equals("null")) return null;
		if (!MidataId.isValid(id)) throw new JsonValidationException("error.invalid.MidataId", field, "noMidataId", "MidataId expected.");
		return new MidataId(id);
	}
	
	public static int getInteger(JsonNode json, String field, int lowest, int highest) throws JsonValidationException {
		int val;
		if (json.path(field).isTextual()) {
			val = json.path(field).asInt(Integer.MIN_VALUE);
			if (val == Integer.MIN_VALUE) throw new JsonValidationException("error.invalid.integer", field, "nonumber", "Integer value expected.");
		} else
		if (! json.path(field).isInt()) throw new JsonValidationException("error.invalid.integer", field, "nonumber", "Integer value expected.");
		else val = json.path(field).intValue();
		if (val < lowest) throw new JsonValidationException("error.invalid.integer.toolow", field, "toolow", "Value must be " + lowest+" at minimum.");
		if (val > highest) throw new JsonValidationException("error.invalid.integer.toohigh", field, "toohigh", "Value may be " + lowest+" at maximum.");
		return val;
	}
	
	public static long getLong(JsonNode json, String field) throws JsonValidationException {
		if (! json.path(field).canConvertToLong()) throw new JsonValidationException("error.invalid.long", field, "nonumber", "Long value expected.");
		return json.path(field).longValue();		
	}
	
	public static boolean getBoolean(JsonNode json, String field) {
		return json.path(field).asBoolean();		
	}
	
	private final static Pattern NUMBER = Pattern.compile("[0-9]");
	private final static Pattern LC = Pattern.compile("[a-z]");
	private final static Pattern UC = Pattern.compile("[A-Z]");
	private final static Pattern OC = Pattern.compile("[^A-Za-z0-9]");
	
	public static String getPassword(JsonNode json, String field) throws JsonValidationException {
		String pw = json.path(field).asText();
		if (pw.length() < 8) throw new JsonValidationException("error.tooshort.password", field, "tooshort", "Password is too weak. It must be 8 characters at minimum.");
		if (pw.length() > MAX_STRING_LENGTH) throw new JsonValidationException("error.toolong.password", field, "toolong", "Password must be shorter than "+MAX_STRING_LENGTH);
		// Do not check for hashes
		if (pw.length() < 12) {
			if (!NUMBER.matcher(pw).find()) throw new JsonValidationException("error.tooweak.password", field, "tooweak", "Password is too weak. It must container numbers and a mix of upper/lowercase letters.");
			if (!LC.matcher(pw).find() && !UC.matcher(pw).find()) throw new JsonValidationException("error.tooweak.password", field, "tooweak", "Password is too weak. It must container numbers and a mix of upper/lowercase letters.");
		}
		//if (!UC.matcher(pw).find()) throw new JsonValidationException("error.tooweak.password", field, "tooweak", "Password is too weak. It must container numbers and a mix of upper/lowercase letters.");
		//if (!OC.matcher(pw).find()) throw new JsonValidationException("error.tooweak.password", field, "tooweak", "Password is too weak. It must container numbers and a mix of upper/lowercase letters.");
		return pw;
	}
	
	public static String getEMail(JsonNode json, String field) throws JsonValidationException {				
		String email = json.path(field).asText();
		if (email != null) email = email.trim();
		if (email == null || email.length() == 0) return null;
		if (email.length() > MAX_EMAIL_LENGTH) throw new JsonValidationException("error.toolong.email", field, "toolong", "E-Mail address is too long.");
		if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) throw new JsonValidationException("error.invalid.email", field, "noemail", "Please enter a valid email address.");
		return email;
	}
	
	public static boolean isEMail(String email) {
	  if (email==null || !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) return false;
	  return true;
	}	
		
	public static Date getDate(JsonNode json, String field) throws JsonValidationException {
		JsonNode dateNode = json.path(field);
		if (dateNode.isNumber()) {
			long dateLong = dateNode.asLong();
			if (dateLong > 0) return new Date(dateLong);
		}
		if (dateNode.isNull()) return null;
		String dateStr = dateNode.asText();		
		if (dateStr == null || dateStr.length() == 0) return null;		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
          Date result = formatter.parse(dateStr);
          return result;
		} catch (ParseException e) {
		  throw new JsonValidationException("error.invalid.date", "Date must have format year-month-day.");
		}
	}
	
	public static <T extends Enum<T>> T getEnum(JsonNode json, String field, Class<T> en) throws JsonValidationException {
		String val = json.path(field).asText().toUpperCase();		
		try {
		  T result = (T) Enum.valueOf(en, val);
		  return result;
		} catch (IllegalArgumentException e) {
		  throw new JsonValidationException("error.validation.enum", "Value of parameter '" + field + "' has none of the valid values.");		
		}
	}
	
	public static <T extends Enum<T>> EnumSet<T> getEnumSet(JsonNode json, String field, Class<T> en) throws JsonValidationException {
		JsonNode val = json.path(field);
		EnumSet<T> result = EnumSet.noneOf(en);
		try {
			for (JsonNode jsonNode : val) {
				result.add((T) Enum.valueOf(en, jsonNode.asText().toUpperCase()));
			}
					  
		    return result;
		} catch (IllegalArgumentException e) {
		  throw new JsonValidationException("error.validation.enum", "Value of parameter '" + field + "' has none of the valid values.");		
		}
	}
	

	public static class JsonValidationException extends BadRequestException {

		private static final long serialVersionUID = 1L;
		
		private String field;
		private String type;

		public JsonValidationException(String localeKey, String message) {
			super(localeKey, message);
		}
		
		public JsonValidationException(String localeKey, String field, String type, String message) {
			super(localeKey, message);
			this.field = field;
			this.type = type;
		}

		public String getField() {
			return field;
		}

		public String getType() {
			return type;
		}
		
		
	}

}
