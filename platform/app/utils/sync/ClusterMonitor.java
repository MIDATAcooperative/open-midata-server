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

package utils.sync;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import utils.InstanceConfig;
import utils.messaging.Messager;
import utils.messaging.ServiceHandler;

public class ClusterMonitor extends AbstractActor {
	  
	  Cluster cluster = Cluster.get(getContext().getSystem());

	  //subscribe to cluster changes
	  @Override
	  public void preStart() {
	    cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(), 
	        MemberEvent.class, UnreachableMember.class);
	  }

	  //re-subscribe when restart
	  @Override
	  public void postStop() {
	    cluster.unsubscribe(getSelf());
	  }

	  @Override
	  public Receive createReceive() {
	    return receiveBuilder()
	      .match(MemberUp.class, mUp -> {
	    	  ServiceHandler.startup();
	    	  ServiceHandler.shareKey();
	    	  if (!cluster.selfAddress().equals(mUp.member().address())) {
	    	    Messager.sendTextMail(InstanceConfig.getInstance().getAdminEmail(), "Midata Platform", "Cluster member joined", mUp.member().address().toString());
	    	  }
	         
	      })
	      .match(UnreachableMember.class, mUnreachable -> {
	    	  if (!cluster.selfAddress().equals(mUnreachable.member().address())) {
	    	    Messager.sendTextMail(InstanceConfig.getInstance().getAdminEmail(), "Midata Platform", "Cluster member unreachable", mUnreachable.member().address().toString());
	    	  }
	      })
	      .match(MemberRemoved.class, mRemoved -> {
	    	  if (!cluster.selfAddress().equals(mRemoved.member().address())) {
	    	    Messager.sendTextMail(InstanceConfig.getInstance().getAdminEmail(), "Midata Platform", "Cluster member removed", mRemoved.member().address().toString());
	    	  }
	      })
	      .match(MemberEvent.class, message -> {

	      })
	      .build();
	  }
	}