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
import models.enums.APSSecurityLevel;

public class HistoryIndexKey extends BaseIndexKey<HistoryIndexKey,HistoryIndexKey> implements Comparable<HistoryIndexKey> {

    public long ts;	
	public boolean isDelete;
	public byte stream;
	public MidataId recordId;
	
	public HistoryIndexKey() {}
	
	public HistoryIndexKey(long ts, boolean isDelete, APSSecurityLevel stream, MidataId recordId) {
		this.ts = ts;
		this.isDelete = isDelete;
		setIsstream(stream);
		this.recordId = recordId;
	}
	
	public APSSecurityLevel getIsstream() {
		switch(stream) {
		case 0:return null;
		case 1:return APSSecurityLevel.MEDIUM;
		case 2:return APSSecurityLevel.HIGH;
		default:return null;
		}		
	}
	
	private void setIsstream(APSSecurityLevel isstream) {
		this.stream = (isstream != null) ? (isstream==APSSecurityLevel.MEDIUM ? (byte) 1 : (byte) 2) : (byte) 0;
	}
	 
	@Override
	public int compareTo(HistoryIndexKey other) {
		int b = Long.compare(ts, other.ts);
		if (b!=0) return b;
		b = recordId.compareTo(other.recordId);
		if (b!=0) return b;
		b = Boolean.compare(isDelete, other.isDelete);
		return b;
	}


	@Override
	public HistoryIndexKey toMatch() {
		return this;				
	}


	@Override
	public HistoryIndexKey copy() {
		HistoryIndexKey result = new HistoryIndexKey(ts, isDelete, getIsstream(), recordId);		
		return result;
	}


	@Override
	public void fetchValue(HistoryIndexKey otherKey) {
		this.ts = otherKey.ts;
		this.isDelete = otherKey.isDelete;
		this.stream = otherKey.stream;
		this.recordId = otherKey.recordId;	
	}


	@Override
	public boolean matches(HistoryIndexKey match) {
		return this.equals(match);
	}
	

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof HistoryIndexKey) {
			HistoryIndexKey other = (HistoryIndexKey) obj;			
			return ts == other.ts && recordId.equals(other.recordId) && isDelete == other.isDelete;
		}
		return false;
	}

	@Override
	public void writeObject(ObjectOutputStream s, HistoryIndexKey last) throws IOException {		
		s.writeLong(ts);
		s.writeBoolean(isDelete);
		s.writeByte(stream);
		s.writeUTF(recordId.toString());
	}

	@Override
	public void readObject(ObjectInputStream s, HistoryIndexKey last) throws IOException, ClassNotFoundException {			
		ts = s.readLong();
		isDelete = s.readBoolean();
		stream = s.readByte();
		recordId = MidataId.from(s.readUTF());
	}

	public long getTs() {
		return ts;
	}

	public boolean isDelete() {
		return isDelete;
	}

	public byte getStream() {
		return stream;
	}

	public MidataId getRecordId() {
		return recordId;
	}

	@Override
	public String toString() {
		return "{ ts="+ts+", rec="+recordId.toString()+", del="+isDelete+" }";
	}
	
	
	
	
	
}
