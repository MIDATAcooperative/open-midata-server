package utils.messaging;

public class SMS {

	private final String text;
	private final String phone;
	
	public SMS(String phone, String text) {
		this.phone = phone;
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public String getPhone() {
		return phone;
	}
	
	
}
