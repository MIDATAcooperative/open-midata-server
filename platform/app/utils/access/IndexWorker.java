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
import utils.access.index.BaseIndexRoot;
import utils.access.index.IndexDefinition;
import utils.access.index.IndexMsg;
import utils.access.index.IndexRemoveMsg;
import utils.access.index.IndexRoot;
import utils.access.index.IndexUpdateMsg;
import utils.access.index.StatsIndexRoot;
import utils.access.index.StreamIndexRoot;
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
	private BaseIndexRoot root;
	private long lastFullUpdate = 0;
	
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
			AccessLog.logStart("index", message.toString());
			AccessLog.logBegin("START INDEX UPDATE:");			
			
				this.handle = ((IndexMsg) message).getHandle();
			
				if (!((IndexMsg) message).getExecutor().equals(executor)) throw new InternalServerException("error.internal", "Wrong executor for index update:"+executor.toString()+" vs "+((IndexMsg) message).getExecutor());
				KeyManager.instance.continueSession(handle, executor);
				if (cache == null) cache = RecordManager.instance.getCache(executor);			
				if (idx == null) idx = IndexManager.instance.findIndex(pseudo, indexId);
				if (root == null) {
					if (idx.formats.contains("_streamIndex")) {
					   root = new StreamIndexRoot(pseudo.getKey(), idx, false);
					} else if (idx.formats.contains("_statsIndex")) {
					   root = new StatsIndexRoot(pseudo.getKey(), idx, false);
					} else {
					   root = new IndexRoot(pseudo.getKey(), idx, false);
					}
				}
			
			  IndexUpdateMsg msg = (IndexUpdateMsg) message;
			  if (msg.getAps() != null || System.currentTimeMillis() > lastFullUpdate + 1000l* 60l) {
				  IndexManager.instance.indexUpdate(cache, root, executor, msg.getAps());	
				  if (msg.getAps()==null) lastFullUpdate = System.currentTimeMillis();
			  }
							
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
			AccessLog.logStart("index", message.toString());
			AccessLog.logBegin("START INDEX UPDATE:");			
			
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
