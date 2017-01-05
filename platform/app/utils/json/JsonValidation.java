package utils.json;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import models.MidataId;
import utils.exceptions.BadRequestException;

/**
 * functions for validating JSON input
 *
 */
public class JsonValidation {

	public static void validate(JsonNode json, String... requiredFields) throws JsonValidationException {
		if (json == null) {
			throw new JsonValidationException("error.missing.json", "No json found.");
		} else {
			for (String requiredField : requiredFields) {
				if (!json.has(requiredField)) {
					throw new JsonValidationException("error.missing.field", requiredField, "required", "Request parameter '" + requiredField + "' not found.");
				}
			}
		}
	}
	
	public static String getString(JsonNode json, String field) {
		String res = json.path(field).asText();
		if (res != null) res = res.trim();
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
	
	public static String getStringOrNull(JsonNode json, String field) {
		String res = json.path(field).asText();
		if (res != null) res = res.trim();
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
		if (! json.path(field).isInt()) throw new JsonValidationException("error.invalid.integer", field, "nonumber", "Integer value expected.");
		int val = json.path(field).intValue();
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
	
	public static String getPassword(JsonNode json, String field) throws JsonValidationException {
		String pw = json.path(field).asText();
		if (pw.length() < 8) throw new JsonValidationException("error.invalid.password.tooshort", field, "tooshort", "Password is too weak. It must be 8 characters at minimum.");
		if (!NUMBER.matcher(pw).find()) throw new JsonValidationException("error.invalid.password.weak", field, "tooweak", "Password is too weak. It must container numbers and a mix of upper/lowercase letters.");
		if (!LC.matcher(pw).find()) throw new JsonValidationException("error.invalid.password.weak", field, "tooweak", "Password is too weak. It must container numbers and a mix of upper/lowercase letters.");
		if (!UC.matcher(pw).find()) throw new JsonValidationException("error.invalid.password.weak", field, "tooweak", "Password is too weak. It must container numbers and a mix of upper/lowercase letters.");
		return pw;
	}
	
	public static String getEMail(JsonNode json, String field) throws JsonValidationException {
		String email = json.path(field).asText();
		if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) throw new JsonValidationException("error.invalid.email", field, "noemail", "Please enter a valid email address.");
		return email;
	}
	
	public static Date getDate(JsonNode json, String field) throws JsonValidationException {
		JsonNode dateNode = json.path(field);
		if (dateNode.isNumber()) {
			long dateLong = dateNode.asLong();
			if (dateLong > 0) return new Date(dateLong);
		}
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
