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

import java.util.List;

import models.MidataId;
import utils.access.IndexPseudonym;
import utils.access.op.Condition;

public class IndexRemoveMsg extends IndexMsg {	
    private final List<IndexMatch> records;
    private final Condition[] condition;    
    
    public IndexRemoveMsg(MidataId indexId, MidataId executor, IndexPseudonym pseudo, String handle, List<IndexMatch> records, Condition[] condition) {
    	super(indexId, executor, pseudo, handle);
    	this.records = records;
    	this.condition = condition;
    }	
	
	public List<IndexMatch> getRecords() {
		return records;
	}

	public Condition[] getCondition() {
		return condition;
	}
	
	public String toString() {
		return "Index Remove #records="+records.size()+" "+super.toString();
	}
	    
}
