/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

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
