package utils.json;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class CustomObjectMapper extends ObjectMapper {

	private static final long serialVersionUID = 1L;
	
	public static ObjectMapper me;

	public CustomObjectMapper() {
		me = this;
		SimpleModule module = new SimpleModule("ObjectIdModule");
		module.addSerializer(ObjectId.class, new ObjectIdSerializer());
		this.registerModule(module);
		setSerializationInclusion(Include.NON_NULL);
	}
	
	public CustomObjectMapper(CustomObjectMapper old) {
		super(old);
		
	}

	@Override
	public ObjectMapper copy() {		
		return new CustomObjectMapper(this);
	}

	
}