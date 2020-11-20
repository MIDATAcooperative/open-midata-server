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

import java.util.Set;

import models.MidataId;
import utils.access.IndexPseudonym;

public class IndexUpdateMsg extends IndexMsg {
		
	
    private final Set<MidataId> aps;
        
    
    public IndexUpdateMsg(MidataId indexId, MidataId executor, IndexPseudonym pseudo, String handle, Set<MidataId> aps) {    	
    	super(indexId, executor, pseudo, handle);
    	this.aps = aps;
    }	
	
	public Set<MidataId> getAps() {
		return aps;
	}
    
	public String toString() {
		return "Index Update: #aps="+(aps==null?"null":aps.size())+" "+super.toString();
	}
    
}
