package controllers;

import java.util.Collection;
import java.util.Map;

import models.FormatInfo;
import models.ModelException;
import models.Record;
import models.enums.APSSecurityLevel;

import org.bson.types.ObjectId;
import play.Logger;

public class StreamLayouter {

	public static final StreamLayouter instance = new StreamLayouter();
	
	public void placeRecord(ObjectId who, ObjectId aps, Record record) throws ModelException {
		Logger.debug("placeRecord");
		if (record.format.equals(RecordSharing.STREAM_TYPE)) return;
		if (record.stream == null) {
			record.stream = RecordSharing.instance.getStreamByName(who, aps, record.format);
		}
		if (record.stream == null && record.format != null) {
			FormatInfo format = FormatInfo.getByName(record.format);
			Record stream = RecordSharing.instance.createStream(who, aps, record.format, format.security.equals(APSSecurityLevel.MEDIUM));
			record.stream = stream._id;
		}
	}
	
	public void adjustQuery(ObjectId who, ObjectId apsId, Map<String,Object> query) throws ModelException {
		Logger.debug("adjustQuery");
		 if (query.containsKey("format") && !query.containsKey("stream") && !query.containsKey("_id")) {
			 Object format = query.get("format");
			 if (format.equals(RecordSharing.STREAM_TYPE)) return;
			 if (format instanceof Collection) {
			   query.put("stream", RecordSharing.instance.getStreamsByName(who, apsId, (Collection) format));
			 } else if (format instanceof String) {
			   query.put("stream", RecordSharing.instance.getStreamByName(who, apsId, (String) format));
			 }
		 }
	}
}
