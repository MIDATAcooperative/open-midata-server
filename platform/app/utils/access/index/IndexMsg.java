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
