package utils.collections;

import java.util.HashSet;
import java.util.Set;

public class Sets {

	public static Set<String> create(String... contents) {
		HashSet<String> result = new HashSet<String>();
		for (String entry : contents) result.add(entry);
		return result;
	}
}
