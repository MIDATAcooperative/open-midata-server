package models;

import org.bson.types.ObjectId;

import utils.auth.RecordToken;

public class RecordMetadata implements Comparable<RecordMetadata> {

	public ObjectId _id;
	public String id;
	public ObjectId owner; // person the record is about
	public String ownerName;
	
	public ObjectId app; // app that created the record	
	public ObjectId creator; // user that imported the record
	public String created; // date + time created
	public String name; // used to display a record and for autocompletion
	public String format; // format of record
	public String description; // this will be indexed in the search cluster	
	
	@Override
	public int compareTo(RecordMetadata other) {
		if (this.created != null && other.created != null) {
			// newest first
			return -this.created.compareTo(other.created);
		} else {
			return this._id.compareTo(other._id);
		}
	}
}
