package utils.json;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bson.types.ObjectId;

import models.enums.Gender;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonValidation {

	public static void validate(JsonNode json, String... requiredFields) throws JsonValidationException {
		if (json == null) {
			throw new JsonValidationException("No json found.");
		} else {
			for (String requiredField : requiredFields) {
				if (!json.has(requiredField)) {
					throw new JsonValidationException("Request parameter '" + requiredField + "' not found.");
				}
			}
		}
	}
	
	public static String getString(JsonNode json, String field) {
		return json.path(field).asText();
	}
	
	public static ObjectId getObjectId(JsonNode json, String field) {
		return new ObjectId(json.path(field).asText());
	}
	
	public static int getInteger(JsonNode json, String field, int lowest, int highest) throws JsonValidationException {
		if (! json.path(field).isInt()) throw new JsonValidationException(field, "nonumber", "Integer value expected.");
		int val = json.path(field).intValue();
		if (val < lowest) throw new JsonValidationException(field, "toolow", "Value must be " + lowest+" at minimum.");
		if (val > highest) throw new JsonValidationException(field, "toohigh", "Value may be " + lowest+" at maximum.");
		return val;
	}
	
	public static boolean getBoolean(JsonNode json, String field) {
		return json.path(field).asBoolean();		
	}
	
	public static String getPassword(JsonNode json, String field) throws JsonValidationException {
		String pw = json.path(field).asText();
		if (pw.length() < 5) throw new JsonValidationException("Password is too weak. It must be 5 characters at minimum.");
		return pw;
	}
	
	public static String getEMail(JsonNode json, String field) {
		String email = json.path(field).asText();
		//TODO Validation
		
		return email;
	}
	
	public static Date getDate(JsonNode json, String field) throws JsonValidationException {
		String dateStr = json.path(field).asText();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
          Date result = formatter.parse(dateStr);
          return result;
		} catch (ParseException e) {
		  throw new JsonValidationException("Date must have format year-month-day.");
		}
	}
	
	public static <T extends Enum> T getEnum(JsonNode json, String field, Class<T> en) throws JsonValidationException {
		String val = json.path(field).asText();
		try {
		  T result = (T) Enum.valueOf(en, val);
		  return result;
		} catch (IllegalArgumentException e) {
		  throw new JsonValidationException("Value of parameter '" + field + "' has none of the valid values.");		
		}
	}
	

	public static class JsonValidationException extends Exception {

		private static final long serialVersionUID = 1L;
		
		private String field;
		private String type;

		public JsonValidationException(String message) {
			super(message);
		}
		
		public JsonValidationException(String field, String type, String message) {
			super(message);
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
