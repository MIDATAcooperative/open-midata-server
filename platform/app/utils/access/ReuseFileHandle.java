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

import models.MidataId;
import utils.exceptions.BadRequestException;

public class ReuseFileHandle implements UpdateFileHandleSupport{

	private int idx;
	
	public ReuseFileHandle(int idx) {
		this.idx = idx;
	}

	public int getIdx() {
		return idx;
	}
	
	public EncryptedFileHandle toEncryptedFileHandle(DBRecord rec) throws BadRequestException {
		if (rec.meta.containsField("file") || idx>0) {
			MidataId fileId = MidataId.from(rec.meta.get(RecordManager.instance.getFileMetaName(idx)));
			if (fileId==null) throw new BadRequestException("error.not_exists_attachment", "Previous attachments does not exist");;
			byte[] key = (byte[]) rec.meta.get(RecordManager.instance.getFileMetaName(idx)+"-key");
			return new EncryptedFileHandle(fileId, key, 0);
		} 
		throw new BadRequestException("error.not_exists_attachment", "Previous attachments does not exist");
	}
		
}
