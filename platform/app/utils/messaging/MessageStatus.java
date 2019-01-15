package utils.messaging;

public class MessageStatus {

	public final int code;
	
	public final String message;
	
	public MessageStatus(int code, String message) {
		this.code = code;
		this.message = message;
	}
	
	public MessageStatus() {
		this.code = 0;
		this.message = null;
	}
}
