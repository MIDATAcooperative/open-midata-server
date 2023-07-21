package utils.plugins;

import java.util.concurrent.CompletionStage;

import org.apache.commons.lang3.tuple.Pair;

import akka.actor.ActorRef;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.stream.SourceRef;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamRefs;
import akka.util.ByteString;
import play.libs.ws.DefaultBodyWritables;
import play.libs.ws.SourceBodyWritable;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import utils.AccessLog;

public abstract class AbstractExternContainer extends AbstractContainer {

    protected WSClient ws;	
	
	public AbstractExternContainer() {
		super();
		this.ws = DeploymentManager.getWsClient();
	}
	
	protected String serviceUrl;
	
	public void process(String pluginName, String command, String repo, DeployAction action, DeployPhase next) {
		 AccessLog.log("Execute command "+command.toString());
		 		 
		 final ActorRef sender = getSender();
	    AccessLog.log(serviceUrl);
	    //Http.get(getContext().getSystem()).singleRequest(HttpRequest.POST(serviceUrl+"/?action="+command+"&name="+pluginName)., null)
		WSRequest holder = ws.url(serviceUrl);
		holder = holder.addQueryParameter("action", command).addQueryParameter("name", pluginName);
		if (repo != null) holder = holder.addQueryParameter("repository",repo);
		CompletionStage<WSResponse> promise = holder.get();//setContentType("application/x-www-form-urlencoded; charset=utf-8").post(post);
		promise.thenAccept(response -> {							
			final String body = response.getBody();
			final int status = response.getStatus();
			
			System.out.println("BODY="+body+" status="+status);
			result(sender, action, next, Pair.of(status == 200, body));					
	    });
	}
	
	public void processWithResult(String pluginName, String command, String repo, DeployAction action, DeployPhase next) {
		 AccessLog.log("Execute command "+command.toString());
		 		 
		 final ActorRef sender = getSender();
	    
		WSRequest holder = ws.url(serviceUrl);
		holder = holder.addQueryParameter("action", command).addQueryParameter("name", pluginName);
		if (repo != null) holder = holder.addQueryParameter("repository",repo);
		CompletionStage<WSResponse> promise = holder.stream();//setContentType("application/x-www-form-urlencoded; charset=utf-8").post(post);
		promise.thenAccept(response -> {							
			
			final int status = response.getStatus();
			if (status >= 200 && status < 300) {
				Source<ByteString, ?> result = response.getBodyAsSource();			
				SourceRef<ByteString> ref = result.runWith(StreamRefs.sourceRef(), getContext().getSystem());
								
				sender.tell(action.response(next, ref), getSelf());
								
			} else {
			
				final String body = response.getBody();
				System.out.println("BODY="+body+" status="+status);
				result(sender, action, next, Pair.of(false, body));
			}
	    });
	}
	
	public void processWithInput(String pluginName, String command, String repo, DeployAction action, DeployPhase next) {
		 AccessLog.log("Execute command "+command.toString());
		 		 
		 final ActorRef sender = getSender();
	    
		WSRequest holder = ws.url(serviceUrl+"/");
		AccessLog.log("SENDTO:"+serviceUrl);
		holder = holder.addQueryParameter("action", command).addQueryParameter("name", pluginName);
		if (repo != null) holder = holder.addQueryParameter("repository",repo);
		//holder = holder.setContentType("application/binary");
		CompletionStage<WSResponse> promise = holder.post(new SourceBodyWritable(action.exportedData.getSource()));//setContentType("application/x-www-form-urlencoded; charset=utf-8").post(post);
		promise.thenAccept(response -> {							
			final String body = response.getBody();
			final int status = response.getStatus();
			
			System.out.println("BODY="+body+" status="+status);
			result(sender, action, next, Pair.of(status == 200, body));					
	    });
	}

}
