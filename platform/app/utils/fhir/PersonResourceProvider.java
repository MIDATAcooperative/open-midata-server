package utils.fhir;

import utils.exceptions.InternalServerException;
import ca.uhn.fhir.model.dstu2.resource.Person;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.IResourceProvider;

public class PersonResourceProvider extends ResourceProvider implements IResourceProvider {
 
    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    @Override
    public Class<Person> getResourceType() {
        return Person.class;
    }
     
    /**
     * The "@Read" annotation indicates that this method supports the
     * read operation. Read operations should return a single resource
     * instance.
     *
     * @param theId
     *    The read operation takes one parameter, which must be of type
     *    IdDt and must be annotated with the "@Read.IdParam" annotation.
     * @return
     *    Returns a resource matching this identifier, or null if none exists.
     */
    @Read()
    public Person getResourceById(@IdParam IdDt theId) throws InternalServerException {    	
    	Person person = FHIRTools.getPersonRecordOfUser(theId.getIdPart());
    	return person;
    }
          
 
}