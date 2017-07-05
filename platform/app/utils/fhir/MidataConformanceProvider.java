package utils.fhir;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.dstu3.hapi.rest.server.ServerCapabilityStatementProvider;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.UriType;

import ca.uhn.fhir.rest.annotation.Metadata;
import play.Play;

public class MidataConformanceProvider  extends ServerCapabilityStatementProvider  {

	private boolean doadd = true;
	@Override
	@Metadata
	public CapabilityStatement  getServerConformance(HttpServletRequest arg0) {
		CapabilityStatement  conformance = super.getServerConformance(arg0);
				
		if (doadd) {
			Extension dt = conformance.getRest().get(0).getSecurity().addExtension().setUrl("http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris");
			String tokenUrl = "https://"+Play.application().configuration().getString("platform.server")+"/v1/token";
			dt.addExtension(new Extension("token", new UriType(tokenUrl)));
			String authUrl = Play.application().configuration().getString("portal.originUrl")+"/oauth.html#/portal/oauth2";		
			dt.addExtension(new Extension("authorize", new UriType(authUrl)));
			doadd = false;
		}
		
		return conformance;
	}

	
}
