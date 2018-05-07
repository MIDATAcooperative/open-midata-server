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
