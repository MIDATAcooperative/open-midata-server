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

package utils.db;

import java.io.InputStream;

import org.bson.BsonObjectId;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.result.UpdateResult;

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
		//AccessLog.log("store id = ", id.toString(), " filename=", filename);
		GridFSBucket fileSystem = GridFSBuckets.create(DBLayer.getFSDB(), FILE_STORAGE);		
		
		GridFSUploadOptions options = new GridFSUploadOptions()
                //.chunkSizeBytes(1024)                
                .metadata(new Document("contentType", contentType).append("filename", filename));

        fileSystem.uploadFromStream(new BsonObjectId(id.toObjectId()), id.toString()+"_"+index, file, options);
						
        return id;
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
		//if (contentType == null) contentType = gridfile.getContentType();
		
		String filename = null;
		if (meta != null) filename = meta.getString("filename");
		if (filename == null) 
			filename = gridfile.getFilename();
		
		return new FileData(fileSystem.openDownloadStream(fileid), filename, contentType);		
	}
	
	public static void delete(ObjectId fileId) {
		GridFSBucket fileSystem = GridFSBuckets.create(DBLayer.getFSDB(), FILE_STORAGE);
		System.out.println("delete id="+fileId.toString());
		fileSystem.delete(fileId);
	}
	
	public static void rename(ObjectId fileId, String filename) {
		//GridFSBucket fileSystem = GridFSBuckets.create(DBLayer.getFSDB(), FILE_STORAGE);
		System.out.println("rename id="+fileId.toString()+" to "+filename);
		//fileSystem.rename(fileId, filename);
		
		BasicDBObject query = new BasicDBObject();
		query.put("_id", fileId);
								
		BasicDBObject updateContent = new BasicDBObject();
		updateContent.put("metadata.filename", filename);
					
		BasicDBObject update = new BasicDBObject("$set", updateContent);	
		MongoCollection<Document> mc = DBLayer.getFSDB().getCollection("fs.files");
		System.out.println("query="+query.toJson());
		System.out.println("update="+update.toJson());
		
		UpdateResult res = mc.updateOne(query, update);
		System.out.println("out="+res.toString());
		
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
