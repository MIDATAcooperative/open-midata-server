package utils.access;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import utils.ErrorReporter;
import utils.ServerTools;
import utils.access.index.IndexMsg;
import utils.access.index.TerminateMsg;

public class IndexSupervisor extends UntypedActor {
	
	@Override
	public void onReceive(Object message) throws Exception {
		try {						
			if (message instanceof IndexMsg) {
			  IndexMsg msg = (IndexMsg) message;
			  ActorRef ref = getContext().getChild(msg.getIndexId().toString());
				
			  if (ref == null) {
				ref = getContext().actorOf(Props.create(IndexWorker.class, msg.getExecutor(), msg.getPseudo(), msg.getIndexId(), msg.getHandle()), msg.getIndexId().toString());  
			  }
			  
			  ref.forward(message, getContext());
			} else if (message instanceof TerminateMsg) {
				getContext().stop(getSender());
			} else {
			    unhandled(message);
		    }	
		} catch (Exception e) {
			ErrorReporter.report("Messager", null, e);	
			throw e;
		} finally {
			ServerTools.endRequest();			
		}
	}

}
