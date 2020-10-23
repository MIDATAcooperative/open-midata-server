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

package utils.access;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BasicBSONObject;

import models.MidataId;
import models.enums.APSSecurityLevel;
import utils.auth.EncryptionNotSupportedException;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

/**
 * abstract interface for an access permission set
 *
 */
public abstract class APS extends Feature {

	public final static String QUERY = "_query";
	
	public abstract MidataId getId();
	
	public abstract boolean isReady() throws AppException;
	
	public abstract boolean isAccessible() throws AppException;
	
	public abstract boolean isUsable() throws AppException;
	
	public abstract void touch() throws AppException;
	
	public abstract long getLastChanged() throws AppException;
		
	public abstract APSSecurityLevel getSecurityLevel() throws InternalServerException;
	
	public abstract void provideRecordKey(DBRecord record) throws AppException;
	
	public abstract void addAccess(Set<MidataId> targets) throws AppException,EncryptionNotSupportedException;

	public abstract void addAccess(MidataId target, byte[] publickey) throws AppException,EncryptionNotSupportedException;
	
	public abstract void removeAccess(Set<MidataId> targets) throws InternalServerException;
	
	public abstract boolean hasAccess(MidataId target) throws InternalServerException;
	
	public abstract Set<MidataId> getAccess() throws AppException;		
	
	public abstract void setMeta(String key, Map<String, Object> data) throws AppException;
	
	public abstract void removeMeta(String key) throws AppException;
	
	public abstract MidataId getStoredOwner() throws AppException;
		
	public abstract BasicBSONObject getMeta(String key) throws AppException;
				
							
	public abstract void addPermission(DBRecord record, boolean withOwner) throws AppException;
		
	public abstract void addPermission(Collection<DBRecord> records, boolean withOwner) throws AppException;
		
			
	public abstract boolean removePermission(DBRecord record) throws AppException;
		
	public abstract void removePermission(Collection<DBRecord> records) throws AppException;

	public abstract void clearPermissions() throws AppException;
	
	public abstract List<DBRecord> historyQuery(long minUpd, boolean removes) throws AppException;

	public abstract boolean hasNoDirectEntries() throws AppException;
	
	public abstract void provideAPSKeyAndOwner(byte[] unlock, MidataId owner);
					
}
