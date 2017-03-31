package utils.messaging;

public class Message {

	private final String text;
	private final String subject;
	private final String receiverEmail;
	private final String receiverName;
	
	public Message(String receiverEmail, String receiverName, String subject, String text) {
		this.text = text;
		this.subject = subject;
		this.receiverEmail = receiverEmail;
		this.receiverName = receiverName;
	}

	public String getText() {
		return text;
	}

	public String getSubject() {
		return subject;
	}

	public String getReceiverEmail() {
		return receiverEmail;
	}

	public String getReceiverName() {
		return receiverName;
	}

	
		
}
