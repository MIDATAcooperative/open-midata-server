package utils.json;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;

import utils.exceptions.BadRequestException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * functions for validating JSON input
 *
 */
public class JsonValidation {

	public static void validate(JsonNode json, String... requiredFields) throws JsonValidationException {
		if (json == null) {
			throw new JsonValidationException("error.validation.nojson", "No json found.");
		} else {
			for (String requiredField : requiredFields) {
				if (!json.has(requiredField)) {
					throw new JsonValidationException("error.validation.fieldmissing", "Request parameter '" + requiredField + "' not found.");
				}
			}
		}
	}
	
	public static String getString(JsonNode json, String field) {
		String res = json.path(field).asText();
		if (res != null) res = res.trim();
		return res;
	}
	
	public static String getStringOrNull(JsonNode json, String field) {
		String res = json.path(field).asText();
		if (res != null) res = res.trim();
		if (res != null && res.length() == 0) return null;
		return res;
	}
	
	public static ObjectId getObjectId(JsonNode json, String field) throws JsonValidationException {
		String id = json.path(field).asText();
		if (id == null || id.trim().equals("") || id.equals("null")) return null;
		if (!ObjectId.isValid(id)) throw new JsonValidationException("error.validation.objectid", field, "noobjectid", "ObjectID expected.");
		return new ObjectId(id);
	}
	
	public static int getInteger(JsonNode json, String field, int lowest, int highest) throws JsonValidationException {
		if (! json.path(field).isInt()) throw new JsonValidationException("error.validation.integer", field, "nonumber", "Integer value expected.");
		int val = json.path(field).intValue();
		if (val < lowest) throw new JsonValidationException("error.validation.integer.toolow", field, "toolow", "Value must be " + lowest+" at minimum.");
		if (val > highest) throw new JsonValidationException("error.validation.integer.toohigh", field, "toohigh", "Value may be " + lowest+" at maximum.");
		return val;
	}
	
	public static long getLong(JsonNode json, String field) throws JsonValidationException {
		if (! json.path(field).canConvertToLong()) throw new JsonValidationException("error.validation.long", field, "nonumber", "Long value expected.");
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
		if (pw.length() < 8) throw new JsonValidationException("error.validation.password.tooshort", field, "tooshort", "Password is too weak. It must be 8 characters at minimum.");
		if (!NUMBER.matcher(pw).find()) throw new JsonValidationException("error.validation.password.weak", field, "tooweak", "Password is too weak. It must container numbers and a mix of upper/lowercase letters.");
		if (!LC.matcher(pw).find()) throw new JsonValidationException("error.validation.password.weak", field, "tooweak", "Password is too weak. It must container numbers and a mix of upper/lowercase letters.");
		if (!UC.matcher(pw).find()) throw new JsonValidationException("error.validation.password.weak", field, "tooweak", "Password is too weak. It must container numbers and a mix of upper/lowercase letters.");
		return pw;
	}
	
	public static String getEMail(JsonNode json, String field) throws JsonValidationException {
		String email = json.path(field).asText();
		if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) throw new JsonValidationException("error.validation.email", field, "noemail", "Please enter a valid email address.");
		return email;
	}
	
	public static Date getDate(JsonNode json, String field) throws JsonValidationException {
		String dateStr = json.path(field).asText();
		if (dateStr == null || dateStr.length() == 0) return null;
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
          Date result = formatter.parse(dateStr);
          return result;
		} catch (ParseException e) {
		  throw new JsonValidationException("error.validation.date", "Date must have format year-month-day.");
		}
	}
	
	public static <T extends Enum> T getEnum(JsonNode json, String field, Class<T> en) throws JsonValidationException {
		String val = json.path(field).asText();
		try {
		  T result = (T) Enum.valueOf(en, val);
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
