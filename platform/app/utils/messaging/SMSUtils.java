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

import javax.inject.Inject;

import com.typesafe.config.Config;

import play.libs.ws.WSClient;


public class SMSUtils {

	private SMSProvider smsClient;
	
	
	@Inject
	public SMSUtils(SMSProvider smsClient) {
		this.smsClient = smsClient;		
	}
	
	private static SMSUtils instance;
	
	public static void setInstance(WSClient ws, Config config) {
		String provider = config.getString("sms.provider");
		if (provider.equals("smsapi")) {
			String token = config.getString("sms.token");
			if (token != null && token.length()>0 && !token.equals("SMS_OAUTH_TOKEN")) {
			  instance = new SMSUtils(new SMSAPIProvider(ws, token, config.getString("sms.from")));
			}
		} else if (provider.equals("swisscom")) {
			String token = config.getString("sms.token");
			if (token != null && token.length()>0 && !token.equals("SMS_OAUTH_TOKEN")) {
			  int p = token.indexOf(":");			   
			  String clientid = token.substring(0, p);
			  String clientsecret = token.substring(p+1);
			  instance = new SMSUtils(new SMSSwisscomProvider(ws, clientid, clientsecret, config.getString("sms.from")));
			}
		}
	}
	
	public static boolean isAvailable() {
		return instance != null;
	}
	
	public static void sendSMS(String phone, String text) {
		if (instance == null) throw new NullPointerException("SMSUtils not initialized");
		instance.smsClient.sendSMS(normalizePhone(phone), text);
	}
	
	public static String normalizePhone(String phone) {
		phone = phone.replace(" ", "");
		phone = phone.replace("/", "");
		phone = phone.replace("+", "00");
		if (!phone.startsWith("00")) {
		  if (phone.startsWith("0")) phone = "0041" + phone.substring(1);
		  else phone = "0041" + phone;
		}
		return phone;
	}
}
