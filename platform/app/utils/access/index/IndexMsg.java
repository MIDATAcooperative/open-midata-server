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

package utils.access.index;

import java.io.Serializable;

import models.MidataId;
import utils.access.IndexPseudonym;

public class IndexMsg implements Serializable {
	
	private static final long serialVersionUID = 4129300859725080477L;
	private final MidataId indexId;
    private final MidataId executor;
    private final IndexPseudonym pseudo;
    private final String handle;   
    
    public IndexMsg(MidataId indexId, MidataId executor, IndexPseudonym pseudo, String handle) {
    	this.indexId = indexId;
    	this.executor = executor;
    	this.pseudo = pseudo;
    	this.handle = handle;
    }

	public MidataId getIndexId() {
		return indexId;
	}

	public MidataId getExecutor() {
		return executor;
	}
	

	public String getHandle() {
		return handle;
	}

	public IndexPseudonym getPseudo() {
		return pseudo;
	}
	
	public String toString() {
		return "index="+indexId.toString()+" exec="+executor.toString();
	}
    
	
    
}
