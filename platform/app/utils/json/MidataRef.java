package utils.json;

import org.bson.types.ObjectId;

public class MidataRef extends ObjectId {
	private int server;

	public MidataRef(int server, ObjectId target) {
		super(target.toByteArray());
		this.server = server;
	}
	
	public MidataRef(int server, String target) {
		super(target);
		this.server = server;
	}
		
	
	public MidataRef() {		
		super();
		server = 0;
	}
	
	public static MidataRef parseString(String in) {
		return new MidataRef(0, in);
	}
	
	public ObjectId getObjectId() {
		return this;
	}

	public int getServer() {
		return server;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o instanceof MidataRef) {
		   return ((MidataRef) o).getObjectId().equals(this) && ((MidataRef) o).getServer() == server;
		}
		return false;
	}

	@Override
	public int hashCode() {		
		return server + super.hashCode();
	}

	@Override
	public String toString() {
		return getServer() != 0 ? getServer() + "/" + super.toString() : super.toString();
	}
	
		
}
