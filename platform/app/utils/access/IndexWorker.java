package utils.access;

import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import models.MidataId;
import scala.concurrent.duration.Duration;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.InstanceConfig;
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
import utils.messaging.MailUtils;
import utils.messaging.Message;

public class IndexWorker extends UntypedActor {
	
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
		
		getContext().setReceiveTimeout(Duration.create("30 seconds"));
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
	public void onReceive(Object message) throws Exception {
		try {
			AccessLog.logBegin("START INDEX UPDATE:");
			AccessLog.log("in:"+message.toString());
			if (message instanceof IndexMsg) {
				this.handle = ((IndexMsg) message).getHandle();
			
				if (!((IndexMsg) message).getExecutor().equals(executor)) throw new InternalServerException("error.internal", "Wrong executor for index update:"+executor.toString()+" vs "+((IndexMsg) message).getExecutor());
				KeyManager.instance.continueSession(handle, executor);
				if (cache == null) cache = RecordManager.instance.getCache(executor);			
				if (idx == null) idx = IndexManager.instance.findIndex(pseudo, indexId);
				if (root == null) root = new IndexRoot(pseudo.getKey(), idx, false);	
			}
			if (message instanceof IndexUpdateMsg) {
			  IndexUpdateMsg msg = (IndexUpdateMsg) message;
			  IndexManager.instance.indexUpdate(cache, root, executor, msg.getAps());									
				
			} else if (message instanceof IndexRemoveMsg) {
			  IndexRemoveMsg msg = (IndexRemoveMsg) message;
			  
			  IndexManager.instance.removeRecords(cache, executor, msg.getRecords(), msg.getIndexId(), msg.getCondition());
			} else if (message instanceof ReceiveTimeout) {
			  getContext().parent().tell(new TerminateMsg(indexId.toString()), getSelf());
			} else {
			    unhandled(message);
		    }	
		} catch (Exception e) {
			ErrorReporter.report("Messager", null, e);	
			throw e;
		} finally {
			AccessLog.logEnd("END INDEX UPDATE");
			ServerTools.endRequest();			
		}
	}
}
