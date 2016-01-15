package utils.fhir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import utils.servlet.PlayHttpServletContext;

import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;

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
	   
	   String serverBaseUrl = "https://demo.midata.coop:9000/fhir";
       setServerAddressStrategy(new HardcodedServerAddressStrategy(serverBaseUrl));
	   
      /*
       * The servlet defines any number of resource providers, and
       * configures itself to use them by calling
       * setResourceProviders()
       */
      List<IResourceProvider> resourceProviders = new ArrayList<IResourceProvider>();
      resourceProviders.add(new PatientResourceProvider());
      resourceProviders.add(new ObservationResourceProvider());   
      setResourceProviders(resourceProviders);
          
   }

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		super.doGet(request, response);
	}
   
    
     
}