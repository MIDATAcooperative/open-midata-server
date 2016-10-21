package utils.fhir;

import org.hl7.fhir.dstu3.model.Person;

import ca.uhn.fhir.rest.server.IResourceProvider;


public abstract class PersonResourceProvider extends ResourceProvider<Person> implements IResourceProvider {
 
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