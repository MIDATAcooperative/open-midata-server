package models;

import org.bson.types.ObjectId;

public class MidataId implements Comparable<MidataId> {

	private String id;
	private ObjectId objId;
	
	public MidataId() {
       objId = new ObjectId();
       id = objId.toString();
	}
	
	public MidataId(String id) {
		if (id == null) throw new NullPointerException();
		if (id.startsWith("midata:/")) id = id.substring(8);
		this.id = id;
	}
	
	public MidataId(ObjectId objId) {
		if (objId == null) throw new NullPointerException();
		this.objId = objId;
		this.id = objId.toString();
	}
	
	public boolean isLocal() {
		return true;
	}
	
	public String toString() {
		return id;
	}
	
	public String toURI() {
		return id;
	}
	
	public ObjectId toObjectId() {
		if (objId!=null) return objId;
		objId = new ObjectId(id);
		return objId;
	}
	
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
	
	public byte[] toByteArray() {
		return id.getBytes();
	}
	
	public static boolean isValid(String str) {		
		if (ObjectId.isValid(str)) return true;
		return false;
	}
	
	public static MidataId from(Object o) {
		if (o == null) return null;
		if (o instanceof ObjectId) return new MidataId((ObjectId) o);
		return new MidataId(o.toString());
	}
	
	public static MidataId fromURI(String uri) {
		if (uri == null) return null;		
		return new MidataId(uri.substring(8)); // midata:/
	}
}
