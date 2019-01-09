package utils.messaging;

import java.util.concurrent.CompletionStage;

import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

public class SMSAPIProvider implements SMSProvider {

	WSClient ws;
	public final static String BASEURL = "https://api.smsapi.com/sms.do";
	private String TOKEN = "hEF04ck2rNYoJMS3Z259PDD8Zg9PeZT8MxRQmwRg";
	private String SENDER = "midata";
	
	public SMSAPIProvider(WSClient ws, String token, String sender) {
		this.ws = ws;
		this.TOKEN = token;
		this.SENDER = sender;
	}
	
	public CompletionStage<MessageStatus> sendSMS(String phone, String text) {
		System.out.println("send SMS: "+phone+" "+text);
		WSRequest holder = ws.url(BASEURL);
		holder.addHeader("Authorization", "Bearer " + TOKEN);
	    holder.addQueryParameter("to", phone);
	    holder.addQueryParameter("message", text);
	    //holder.addQueryParameter("from", SENDER);
		holder.addQueryParameter("format", "json");
		//holder.addQueryParameter("test", "1");
		return holder.get().thenApply(response -> {
			if (response.getStatus() != 200) {		
				System.out.println(response.asJson().toString());
				return new MessageStatus(response.getStatus(), response.asJson().toString());
			} else {
				System.out.println(response.asJson().toString());
			}
			return new MessageStatus(); 
		});
	}
}
