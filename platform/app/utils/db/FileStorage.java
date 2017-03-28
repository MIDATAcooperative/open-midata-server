package utils.db;

import java.io.InputStream;

import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

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
	public static void store(InputStream file, MidataId id, String filename, String contentType) throws DatabaseException {
		AccessLog.log("store id = "+id+" filename="+filename);
		GridFS fileSystem = new GridFS(DBLayer.getFSDB(), FILE_STORAGE);
		GridFSInputFile inputFile;
		
		inputFile = fileSystem.createFile(file, filename);		
		inputFile.setId(id.toObjectId());
		inputFile.setFilename(filename);
		inputFile.setContentType(contentType);
		inputFile.save();
	}

	/**
	 * Returns an input stream from which the file can be read.
	 */
	public static FileData retrieve(MidataId id) {
		GridFS fileSystem = new GridFS(DBLayer.getFSDB(), FILE_STORAGE);
		GridFSDBFile retrievedFile = fileSystem.findOne(id.toObjectId());
		if (retrievedFile != null) return new FileData(retrievedFile.getInputStream(), retrievedFile.getFilename(), retrievedFile.getContentType());
		return null;
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
