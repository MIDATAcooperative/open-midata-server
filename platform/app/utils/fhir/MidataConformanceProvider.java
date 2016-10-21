package utils.fhir;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.dstu3.hapi.rest.server.ServerConformanceProvider;
import org.hl7.fhir.dstu3.model.Conformance;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.UriType;

import ca.uhn.fhir.rest.annotation.Metadata;
import play.Play;

public class MidataConformanceProvider extends ServerConformanceProvider {

	@Override
	@Metadata
	public Conformance getServerConformance(HttpServletRequest arg0) {
		Conformance conformance = super.getServerConformance(arg0);
				
		Extension dt = conformance.getRest().get(0).getSecurity().addExtension().setUrl("http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris");
		String tokenUrl = "https://"+Play.application().configuration().getString("platform.server")+"/v1/token";
		dt.addExtension(new Extension("token", new UriType(tokenUrl)));
		String authUrl = Play.application().configuration().getString("portal.originUrl")+"/#/portal/oauth2";		
		dt.addExtension(new Extension("authorize", new UriType(authUrl)));
		
		return conformance;
	}

	
}
