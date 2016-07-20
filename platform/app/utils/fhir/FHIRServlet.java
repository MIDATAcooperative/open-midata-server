package utils.fhir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import play.Play;

import utils.servlet.PlayHttpServletContext;

import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;

// https://demo.careevolution.com/PDemo/PDemo.html?iss=https://localhost:9000/fhir
// https://demo.careevolution.com/PDemo/PDemo.html?iss=https://test.midata.coop:9000/fhir
// https://sap.modulemd.com/SmartFhirPortal/?iss=https://test.midata.coop:9000/fhir
// http://pillbox.medapptech.com/initapp/launch.html?iss=https://test.midata.coop:9000/fhir
// https://apps-dstu2.smarthealthit.org/cardiac-risk/launch.html?iss=https://test.midata.coop:9000/fhir

@WebServlet(urlPatterns= {"/fhir/*"}, displayName="FHIR Server")
public class FHIRServlet extends RestfulServer {
 
    private static final long serialVersionUID = 1L;
   
    /**
     * The initialize method is automatically called when the servlet is starting up, so it can
     * be used to configure the servlet to define resource providers, or set up
     * configuration, interceptors, etc.
     */
   @Override
   protected void initialize() throws ServletException {
	   
	   String serverBaseUrl = "https://"+Play.application().configuration().getString("platform.server")+"/fhir";
       setServerAddressStrategy(new HardcodedServerAddressStrategy(serverBaseUrl));
       this.setServerConformanceProvider(new MidataConformanceProvider());
       ResourceProvider.ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
	   
      /*
       * The servlet defines any number of resource providers, and
       * configures itself to use them by calling
       * setResourceProviders()
       */
      List<IResourceProvider> resourceProviders = new ArrayList<IResourceProvider>();
      resourceProviders.add(new PatientResourceProvider());
      resourceProviders.add(new ObservationResourceProvider());   
      resourceProviders.add(new MedicationOrderResourceProvider());
      setResourceProviders(resourceProviders);
         
   }

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		super.doGet(request, response);
	}
   
    
     
}