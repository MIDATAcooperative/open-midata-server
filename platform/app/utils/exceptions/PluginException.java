package utils.exceptions;

import models.MidataId;

public class PluginException extends AppException {

	private static final long serialVersionUID = 1L;
	
	private MidataId pluginId;

	public PluginException(String localeKey, Throwable cause) {
		super(localeKey, cause);
	}
	
	public PluginException(MidataId pluginId, String localeKey, String msg) {
		super(localeKey, msg);
		this.pluginId = pluginId;
	}

	public MidataId getPluginId() {
		return pluginId;
	}

}
