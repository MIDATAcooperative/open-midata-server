package utils.json;

import java.io.IOException;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.mongodb.util.JSON;

public class ObjectIdSerializer extends JsonSerializer<ObjectId> {

	@Override
	public void serialize(ObjectId id, JsonGenerator generator, SerializerProvider provider) throws IOException,
			JsonProcessingException {
	    generator.writeStartObject();
		generator.writeFieldName("$oid");
		generator.writeString(id.toString());
		generator.writeEndObject();
	}

}
