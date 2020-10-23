/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.messaging;

import java.util.concurrent.CompletionStage;

import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

public class SMSAPIProvider implements SMSProvider {

	WSClient ws;
	public final static String BASEURL = "https://api.smsapi.com/sms.do";
	private String TOKEN = null;
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
