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

package models;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.bson.BSONObject;

import com.fasterxml.jackson.annotation.JsonFilter;

import utils.collections.Sets;
import utils.db.NotMaterialized;

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
			"app", "creator", "created", "name", "format", "content", "code", "description", "data", "group");

	public @NotMaterialized final static Set<String> ALL_PUBLIC_WITHNAMES = Sets.create("_id", "id", "version", "owner", "ownerName",
			"app", "creator", "creatorName", "created", "name", "format", "content", "code", "description", "data", "group");

		
	
	public MidataId stream;
				
	
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
	public MidataId owner; 
	
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
	public  MidataId app; 
	
	/**
	 * the id of the user that created this record
	 * 
	 * The contents of this field is stored in the encrypted part of this record.
	 * If the creator is the same as the owner the creator is not stored 	
	 */
	public  MidataId creator; 
	
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
	
	
	//public Set<MidataId> dependencies;

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
	
	public void addTag(String tag) {
		if (this.tags == null) this.tags = new HashSet<String>();
		this.tags.add(tag);
	}
	
	public String getErrorInfo() {
		return "Record('"+format+"','"+content+"')";
	}

}
