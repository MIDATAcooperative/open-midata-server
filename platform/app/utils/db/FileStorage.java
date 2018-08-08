package utils.db;

import java.io.InputStream;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;

import models.MidataId;
import utils.AccessLog;

/**
 * Access to GridFS for records with attached file data
 *
 */
public class FileStorage {

	private static final String FILE_STORAGE = "fs";

	/**
	 * Stores an input file with GridFS, which automatically divides a file into chunks of 265 kB.
	 */
	public static MidataId store(InputStream file, MidataId id, int index, String filename, String contentType) throws DatabaseException {
		AccessLog.log("store id = "+id+" filename="+filename);
		GridFSBucket fileSystem = GridFSBuckets.create(DBLayer.getFSDB(), FILE_STORAGE);		
		
		GridFSUploadOptions options = new GridFSUploadOptions()
                //.chunkSizeBytes(1024)                
                .metadata(new Document("contentType", contentType).append("filename", filename));

        return MidataId.from(fileSystem.uploadFromStream(id.toString()+"_"+index, file, options));
						
	}

	/**
	 * Returns an input stream from which the file can be read.
	 */
	public static FileData retrieve(MidataId id, int index) {
		GridFSBucket fileSystem = GridFSBuckets.create(DBLayer.getFSDB(), FILE_STORAGE);
				
		GridFSFindIterable retrievedFile = fileSystem.find(new BasicDBObject("filename", id.toString()+"_"+index));
		GridFSFile gridfile = retrievedFile.first();
		if (gridfile == null) {
			retrievedFile = fileSystem.find(new BasicDBObject("_id", id.toObjectId()));
			gridfile = retrievedFile.first();
		}
		if (gridfile == null) return null;
		
		ObjectId fileid = gridfile.getObjectId();
		
		Document meta = gridfile.getMetadata();
		String contentType = null;		
		if (meta != null) contentType = meta.getString("contentType");
		if (contentType == null) contentType = gridfile.getContentType();
		
		String filename = null;
		if (meta != null) filename = meta.getString("filename");
		if (filename == null) filename = gridfile.getFilename();
				
		return new FileData(fileSystem.openDownloadStream(fileid), filename, contentType);		
	}
	
	public static void delete(ObjectId fileId) {
		GridFSBucket fileSystem = GridFSBuckets.create(DBLayer.getFSDB(), FILE_STORAGE);
		System.out.println("delete id="+fileId.toString());
		fileSystem.delete(fileId);
	}

	/**
	 * Inner class used for passing back the filename along with the input stream when retrieving a file.
	 */
	public static class FileData {

		public InputStream inputStream;
		public String filename;
		public String contentType;

		public FileData(InputStream inputStream, String filename, String contentType) {
			this.inputStream = inputStream;
			this.filename = filename;
			this.contentType = contentType;
		}
	}

}
