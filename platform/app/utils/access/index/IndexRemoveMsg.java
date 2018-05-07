package utils.access.index;

import java.util.List;
import java.util.Set;

import models.MidataId;
import utils.access.DBRecord;
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
