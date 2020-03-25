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
