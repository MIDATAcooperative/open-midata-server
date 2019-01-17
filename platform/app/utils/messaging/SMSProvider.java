package utils.messaging;

import java.util.concurrent.CompletionStage;

public interface SMSProvider {

	public CompletionStage<MessageStatus> sendSMS(String phone, String text);
}
