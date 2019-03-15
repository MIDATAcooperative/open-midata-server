package utils.messaging;

public class MessageResponse {
	final String response;
	final String plugin;
	final int errorcode;
	
	MessageResponse(String response, int errorcode, String plugin) {
		this.response = response;
		this.errorcode = errorcode;
		this.plugin = plugin;
	}

	public String getResponse() {
		return response;
	}
	
	
	/**
	 * Get used plugin. Just for error reporting.
	 * @return
	 */
	public String getPlugin() {
		return plugin;
	}

	public int getErrorcode() {
		return errorcode;
	}

	public String toString() {
		return response;
	}
		
}