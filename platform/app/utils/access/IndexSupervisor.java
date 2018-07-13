package utils.access;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import utils.ErrorReporter;
import utils.ServerTools;
import utils.access.index.IndexMsg;
import utils.access.index.TerminateMsg;

public class IndexSupervisor extends AbstractActor {
	
	@Override
	public Receive createReceive() {
	    return receiveBuilder()
	      .match(IndexMsg.class, this::indexMessage)
	      .match(TerminateMsg.class, this::terminateMessage)
	      .build();
	}
	
	public void indexMessage(IndexMsg msg) throws Exception {
		try {						
			
			  ActorRef ref = getContext().getChild(msg.getIndexId().toString());
				
			  if (ref == null) {
				ref = getContext().actorOf(Props.create(IndexWorker.class, msg.getExecutor(), msg.getPseudo(), msg.getIndexId(), msg.getHandle()), msg.getIndexId().toString());  
			  }
			  
			  ref.forward(msg, getContext());			
		} catch (Exception e) {
			ErrorReporter.report("Messager", null, e);	
			throw e;
		} finally {
			ServerTools.endRequest();			
		}
	}
	
	public void terminateMessage(TerminateMsg message) throws Exception {		
		getContext().stop(getSender());			
	}

}
