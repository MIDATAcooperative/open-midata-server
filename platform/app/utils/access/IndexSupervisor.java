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

import java.util.Optional;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import utils.ErrorReporter;
import utils.ServerTools;
import utils.access.index.IndexMsg;
import utils.access.index.TerminateMsg;
import utils.stats.ActionRecorder;

public class IndexSupervisor extends AbstractActor {
	
	@Override
	public Receive createReceive() {
	    return receiveBuilder()
	      .match(IndexMsg.class, this::indexMessage)
	      .match(TerminateMsg.class, this::terminateMessage)
	      .build();
	}
	
	public void indexMessage(IndexMsg msg) throws Exception {
		String path = "IndexSupervisor/indexMessage";
		long st = ActionRecorder.start(path);
		
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
			ActionRecorder.end(path, st);
		}
	}
	
	public void terminateMessage(TerminateMsg message) throws Exception {		
		getContext().stop(getSender());			
	}

}
