package utils.messaging;

public class MessageResponse {
	final String response;
	
	final int errorcode;
	
	MessageResponse(String response, int errorcode) {
		this.response = response;
		this.errorcode = errorcode;
	}

	public String getResponse() {
		return response;
	}
	
	
	
	public int getErrorcode() {
		return errorcode;
	}

	public String toString() {
		return response;
	}
		
}