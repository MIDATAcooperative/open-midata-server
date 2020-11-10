/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.collections;

import java.util.HashSet;
import java.util.Set;

public class Sets {

	public static Set<String> create(String... contents) {
		HashSet<String> result = new HashSet<String>();
		for (String entry : contents) result.add(entry);
		return result;
	}
	
	public static Set<Object> create(Object... contents) {
		HashSet<Object> result = new HashSet<Object>();
		for (Object entry : contents) result.add(entry);
		return result;
	}
	
	public static Set<String> create(Set<String> base, String... contents) {
		HashSet<String> result = new HashSet<String>();
		result.addAll(base);
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
