package utils.access;

import java.util.Comparator;

import org.bson.BSONObject;

public class RecordComparator implements Comparator<DBRecord> {

	private String[] path;
	private String last;
	private boolean data;
	private int direction = 1;
	
	public RecordComparator(String fullPath) {
		if (fullPath.endsWith(" asc")) {
			direction = 1;
			fullPath = fullPath.substring(0, fullPath.length() - 4);
		} else if (fullPath.endsWith(" desc")) {
			direction = -1;
			fullPath = fullPath.substring(0, fullPath.length() - 5);
		}
		if (fullPath.startsWith("data.")) {
			data = true;
			String p[] = fullPath.split("\\.");
			if (p.length > 2) {
				path = new String[p.length-2];
				for (int i=0;i<p.length-2;i++) path[i] = p[i+1];
			}
			last = p[p.length-1];
		} else {
			String p[] = fullPath.split("\\.");
			if (p.length > 1) {
				path = new String[p.length-1];
				for (int i=0;i<p.length-1;i++) path[i] = p[i];
			}
			last = p[p.length-1];
		}
	}
	
	@Override
	public int compare(DBRecord o1, DBRecord o2) {
		BSONObject obj1;
		BSONObject obj2;
		
		if (data) {
			obj1 = o1.data;
			obj2 = o2.data;
		} else {
			obj1 = o1.meta;
			obj2 = o2.meta;
		}
		
		if (path != null) {
			for (String part : path) {
				obj1 = (BSONObject) obj1.get(part);
				obj2 = (BSONObject) obj2.get(part);
			}
		}
		
		Object o = obj1.get(last);
		if (o == null) {
			return obj2.get(last) == null ? 0 : -direction;
		}
		return direction * ((Comparable) o).compareTo(obj2.get(last));		
	}

}
