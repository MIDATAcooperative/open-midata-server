package models;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonFilter;

import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;
import utils.search.Search;

/**
 * data model for a record
 *
 */
@JsonFilter("Record")
public class Record extends Model implements Comparable<Record>, Cloneable {

	private static final String collection = "records";
	
	/**
	 * constant containing the set of all public field names
	 */
	public @NotMaterialized final static Set<String> ALL_PUBLIC = Sets.create("_id", "id", "version", "owner",
			"app", "creator", "created", "name", "format", "subformat", "content", "code", "description", "data", "group");

	
	/**
	 * the id of the document record this record belongs to.
	 * 
	 * This field is null for all records that are not part of a document
	 * This field is not encrypted but stored in the database.
	 * 
	 */
	public ObjectId document;
	
	public ObjectId stream;
	
	/**
	 * a part name for records that are part of a document,
	 * 
	 * This field is null for all records that are not part of a document
	 * This field is not encrypted but stored in the database
	 */
	public String part;
		
	
	/**
	 * an alternative ID for this record that has the access permission set ID also encoded in the ID.
	 * 
	 * This field is computed upon request and is neither stored in the database nor contained in the encrypted part
	 */
	public String id;
	
	/**
	 * version of this document
	 */
	public String version;
	
	/**
	 * the id of the owner of this record. 
	 * 
	 * The owner is the person the data of this record belongs to.
	 * This field is neither stored in the database nor contained in the encrypted part of the record.
	 * The owner information is derived from information of the APS this record is stored in or 
	 * from the owner of the stream this record is stored in.
	 */
	public ObjectId owner; 
	
	/**
	 * firstname lastname of the owner of this record.
	 * 
	 * This field is neither stored in the database nor contained in the encrypted part of the record.
	 * This field is computed upon request from the owner field
	 */
	public String ownerName;
	
	/**
	 * firstname lastname of the creator of this record
	 * 
	 * This field is neither stored in the database nor contained in the encrypted part of the record.
	 * This field is computed upon request from the creator field
	 */
	public String creatorName;
			
	/**
	 * The format of the records data.
	 * 
	 * This is the syntactical data format.
	 * This field is contained in the encrypted part of the record.
	 */
	public String format;
	
	/**
	 * (Optional) subformat of record
	 * This field is contained in the encrypted part of the record.
	 */
	public String subformat;
	
	/**
	 * The content type of the records data.
	 * 
	 * This is the semantical data format.
	 * This field is contained in the encrypted part of the record. 
	 */
	public String content;
	
	/**
	 * The content system+code of the records data.
	 * 
	 * This is the semantical data format expressed as a code system + code pair.
	 * This field is contained in the encrypted part of the record. 
	 */
	public Set<String> code;
	
	/**
	 * The group name where the record will be placed in the record tree
	 * 
	 * This field is neither stored in the database nor contained in the encrypted part of the record.
	 */
	public String group;
	
	/**
	 * Is this record a stream record?
	 * 
	 * This field is neither stored in the database nor contained in the encrypted part of the record.
	 * This field is only internally used.
	 */
	public boolean isStream;
	
	/**
	 * the id of the plugin that created this record
	 * 
	 * The contents of this field is stored in the encrypted part of this record
	 */
	public  ObjectId app; 
	
	/**
	 * the id of the user that created this record
	 * 
	 * The contents of this field is stored in the encrypted part of this record.
	 * If the creator is the same as the owner the creator is not stored 	
	 */
	public  ObjectId creator; 
	
	/**
	 * record creation timestamp
	 * 
	 * The contents of this field is stored in the encrypted part of this record
	 */
	public  Date created;
	
	/**
	 * record update timestamp
	 * 
	 * The contents of this field is stored in the encrypted part of this record
	 */
	public  Date lastUpdated;  
	
	/**
	 * the title for this record
	 * 
	 * The contents of this field is stored in the encrypted part of this record
	 */
	public  String name; 
	
	/**
	 * textual description of this record
	 * 
	 * The contents of this field is stored in the encrypted part of this record
	 */
	public  String description; 
	
	/**
	 * optional set of tags describing the record
	 * 
	 * The contents of this field is stored in the encrypted part of this record
	 */
	public  Set<String> tags; 
	
	/**
	 * the record data. 
	 * 
	 * May be any JSON data.
	 * The contents of this field is encrypted into encryptedData field and not directly stored in the database.
	 */
	public BSONObject data; 

	@Override
	public int compareTo(Record other) {
		if (this.created != null && other.created != null) {
			// newest first
			return -this.created.compareTo(other.created);
		} else if (this.created == null && other.created == null ){
			return super.compareTo(other);
		} else if (this.created == null) return 1;
		else return -1;
	}
	
	@Override
	public Record clone() {	
		try {
			return (Record) super.clone();
		} catch (CloneNotSupportedException e) {			
			e.printStackTrace();
			return null;
		}
	}
	
	

}
