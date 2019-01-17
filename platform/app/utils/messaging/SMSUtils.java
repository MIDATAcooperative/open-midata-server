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
