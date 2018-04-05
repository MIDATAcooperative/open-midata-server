package controllers.research;

import models.MidataId;

public class JoinMessage {
  
	private final MidataId app;
	private final MidataId user;
	private final MidataId study;
	
	public JoinMessage(MidataId app, MidataId user, MidataId study) {
		this.app = app;
		this.user = user;
		this.study = study;
	}

	public MidataId getApp() {
		return app;
	}

	public MidataId getUser() {
		return user;
	}

	public MidataId getStudy() {
		return study;
	}
	
	
}
