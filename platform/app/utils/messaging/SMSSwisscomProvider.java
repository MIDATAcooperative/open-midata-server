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

package utils.messaging;

import java.util.concurrent.CompletionStage;

import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

public class SMSSwisscomProvider implements SMSProvider {

	WSClient ws;
	public final static String BASEURL = "https://api.swisscom.com/messaging/sms";
	private String CLIENTID = null;
	private String CLIENTSECRET = null;
	private String SENDER = "Midata";
	
	public SMSSwisscomProvider(WSClient ws, String clientid, String clientsecret, String sender) {
		this.ws = ws;
		this.CLIENTID = clientid;
		this.CLIENTSECRET = clientsecret;
		this.SENDER = sender;
	}
	
	public CompletionStage<MessageStatus> sendSMS(String phone, String text) {
		System.out.println("send SMS: "+phone+" "+text);
		WSRequest holder = ws.url(BASEURL);
		holder.addHeader("client_id", CLIENTID);
		holder.addHeader("SCS-Version", "2");
		holder.addHeader("Content-Type", "application/json");
		holder.addHeader("Accept", "application/json");
		
		holder.addHeader("Authorization", CLIENTSECRET);
		ObjectNode body = Json.newObject();
		//body.put("from", SENDER);
		body.put("to", phone);
		body.put("text", text);
					    
		return holder.post(body).thenApply(response -> {
			if (response.getStatus() != 201) {		
				System.out.println(response.asJson().toString());
				return new MessageStatus(response.getStatus(), response.asJson().toString());
			} 
			return new MessageStatus(); 
		});
	}
		
}
