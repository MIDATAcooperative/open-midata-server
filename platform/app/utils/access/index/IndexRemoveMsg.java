package utils.access.index;

import java.util.List;
import java.util.Set;

import models.MidataId;
import utils.access.DBRecord;
import utils.access.IndexPseudonym;

public class IndexRemoveMsg extends IndexMsg {	
    private final List<DBRecord> records;
    
    public IndexRemoveMsg(MidataId indexId, MidataId executor, IndexPseudonym pseudo, String handle, List<DBRecord> records) {
    	super(indexId, executor, pseudo, handle);
    	this.records = records;
    }	
	
	public List<DBRecord> getRecords() {
		return records;
	}

	
    
}
