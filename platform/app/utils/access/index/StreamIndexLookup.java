package utils.access.index;

import java.util.Set;

import models.MidataId;
import utils.AccessLog;

public class StreamIndexLookup extends BaseLookup<StreamIndexKey>{

	private MidataId id;
	private Set<String> format;
	private Set<String> content;
	private Set<MidataId> owner;
	private Set<MidataId> app;
	private boolean onlyStreams;
	
	public StreamIndexLookup() {	
	}
	
		
	public MidataId getId() {
		return id;
	}


	public void setId(MidataId id) {
		this.id = id;
	}


	public Set<String> getFormat() {
		return format;
	}


	public void setFormat(Set<String> format) {
		this.format = format;
	}


	public Set<String> getContent() {
		return content;
	}


	public void setContent(Set<String> content) {
		this.content = content;
	}


	public Set<MidataId> getApp() {
		return app;
	}


	public void setApp(Set<MidataId> app) {
		this.app = app;
	}
	

	public Set<MidataId> getOwner() {
		return owner;
	}


	public void setOwner(Set<MidataId> owner) {
		this.owner = owner;
	}


	public boolean isOnlyStreams() {
		return onlyStreams;
	}


	public void setOnlyStreams(boolean onlyStreams) {
		this.onlyStreams = onlyStreams;
	}


	@Override
	public boolean conditionCompare(StreamIndexKey inkey) {		
         if (id != null && !id.equals(inkey.getId())) return false;
         if (format != null && !format.contains(inkey.getFormat())) return false;
         if (content != null && !content.contains(inkey.getContent())) return false;
         if (app != null && inkey.getApp() !=null && !app.contains(inkey.getApp())) return false;
         if (owner != null && !owner.contains(inkey.getOwner())) return false;
         if (onlyStreams && inkey.getIsstream()==null) return false;
		return true;
	}

	@Override
	public boolean conditionCompare(StreamIndexKey lk, StreamIndexKey hk) {
		if (id != null) {
			if ((lk==null || id.compareTo(lk.getId()) >= 0) && (hk==null || id.compareTo(hk.getId())<=0)) return true;
		}
		return true;
	}


	@Override
	public String toString() {
		return "stream-lookup({"
	           +(id!=null?(" id:"+id):"")
	           +(format!=null?(" format:"+format.toString()):"")
	           +(content!=null?(" content:"+content.toString()):"")
	           +(owner!=null?(" owner:"+owner.toString()):"")
	           +(app!=null?(" app:"+app.toString()):"")
	           +(onlyStreams?" streams":"")	           
	           +"})";		
	}
	
	

}
