package utils.fhir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.server.BundleInclusionRule;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import play.Play;

// https://demo.careevolution.com/PDemo/PDemo.html?iss=https://localhost:9000/fhir
// https://demo.careevolution.com/PDemo/PDemo.html?iss=https://test.midata.coop:9000/fhir
// https://sap.modulemd.com/SmartFhirPortal/?iss=https://test.midata.coop:9000/fhir
// http://pillbox.medapptech.com/initapp/launch.html?iss=https://test.midata.coop:9000/fhir
// https://apps-dstu2.smarthealthit.org/cardiac-risk/launch.html?iss=https://test.midata.coop:9000/fhir

@WebServlet(urlPatterns= {"/fhir/*"}, displayName="FHIR Server")
public class FHIRServlet extends RestfulServer {
 
    private static final long serialVersionUID = 1L;
    
    public static Map<String, ResourceProvider> myProviders;
   
    public static String getBaseUrl() {
    	return "https://"+Play.application().configuration().getString("platform.server")+"/fhir";
    }
    /**
     * The initialize method is automatically called when the servlet is starting up, so it can
     * be used to configure the servlet to define resource providers, or set up
     * configuration, interceptors, etc.
     */
   @Override
   protected void initialize() throws ServletException {
	   
	   String serverBaseUrl = getBaseUrl();
       setServerAddressStrategy(new HardcodedServerAddressStrategy(serverBaseUrl));
       this.setServerConformanceProvider(new MidataConformanceProvider());
       ResourceProvider.ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());       
       
      /*
       * The servlet defines any number of resource providers, and
       * configures itself to use them by calling
       * setResourceProviders()
       */
      myProviders = new HashMap<String, ResourceProvider>();
      List<IResourceProvider> resourceProviders = new ArrayList<IResourceProvider>();
      
      myProviders.put("Patient", new PatientResourceProvider());
      myProviders.put("Observation",  new ObservationResourceProvider());        
      myProviders.put("DocumentReference",  new DocumentReferenceProvider());
      myProviders.put("Goal",  new GoalResourceProvider());
      myProviders.put("Condition",  new ConditionResourceProvider());
      myProviders.put("Communication", new CommunicationResourceProvider());
      myProviders.put("QuestionnaireResponse", new QuestionnaireResponseResourceProvider());
      myProviders.put("Questionnaire", new QuestionnaireResourceProvider());
      myProviders.put("Basic",  new BasicResourceProvider());
      myProviders.put("Media",  new MediaResourceProvider());
      myProviders.put("Person", new PersonResourceProvider());
      myProviders.put("Task", new TaskResourceProvider());
      myProviders.put("Appointment", new AppointmentResourceProvider());
      myProviders.put("Group", new GroupResourceProvider());
      myProviders.put("Practitioner", new PractitionerResourceProvider());
      myProviders.put("Device", new DeviceResourceProvider());
      myProviders.put("MedicationStatement", new MedicationStatementResourceProvider());
      myProviders.put("Consent", new ConsentResourceProvider());
      
      resourceProviders.addAll(myProviders.values());
      setResourceProviders(resourceProviders);
      
      setPlainProviders(new Transactions());
   }

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		super.doGet(request, response);
	}
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		super.doPost(request, response);
	}
	
	@Override
	public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		super.doPut(request, response);
	}
	
	@Override
	public void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		super.doDelete(request, response);
	}
   
    
     
}