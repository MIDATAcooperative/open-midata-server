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

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.ServerTools;
import utils.exceptions.InternalServerException;

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
			JsonNode result = response.asJson();
			if (response.getStatus() != 200 || result.hasNonNull("error")) {		
				try {
					String msg = response.getStatus()+" "+response.asJson().toString();
					throw new InternalServerException("error.internal", msg);
				} catch (InternalServerException e) {
					ErrorReporter.report("SMSAPI Sender", null, e);
					AccessLog.logException("SMSAPI Sender Error", e);
				} finally {
					ServerTools.endRequest();
				}
				return new MessageStatus(response.getStatus(), response.asJson().toString());								
			} else {
				System.out.println(response.asJson().toString());
			}
			return new MessageStatus(); 
		});
	}
}
