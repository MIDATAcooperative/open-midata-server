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

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import models.MidataId;

/**
 * JSON mapper with MidataId support
 *
 */
public class CustomObjectMapper extends ObjectMapper {

	private static final long serialVersionUID = 1L;
	
	public static ObjectMapper me;

	public CustomObjectMapper() {
		me = this;
		SimpleModule module = new SimpleModule("MidataIdModule");
		module.addSerializer(MidataId.class, new MidataIdSerializer());
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

class MidataIdSerializer extends JsonSerializer<MidataId> {

	@Override
	public void serialize(MidataId id, JsonGenerator generator, SerializerProvider provider) throws IOException,
			JsonProcessingException {
	    /*generator.writeStartObject();
		generator.writeFieldName("$oid");
		generator.writeString(id.toString());
		generator.writeEndObject();*/
		
		generator.writeString(id.toURI());
	}

}