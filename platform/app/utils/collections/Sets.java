package utils.collections;

import java.util.HashSet;
import java.util.Set;

public class Sets {

	public static Set<String> create(String... contents) {
		HashSet<String> result = new HashSet<String>();
		for (String entry : contents) result.add(entry);
		return result;
	}
	
	@SafeVarargs
	public static <T extends Enum> Set<T> createEnum(T... contents) {
		HashSet<T> result = new HashSet<T>();
		for (T entry : contents) result.add(entry);
		return result;
	}
}
