package utils.access;

import java.util.Optional;

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
			
			  Optional<ActorRef> ref = getContext().findChild(msg.getIndexId().toString());
				
			  if (!ref.isPresent()) {
				ref = Optional.of(getContext().actorOf(Props.create(IndexWorker.class, msg.getExecutor(), msg.getPseudo(), msg.getIndexId(), msg.getHandle()).withDispatcher("slow-work-dispatcher"), msg.getIndexId().toString()));  
			  }
			  
			  ref.get().forward(msg, getContext());			
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
