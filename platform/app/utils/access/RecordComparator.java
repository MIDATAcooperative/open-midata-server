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

package utils.access;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

public class RecordComparator implements Comparator<DBRecord> {

	private Path path;
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
			fullPath = fullPath.substring("data.".length());
		}
		String alts[] = fullPath.split("\\|");
		
		if (alts.length == 1) {
			String p[] = alts[0].split("\\.");
			path = new SimplePath(p);
		} else {
			List<SimplePath> lst = new ArrayList<SimplePath>(alts.length);
			for (String alt : alts) {
				String p[] = alt.split("\\.");
				lst.add(new SimplePath(p));
			}
			path = new AlternativePath(lst.toArray(new SimplePath[alts.length]));
		}
		
	}
	
	@Override
	public int compare(DBRecord o1, DBRecord o2) {
		Object obj1;
		Object obj2;
		
		if (data) {
			obj1 = o1.data;
			obj2 = o2.data;
		} else {
			obj1 = o1.meta;
			obj2 = o2.meta;
		}
		
		if (path != null) {			
			obj1 = path.access((BSONObject) obj1);
			obj2 = path.access((BSONObject) obj2);			
		}
				
		if (obj1 == null) {
			return obj2 == null ? 0 : -direction;
		}
		if (obj2 == null) return direction;
		return direction * ((Comparable) obj1).compareTo(obj2);		
	}

    interface Path {
    	Object access(BSONObject in);
    }

	class SimplePath implements Path {
		private String[] path;
		
		SimplePath(String[] path) {
			this.path = path;
		}
		
		public Object access(BSONObject p) {
			Object in = p;
			for (String part : path) {				
				in = ((BSONObject) in).get(part);
				if (in instanceof BasicBSONList) in = ((BasicBSONList) in).get(0);
				if (in == null) return null;
			}
			return in;
		}
	}
	
	class AlternativePath implements Path {
		private SimplePath[] alt;
		
		AlternativePath(SimplePath[] alt) {
			this.alt = alt;
		}
		
		public Object access(BSONObject in) {
			for (SimplePath part : alt) {
				Object r = part.access(in);				
				if (r != null) return r;
			}
			return null;
		}
		
	}
}
