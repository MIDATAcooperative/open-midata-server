package utils.fhir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import models.Model;
import utils.AccessLog;

public class VirtualBundleProvider implements IBundleProvider {

	private ResourceProvider<DomainResource, Model> myProvider;
	private SearchParameterMap myMap;
	private Date now = new Date();
	
	private List<IBaseResource> result;
	private int size = -1;
	
	private String from;
	private int fromSkip;
	
	
	public VirtualBundleProvider(ResourceProvider prov, SearchParameterMap map)  {
		this.myProvider = prov;
		this.myMap = map;
		
		if (myMap.getCount() == null) myMap.setCount(preferredPageSize());
		
		if (myMap.getSkip() == null) {
		   result = myProvider.search(myMap);
		   if (myMap.getFrom() == null) { 
			   this.size = result.size(); 
			   this.fromSkip = -1;
		   } else {
			   this.size = -1;
			   this.fromSkip = result.size();
		   }
		} else {
			this.size = -1;
			result = null;
		}
		
	}
	
	public VirtualBundleProvider(ResourceProvider prov, SearchParameterMap map, int fromSkip)  {
		this.myProvider = prov;
		this.myMap = map;
		this.fromSkip = fromSkip;						
	}
		
	
	protected String serialize() throws IOException {
		AccessLog.log("SERIALIZE "+fromSkip);
		ByteArrayOutputStream s = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(s);
		out.writeUTF(myProvider.getResourceType().getSimpleName());
		out.writeInt(fromSkip);
		out.writeObject(myMap);
		out.close();
		return Base64.getEncoder().encodeToString(s.toByteArray());
	}
	
	@Override
	public IPrimitiveType<Date> getPublished() {
		return new DateTimeType(now);
	}

	@Override
	public List<IBaseResource> getResources(int arg0, int arg1) {
		
		if (arg0 == 0) return result;
		
		AccessLog.log("CALLED "+arg0+" TO "+arg1);
		myMap.setCount(arg1-arg0);
		myMap.setSkip(arg0);
		if (arg0 != fromSkip) {
			AccessLog.log("NO FROM MATCH "+arg0+" vs "+fromSkip);
			myMap.setFrom(null);
		}
		
		List<IBaseResource> result = myProvider.search(myMap);
		if (myMap.getFrom() == null) {
			size = arg0 + result.size();
		} else {
			fromSkip = arg0 + result.size();
			AccessLog.log("SET FROM SKIP="+fromSkip);
		}
		return result;
	}

	@Override
	public String getUuid() {
		AccessLog.log("GETUUID");
		try {
		  return serialize();
		} catch (IOException e) {
		  AccessLog.logException("getUUid", e);
		  return null;
		}
	}

	@Override
	public Integer preferredPageSize() {		
		return 1000;
	}

	@Override
	public Integer size() {		
		return size >= 0 ? size : null;
	}

}
