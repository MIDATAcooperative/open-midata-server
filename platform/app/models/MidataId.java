package models;

import java.io.Serializable;
import java.util.Date;

import org.bson.types.ObjectId;

/**
 * id for any resource stored in MIDATA
 *
 */
public class MidataId implements Comparable<MidataId>, Serializable {

	private static final long serialVersionUID = 5402993258205599040L;
	
	private String id;
	private ObjectId objId;
	
	/**
	 * Creates new unique ID 
	 */
	public MidataId() {
       objId = new ObjectId();
       id = objId.toString();
	}
	
	/**
	 * Creates ID from string representation
	 * @param id
	 */
	public MidataId(String id) {
		if (id == null) throw new NullPointerException();
		if (id.startsWith("midata:/")) id = id.substring(8);
		this.id = id;
	}
	
	/**
	 * Creates MidataID from ObjectId
	 * @param objId _id of object stored in local database
	 */
	public MidataId(ObjectId objId) {
		if (objId == null) throw new NullPointerException();
		this.objId = objId;
		this.id = objId.toString();
	}
	
	/**
	 * Is this ID for an object stored locally?
	 * @return true if ID is from a local object
	 */
	public boolean isLocal() {
		return true;
	}
	
	/**
	 * Returns ID as string representation
	 */
	public String toString() {
		return id;
	}
	
	/**
	 * Returns ID as URI representation
	 * @return ID as URI
	 */
	public String toURI() {
		return id;
	}
	
	/**
	 * Returns ID as mongoDB ObjectId for local IDs
	 * @return
	 */
	public ObjectId toObjectId() {
		if (objId!=null) return objId;
		objId = new ObjectId(id);
		return objId;
	}
	
	public Date getCreationDate() {
		return toObjectId().getDate();
	}
	
	/**
	 * Returns representation suitable for storing in the database
	 * @return returns String or ObjectId
	 */
	public Object toDb() {
		if (isLocal()) return toObjectId(); else return toString();
	}
		

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof MidataId) {
		  return id.equals(((MidataId) arg0).id);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public int compareTo(MidataId arg0) {		
		return id.compareTo(((MidataId) arg0).id); 			
	}
	
	/**
	 * convert ID to byte array
	 * @return byte array representing id
	 */
	public byte[] toByteArray() {
		return id.getBytes();
	}
	
	/**
	 * tests if a string is a valid midata ID
	 * @param str string to test
	 * @return true if str is a valid midata ID
	 */
	public static boolean isValid(String str) {		
		if (ObjectId.isValid(str)) return true;
		return false;
	}
	
	/**
	 * contruct a MidataID from an object
	 * @param o an object that may be a mongoDB ObjectId or a String
	 * @return MidataId
	 */
	public static MidataId from(Object o) {
		if (o == null) return null;
		if (o instanceof ObjectId) return new MidataId((ObjectId) o);
		if (o instanceof MidataId) return (MidataId) o;
		return new MidataId(o.toString());
	}
		
}
