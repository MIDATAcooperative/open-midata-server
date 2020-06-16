package utils.fhir;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.r4.hapi.rest.server.ServerCapabilityStatementProvider;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.UriType;

import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import utils.InstanceConfig;

public class MidataConformanceProvider  extends ServerCapabilityStatementProvider  {

	private boolean doadd = true;
	@Override
	@Metadata
	public CapabilityStatement  getServerConformance(HttpServletRequest arg0, RequestDetails arg1) {
		setPublisher("midata.coop");		
		CapabilityStatement  conformance = super.getServerConformance(arg0,arg1);
				
		if (doadd) {
			Extension dt = conformance.getRest().get(0).getSecurity().addExtension().setUrl("http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris");
			String tokenUrl = "https://"+InstanceConfig.getInstance().getPlatformServer()+"/v1/token";
			dt.addExtension(new Extension("token", new UriType(tokenUrl)));
			String authUrl = InstanceConfig.getInstance().getPortalOriginUrl()+"/authservice";		
			dt.addExtension(new Extension("authorize", new UriType(authUrl)));
			// Not required for newer HAPI versions: doadd = false;
		}
		
		return conformance;
	}

	
}
