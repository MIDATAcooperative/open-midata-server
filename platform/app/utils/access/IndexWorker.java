package utils.access;

import java.time.Duration;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.japi.Creator;
import models.MidataId;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.ServerTools;
import utils.access.index.IndexDefinition;
import utils.access.index.IndexMsg;
import utils.access.index.IndexRemoveMsg;
import utils.access.index.IndexRoot;
import utils.access.index.IndexUpdateMsg;
import utils.access.index.TerminateMsg;
import utils.auth.KeyManager;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;

public class IndexWorker extends AbstractActor {
	
	private MidataId executor;
	
	private MidataId indexId;
	private String handle;
	
	private IndexPseudonym pseudo;
	private APSCache cache;
	private IndexDefinition idx;
	private IndexRoot root;
	
	public IndexWorker(MidataId executor, IndexPseudonym pseudo, MidataId indexId, String handle) {
		this.executor = executor;
		this.pseudo = pseudo;
		this.indexId = indexId;
		this.handle = handle;
		
		getContext().setReceiveTimeout(Duration.ofSeconds(30));
	}
	
	  public static Props props(final MidataId executor, final IndexPseudonym pseudo, final MidataId indexId, final String handle) {
		    return Props.create(new Creator<IndexWorker>() {
		      private static final long serialVersionUID = 1L;
		 
		      @Override
		      public IndexWorker create() throws Exception {
		        return new IndexWorker(executor, pseudo, indexId, handle);
		      }
		    });
		  }
	
	@Override
	public void preStart() throws AppException {		
		
	}
	
	@Override
	public Receive createReceive() {
	    return receiveBuilder()
	      .match(IndexUpdateMsg.class, this::indexUpdate)
	      .match(IndexRemoveMsg.class, this::indexRemove)
	      .match(ReceiveTimeout.class, this::receiveTimeout)
	      .build();
	}
	
		
	public void indexUpdate(IndexUpdateMsg message) throws Exception {
		try {
			AccessLog.logBegin("START INDEX UPDATE:");
			AccessLog.log("in:"+message.toString());
			
				this.handle = ((IndexMsg) message).getHandle();
			
				if (!((IndexMsg) message).getExecutor().equals(executor)) throw new InternalServerException("error.internal", "Wrong executor for index update:"+executor.toString()+" vs "+((IndexMsg) message).getExecutor());
				KeyManager.instance.continueSession(handle, executor);
				if (cache == null) cache = RecordManager.instance.getCache(executor);			
				if (idx == null) idx = IndexManager.instance.findIndex(pseudo, indexId);
				if (root == null) root = new IndexRoot(pseudo.getKey(), idx, false);	
			
			  IndexUpdateMsg msg = (IndexUpdateMsg) message;
			  IndexManager.instance.indexUpdate(cache, root, executor, msg.getAps());									
							
		} catch (Exception e) {
			ErrorReporter.report("Messager", null, e);	
			throw e;
		} finally {
			AccessLog.logEnd("END INDEX UPDATE");
			ServerTools.endRequest();			
		}
	}
		
	public void indexRemove(IndexRemoveMsg message) throws Exception {
		try {
			AccessLog.logBegin("START INDEX UPDATE:");
			AccessLog.log("in:"+message.toString());
			
				this.handle = ((IndexMsg) message).getHandle();
			
				if (!((IndexMsg) message).getExecutor().equals(executor)) throw new InternalServerException("error.internal", "Wrong executor for index update:"+executor.toString()+" vs "+((IndexMsg) message).getExecutor());
				KeyManager.instance.continueSession(handle, executor);
				if (cache == null) cache = RecordManager.instance.getCache(executor);			
				if (idx == null) idx = IndexManager.instance.findIndex(pseudo, indexId);
				if (root == null) root = new IndexRoot(pseudo.getKey(), idx, false);	
			
			  IndexRemoveMsg msg = (IndexRemoveMsg) message;
			  
			  IndexManager.instance.removeRecords(cache, executor, msg.getRecords(), msg.getIndexId(), msg.getCondition(), pseudo);
				
		} catch (Exception e) {
			ErrorReporter.report("Messager", null, e);	
			throw e;
		} finally {
			AccessLog.logEnd("END INDEX UPDATE");
			ServerTools.endRequest();			
		}
	}
	

	public void receiveTimeout(ReceiveTimeout message) throws Exception {	
		getContext().parent().tell(new TerminateMsg(indexId.toString()), getSelf());	
	}
}
