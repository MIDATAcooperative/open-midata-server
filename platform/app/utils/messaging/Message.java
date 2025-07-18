/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.messaging;

import models.MidataId;

public class Message {

	private final String text;
	private final String htmlFrame;
	private final String subject;
	private final String receiverEmail;
	private final String receiverName;
	private final MidataId eventId;
	private final MailSenderType type;
	private final MidataId smtpFromApp;
	
	public Message(String receiverEmail, String receiverName, String subject, String text, String htmlFrame, MidataId eventId, MidataId smtpFromApp) {
		this.text = text;
		this.htmlFrame = htmlFrame;
		this.subject = subject;
		this.receiverEmail = receiverEmail;
		this.receiverName = receiverName;
		this.eventId = eventId;
		this.type = MailSenderType.USER;
		this.smtpFromApp = smtpFromApp;
	}
	
	public Message(MailSenderType type, String receiverEmail, String receiverName, String subject, String text, String htmlFrame, MidataId eventId, MidataId smtpFromApp) {
		this.text = text;
		this.htmlFrame = htmlFrame;
		this.subject = subject;
		this.receiverEmail = receiverEmail;
		this.receiverName = receiverName;
		this.eventId = eventId;
		this.type = type;
		this.smtpFromApp = smtpFromApp;
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
	
	public MidataId getEventId() {
		return eventId;
	}

	public MailSenderType getType() {
		return type;
	}

	public MidataId getSmtpFromApp() {
		return smtpFromApp;
	}
	
	public String getHtmlFrame() {
		return htmlFrame;
	}
		
}
