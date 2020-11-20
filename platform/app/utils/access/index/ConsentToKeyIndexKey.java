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

package utils.access.index;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import models.MidataId;

public class ConsentToKeyIndexKey extends BaseIndexKey<ConsentToKeyIndexKey,ConsentToKeyIndexKey> implements Comparable<ConsentToKeyIndexKey> {

	public MidataId aps;
	public byte[] key;
	
	public ConsentToKeyIndexKey() {}
	
	public ConsentToKeyIndexKey(MidataId aps) {
		this.aps = aps;
	}
	public ConsentToKeyIndexKey(MidataId aps, byte[] key) {
		this.aps = aps;
		this.key = key;
	}
	
	@Override
	public int compareTo(ConsentToKeyIndexKey arg0) {
		return aps.compareTo(arg0.aps);
	}

	@Override
	public ConsentToKeyIndexKey toMatch() {
		return this;
	}

	@Override
	public ConsentToKeyIndexKey copy() {
		ConsentToKeyIndexKey result = new ConsentToKeyIndexKey();
		result.aps = this.aps;
		result.key = this.key;
		return result;
	}

	@Override
	public void fetchValue(ConsentToKeyIndexKey otherKey) {
        this.key = otherKey.key;		
	}

	@Override
	public boolean matches(ConsentToKeyIndexKey match) {
		return this.aps.equals(match.aps);		
	}

	@Override
	public void writeObject(ObjectOutputStream s, ConsentToKeyIndexKey last) throws IOException {
		s.writeUTF(this.aps.toString());
		s.writeInt(this.key.length);
		s.write(this.key);
		
	}

	@Override
	public void readObject(ObjectInputStream s, ConsentToKeyIndexKey last) throws IOException, ClassNotFoundException {
        this.aps = MidataId.from(s.readUTF());
        this.key = new byte[s.readInt()];
        s.readFully(this.key);		
	}

}
