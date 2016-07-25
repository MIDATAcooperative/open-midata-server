package utils.fhir;

import ca.uhn.fhir.rest.server.IResourceProvider;
import utils.exceptions.InternalServerException;


public abstract class PersonResourceProvider extends ResourceProvider implements IResourceProvider {
 
  /*
    @Override
    public Class<Person> getResourceType() {
        return Person.class;
    }
     
  
    @Read()
    public Person getResourceById(@IdParam IdDt theId) throws InternalServerException {    	
    	Person person = FHIRTools.getPersonRecordOfUser(theId.getIdPart());
    	return person;
    }
    */      
 
}