package utils.fhir_stu3;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Base64;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.IPagingProvider;
import utils.AccessLog;

public class VirtualPaging implements IPagingProvider {

	@Override
	public int getDefaultPageSize() {
		return 10;
	}

	@Override
	public int getMaximumPageSize() {
		return 10000;
	}

	@Override
	public IBundleProvider retrieveResultList(String arg0) {		
		try {
			byte[] data = Base64.getDecoder().decode(arg0);
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
			String providerName = in.readUTF();
			int fromSkip = in.readInt();
			AccessLog.log("DESERIALIZE "+fromSkip);
			SearchParameterMap map = (SearchParameterMap) in.readObject();
			ResourceProvider prov = FHIRServlet.myProviders.get(providerName);
			return new VirtualBundleProvider(prov, map, fromSkip);
		} catch (IOException e) {
			return null;
		} catch (ClassNotFoundException e) {
			return null;	
		}
	}

	@Override
	public String storeResultList(IBundleProvider arg0) {
		return arg0.getUuid();	
	}

}