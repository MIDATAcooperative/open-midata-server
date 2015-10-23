package models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.bson.types.ObjectId;

import utils.access.RecordSharing;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


/**
 * Custom implementation of GridFS for large records which exceed the document size limit of 16 MB. Allows to
 * efficiently retrieve specific fields from within a large JSON record by generating a master record that divides the
 * data into chunks and keeps track of which keys are stored in which chunk.
 * <p>
 * <p>
 * The data field of the master record contains the following information:
 * <li>type
 * <li>numChunks
 * <li>chunks
 * <p>
 * where the type is "largeRecord" and the chunks object contains a list with the chunk id and the key of the head
 * element for each chunk.
 */
public class LargeRecord  {

	private static final int numItemsPerChunk = 16000;

	/**
	 * Gets all records and substitutes the data from chunks for the records that are large records.
	 */
	public static List<Record> getAll(ObjectId who, ObjectId aps, Map<String, Object> properties, Set<String> fields) throws AppException {
		Set<String> tempFields = new HashSet<String>(fields);
		tempFields.add("data.type");
		List<Record> records = RecordSharing.instance.list(who, aps, properties, tempFields);
		if (fields.contains("data")) {
		for (Record record : records) {
			// get the data from the chunks for large records
			if (record.data.containsField("type") && record.data.get("type").equals("largeRecord")) {
				// get chunks as well (do this here to not impact normal records)
				//Map<String, ObjectId> recordProperties = new ChainedMap<String, ObjectId>().put("_id", record._id).get();
				//Set<String> chunksField = new ChainedSet<String>().add("data.chunks").get();
				//record.data.put("chunks", Record.get(recordProperties, chunksField).data.get("chunks"));
				replaceData(who, aps, record, fields);
			}
		}
		}
		return records;
	}

	/**
	 * Substitutes the data from the chunk for the base record's data.
	 */
	private static void replaceData(ObjectId who, ObjectId aps, Record masterRecord, Set<String> fields) throws AppException {
		// extract only the fields concerning the data, and strip the "data." qualifier
		Set<String> dataFields = new HashSet<String>();
		for (String field : fields) {
			if (field.startsWith("data.")) {
				dataFields.add(field.substring(5));
			}
		}

		// get ids chunks
		Set<ObjectId> chunksToFetch = getChunkIds(masterRecord, dataFields);
		List<Record> chunksData = RecordSharing.instance.list(who, aps, CMaps.map("_id", chunksToFetch), fields);

		// replace data in master record
		masterRecord.data = new BasicDBObject();
		for (Record chunkData : chunksData) {
			for (String dataField : dataFields) {
				if (chunkData.data.containsField(dataField)) {
					masterRecord.data.put(dataField, chunkData.data.get(dataField));
				}
			}
		}
	}

	/**
	 * Get the ids of the chunks that contain the data requested by 'dataFields'.
	 */
	private static Set<ObjectId> getChunkIds(Record masterRecord, Set<String> dataFields) {
		// create an array of strings from the head keys, and a map from keys to ids to retrieve them later
		BasicDBList chunks = (BasicDBList) masterRecord.data.get("chunks");
		String[] headKeys = new String[chunks.size()];
		Map<String, ObjectId> keysToIds = new HashMap<String, ObjectId>();
		int index = 0;
		for (Object chunk : chunks) {
			DBObject chunkInfo = (DBObject) chunk;
			ObjectId chunkId = (ObjectId) chunkInfo.get("_id");
			String chunkHead = (String) chunkInfo.get("head");
			headKeys[index++] = chunkHead;
			keysToIds.put(chunkHead, chunkId);
		}

		// determine the chunks with the relevant data
		Set<ObjectId> chunksWithData = new HashSet<ObjectId>();
		for (String dataField : dataFields) {
			int searchIndex = Arrays.binarySearch(headKeys, dataField);
			// if field is a head element, index is non-negative and already correct
			if (searchIndex == -1) {
				// field is before first head element: not found
				continue;
			} else if (searchIndex < -1) {
				// otherwise, search index is (- "insertion point" - 1): convert to correct bucket
				searchIndex = (searchIndex + 2) * -1; // we want to compute ("insertion point" - 1)
			}
			chunksWithData.add(keysToIds.get(headKeys[searchIndex]));
		}
		return chunksWithData;
	}

	/*public static void set(Record masterRecord, String field, Object value) throws InternalServerException {
		Set<ObjectId> chunkIds = getChunkIds(masterRecord, new ChainedSet<String>().add("field").get());
		if (chunkIds.size() == 1) {
			ObjectId chunkId = chunkIds.iterator().next();
			LargeRecordChunk.set(chunkId, field, value);
		}
	}*/

	/**
	 * Creates chunks from the data and saves the index information in the master record.
	 */
	public static void add(Member owner, Record masterRecord, TreeMap<String, Object> data) throws AppException {
		// initialize data and set the type to 'largeRecord'
		masterRecord.data = new BasicDBObject();
		masterRecord.data.put("type", "largeRecord");

		// determine the number of splits based on the number of (top-level) items in the data object
		int numChunks = (int) Math.ceil((double) data.size() / numItemsPerChunk);
		masterRecord.data.put("numChunks", numChunks);
        List<Record> recs = new ArrayList<Record>();
		// create the chunks
		BasicDBList chunks = new BasicDBList();
		Iterator<String> iterator = data.keySet().iterator();
		for (int i = 1; i <= numChunks; i++) {
			Record chunk = new Record();
			chunk._id = new ObjectId();
			chunk.document = masterRecord._id;
			chunk.format = "Chunk";
			chunk.app = masterRecord.app;
			chunk.created = masterRecord.created;
			chunk.creator = masterRecord.creator;
			chunk.owner = masterRecord.owner;
			
			chunk.name = masterRecord.name+" [Part "+i+"]";
			chunk.data = new BasicDBObject();
			 

			// fill in the data
			int itemsInThisChunk = 0;
			String headKey = null;
			while (iterator.hasNext() && itemsInThisChunk < numItemsPerChunk) {
				String key = iterator.next();
				chunk.data.put(key, data.get(key));
				itemsInThisChunk++;

				// save the head key
				if (headKey == null) {
					headKey = key;
				}
			}

			chunk.part = headKey;
			// insert chunk into database
			recs.add(chunk);

			// add chunk information to chunks list
			DBObject chunkInfo = new BasicDBObject();
			chunkInfo.put("_id", chunk._id);
			chunkInfo.put("head", headKey);
			chunks.add(chunkInfo);
		}
		masterRecord.data.put("chunks", chunks);

		// insert the master record as a normal record
		RecordSharing.instance.addDocumentRecord(owner._id, masterRecord, recs);
	}

	/**
	 * Deletes all chunks of the given master record.
	 */
	public static void delete(ObjectId masterRecordId) throws InternalServerException {
		//LargeRecordChunk.deleteAll(masterRecordId);
	}
}
