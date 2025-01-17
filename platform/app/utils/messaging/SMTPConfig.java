package utils.messaging;

import models.JsonSerializable;

public class SMTPConfig implements JsonSerializable {
	
	public String host;
	public int port;
	public boolean ssl;
	public boolean tls;
	public String user;
	public String password;
	   
}
