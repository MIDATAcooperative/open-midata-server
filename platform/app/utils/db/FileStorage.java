package utils.db;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.bson.types.ObjectId;

import utils.access.AccessLog;

import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

public class FileStorage {

	private static final String FILE_STORAGE = "fs";

	/**
	 * Stores an input file with GridFS, which automatically divides a file into chunks of 265 kB.
	 */
	public static void store(InputStream file, ObjectId id, String filename, String contentType) throws DatabaseException {
		AccessLog.debug("store id = "+id+" filename="+filename);
		GridFS fileSystem = new GridFS(DBLayer.getFSDB(), FILE_STORAGE);
		GridFSInputFile inputFile;
		
		inputFile = fileSystem.createFile(file, filename);		
		inputFile.setId(id);
		inputFile.setFilename(filename);
		inputFile.setContentType(contentType);
		inputFile.save();
	}

	/**
	 * Returns an input stream from which the file can be read.
	 */
	public static FileData retrieve(ObjectId id) {
		GridFS fileSystem = new GridFS(DBLayer.getFSDB(), FILE_STORAGE);
		GridFSDBFile retrievedFile = fileSystem.findOne(id);
		if (retrievedFile != null) return new FileData(retrievedFile.getInputStream(), retrievedFile.getFilename());
		return null;
	}

	/**
	 * Inner class used for passing back the filename along with the input stream when retrieving a file.
	 */
	public static class FileData {

		public InputStream inputStream;
		public String filename;

		public FileData(InputStream inputStream, String filename) {
			this.inputStream = inputStream;
			this.filename = filename;
		}
	}

}
