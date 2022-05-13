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
import java.util.Date;

import org.bson.types.ObjectId;

import models.MidataId;
import models.enums.APSSecurityLevel;
import utils.access.DBRecord;

public class StreamIndexKey extends BaseIndexKey<StreamIndexKey,DBRecord> implements Comparable<StreamIndexKey> {

	private MidataId id;
	private String format;
	private String content;
	private MidataId app;
	private MidataId aps;
	private MidataId owner;
	private long created;
	private boolean readonly;
	private byte[] key;
	private byte isstream;	
	
	public MidataId getId() {
		return id;
	}

	public String getFormat() {
		return format;
	}

	public String getContent() {
		return content;
	}

	public MidataId getApp() {
		return app;
	}

	public MidataId getAps() {
		return aps;
	}

	public byte[] getKey() {
		return key;
	}
	
	
	public long getCreated() {
		return created;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public APSSecurityLevel getIsstream() {
		switch(isstream) {
		case 0:return null;
		case 1:return APSSecurityLevel.MEDIUM;
		case 2:return APSSecurityLevel.HIGH;
		default:return null;
		}		
	}
	
	private void setIsstream(APSSecurityLevel isstream) {
		this.isstream = (isstream != null) ? (isstream==APSSecurityLevel.MEDIUM ? (byte) 1 : (byte) 2) : (byte) 0;
	}

	public MidataId getOwner() {
		return owner;
	}
	
	
	

	public StreamIndexKey() {		
	}
	
	public StreamIndexKey(MidataId aps, DBRecord r) {
		this.id = r._id;
		this.aps = aps;
		this.owner = r.owner;
		this.format = r.meta.getString("format");
		this.content = r.meta.getString("content");
		this.app = MidataId.from(r.meta.get("app"));
		setIsstream(r.isStream);
		this.key = r.key;
		this.readonly = r.isReadOnly;		
		if (r.isStream==null) {
			this.created = ((Date) r.meta.get("created")).getTime();
		}
	}
	
	public DBRecord toDBRecord() {
		DBRecord result = new DBRecord();
		result._id = this.id;
		result.consentAps = this.aps;
		result.isStream = getIsstream();
		result.isReadOnly = readonly;
		result.key = this.key;
		result.owner = this.owner;
		result.meta.put("format", this.format);
		result.meta.put("content", this.content);
		result.security = APSSecurityLevel.HIGH;
		if (this.created != 0) result.meta.put("created", new Date(this.created));
		if (this.app != null) result.meta.put("app", new ObjectId(app.toString()));
		return result;
	}
	
	@Override
	public int compareTo(StreamIndexKey o) {
		int b = format.compareTo(o.format);
		if (b!=0) return b;
		b = id.compareTo(o.id);
		if (b!=0) return b;
		b = aps.compareTo(o.aps);
		return b;
				
	}
	
	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof StreamIndexKey) {
			StreamIndexKey other = (StreamIndexKey) arg0;			
			return id.equals(other.id) && aps.equals(other.aps) && format.equals(other.format) && content.equals(other.content) && app.equals(other.app);
		}
		return false;
	}
	
	
	
	@Override
	public int hashCode() {
		return id.hashCode() + format.hashCode() + content.hashCode() + owner.hashCode() + aps.hashCode() + owner.hashCode() + (app != null ? app.hashCode() : 0);
	}

	@Override
	public DBRecord toMatch() {
		return toDBRecord();
	}
	
	@Override
	public StreamIndexKey copy() {
		StreamIndexKey result = new StreamIndexKey();
		result.id = this.id;
		result.format = this.format;
		result.content = this.content;
		result.app = this.app;
		result.aps = this.aps;
		result.key = this.key;
		result.owner = this.owner;
		result.isstream = this.isstream;
		result.created = this.created;
		result.readonly = this.readonly;		
		return result;
	}
	
	@Override
	public void fetchValue(StreamIndexKey otherKey) {
		this.key = otherKey.key;
		this.isstream = otherKey.isstream;
		this.readonly = otherKey.readonly;
		this.created = otherKey.created;
	}
	
	@Override
	public boolean matches(DBRecord match) {
		return this.id.equals(match._id) && this.aps.equals(match.consentAps);
	}
	
	@Override
	public void writeObject(ObjectOutputStream s, StreamIndexKey last) throws IOException {
		int flags = 0;
		if (last!=null) {
			if (!format.equals(last.format)) flags |= 2 << 1;
			if (!content.equals(last.content)) flags |= 2 << 2;
			if (app==null || !app.equals(last.app)) flags |= 2 << 3;
			if (!aps.equals(last.aps)) flags |= 2 << 4;
			if (!owner.equals(last.owner)) flags |= 2 << 5;
		} else flags = 255;
		s.writeByte(flags);
		s.writeUTF(id.toString());
		if ((flags & 2<<1)>0) s.writeUTF(format);
		if ((flags & 2<<2)>0) s.writeUTF(content);
		if ((flags & 2<<3)>0) s.writeUTF(app==null?"":app.toString());
		if ((flags & 2<<4)>0) s.writeUTF(aps.toString());
		if ((flags & 2<<5)>0) s.writeUTF(owner.toString());
		s.writeInt(key.length);
		s.write(key);
		s.writeByte(this.isstream);	
		s.writeBoolean(this.readonly);
		s.writeLong(this.created);
	}
	
	@Override
	public void readObject(ObjectInputStream s, StreamIndexKey last) throws IOException, ClassNotFoundException {
		int flags = s.readByte();
		id = new MidataId(s.readUTF());
		if ((flags & 2<<1)>0) format = s.readUTF(); else format = last.format;
		if ((flags & 2<<2)>0) content = s.readUTF(); else content = last.content;
		if ((flags & 2<<3)>0) {
			String appstr = s.readUTF();
			app = appstr.length() > 0 ? new MidataId(appstr) : null; 
		} else app = last.app;
		if ((flags & 2<<4)>0) aps = new MidataId(s.readUTF()); else aps = last.aps;
		if ((flags & 2<<5)>0) owner = new MidataId(s.readUTF()); else owner = last.owner;
		int l = s.readInt();
		key = new byte[l];
		s.read(key);
		isstream = s.readByte();
		readonly = s.readBoolean();
		created = s.readLong();
	}
		
	
}
