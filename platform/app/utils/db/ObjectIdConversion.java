package utils.db;

import java.util.HashSet;
import java.util.Set;

import models.MidataId;

/**
 * Helper functions for converting object ids to string and back
 *
 */
public class ObjectIdConversion {

	public static Set<MidataId> toMidataIds(Set<String> strings) {
		Set<MidataId> set = new HashSet<MidataId>();
		for (String s : strings) {
			set.add(new MidataId(s));
		}
		return set;
	}

	public static Set<MidataId> castToMidataIds(Set<Object> objects) {
		Set<MidataId> set = new HashSet<MidataId>();
		for (Object o : objects) {
			set.add((MidataId) o);
		}
		return set;
	}

	public static Set<String> toStrings(Set<MidataId> MidataIds) {
		Set<String> set = new HashSet<String>();
		for (MidataId oid : MidataIds) {
			set.add(oid.toString());
		}
		return set;
	}

}
