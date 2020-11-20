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

package utils.access;

import java.util.Base64;

import com.mongodb.MongoGridFSException;

import models.MidataId;
import utils.AccessLog;
import utils.auth.KeyManager;
import utils.auth.TokenCrypto;
import utils.db.FileStorage;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.InternalServerException;

public class EncryptedFileHandle {

	private MidataId id;
	private byte[] key;
	private long length;
	
	EncryptedFileHandle(MidataId id, byte[] key, long length) {
		this.id = id;
		this.key = key;
		this.length = length;
	}

	public MidataId getId() {
		return id;
	}

	protected byte[] getKey() {
		return key;
	}

	public long getLength() {
		return length;
	}
	
	public void removeAfterFailure() {
		System.out.println("REMOVE AFTER FAILURE");
		try {
		  FileStorage.delete(id.toObjectId());
		} catch (MongoGridFSException e) {}
	}
	
	public void rename(String filename) {
		AccessLog.log("Rename to: "+filename);
		try {
			FileStorage.rename(id.toObjectId(), filename);
		} catch (MongoGridFSException e) {}
	}
	
	private final static String protokoll = "midata-file://";
	
	public String serializeAsURL(MidataId owner) throws InternalServerException {
		byte[] keydata = KeyManager.instance.encryptKey(owner, key);
		return protokoll+TokenCrypto.encryptToken(id.toString()+"-"+Base64.getEncoder().encodeToString(keydata)+"-"+length);
	}
	
	public static EncryptedFileHandle fromString(MidataId owner, String input) throws AppException {
		if (!input.startsWith(protokoll)) return null;
		input = input.substring(protokoll.length());
		input = TokenCrypto.decryptToken(input);
		if (input==null) return null;
		String parts[] = input.split("-");
		if (parts.length != 3) return null;
		MidataId id = MidataId.from(parts[0]);
		byte[] enckey = Base64.getDecoder().decode(parts[1]);
		byte[] key = KeyManager.instance.decryptKey(owner, enckey);
		return new EncryptedFileHandle(id, key, Long.parseLong(parts[2]));
	}
	
}
