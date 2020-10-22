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

import java.util.HashMap;
import java.util.Map;

public class ChainedMap<K, V> {

	private Map<K, V> map;

	public ChainedMap() {
		map = new HashMap<K, V>();
	}

	public ChainedMap<K, V> put(K key, V value) {
		map.put(key, value);
		return this;
	}

	public Map<K, V> get() {
		return map;
	}

}
