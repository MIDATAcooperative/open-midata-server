package utils.fhir;

import javax.servlet.http.HttpServletRequest;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.resource.Conformance;
import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.server.provider.dstu2.ServerConformanceProvider;

public class MidataConformanceProvider extends ServerConformanceProvider {

	@Override
	@Metadata
	public Conformance getServerConformance(HttpServletRequest arg0) {
		Conformance conformance = super.getServerConformance(arg0);
		
		/*
		 "extension": [{
	          "url": "http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris",
	          "extension": [{
	            "url": "token",
	            "valueUri": "https://my-server.org/token"
	          },{
	            "url": "authorize",
	            "valueUri": "https://my-server.org/authorize"
	          }]
	        }]
		*/
		
		ExtensionDt dt = conformance.getRestFirstRep().getSecurity().addUndeclaredExtension(false, "http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris");
		dt.addUndeclaredExtension(false, "token", new UriDt("https://localhost:9000/v1/token"));
		dt.addUndeclaredExtension(false, "authorize", new UriDt("https://localhost:9002/#/portal/oauth2"));
		
		return conformance;
	}

	
}
