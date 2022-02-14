/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.fhir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import utils.InstanceConfig;
import utils.auth.ExecutionInfo;

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
    	ExecutionInfo inf = ResourceProvider.info();
    	if (inf!=null && inf.overrideBaseUrl!=null) return "https://"+InstanceConfig.getInstance().getPlatformServer()+inf.overrideBaseUrl;
    	
    	return "https://"+InstanceConfig.getInstance().getPlatformServer()+"/fhir";
    }
    /**
     * The initialize method is automatically called when the servlet is starting up, so it can
     * be used to configure the servlet to define resource providers, or set up
     * configuration, interceptors, etc.
     */
   @Override
   protected void initialize() throws ServletException {
	   System.out.println("FHIR Servlet init");
	   //String serverBaseUrl = getBaseUrl();	
	   
       setServerAddressStrategy(new ServerAddressStrategy());
       this.setServerConformanceProvider(new MidataConformanceProvider());
       // ResourceProvider.ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());       
       
      /*
       * The servlet defines any number of resource providers, and
       * configures itself to use them by calling
       * setResourceProviders()
       */
      myProviders = new HashMap<String, ResourceProvider>();
      List<IResourceProvider> resourceProviders = new ArrayList<IResourceProvider>();
      System.out.println("FHIR Servlet register providers");
      
      // HERE each resource provider needs to be registered
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
      myProviders.put("AuditEvent", new AuditEventResourceProvider());
      myProviders.put("EpisodeOfCare", new EpisodeOfCareResourceProvider());
      myProviders.put("Procedure", new ProcedureResourceProvider());
      myProviders.put("AllergyIntolerance", new AllergyIntoleranceResourceProvider());
      myProviders.put("Subscription", new SubscriptionResourceProvider());
      
      myProviders.put("Encounter",  new EncounterResourceProvider()); 
      myProviders.put("Flag",  new FlagResourceProvider()); 
      myProviders.put("FamilyMemberHistory",  new FamilyMemberHistoryResourceProvider()); 
      myProviders.put("DiagnosticReport",  new DiagnosticReportResourceProvider()); 
      myProviders.put("ImagingStudy",  new ImagingStudyResourceProvider()); 
      myProviders.put("MedicationAdministration",  new MedicationAdministrationResourceProvider()); 
      myProviders.put("Immunization",  new ImmunizationResourceProvider()); 
      myProviders.put("ImmunizationRecommendation",  new ImmunizationRecommendationResourceProvider());
      myProviders.put("CarePlan",  new CarePlanResourceProvider());
      myProviders.put("MolecularSequence",  new SequenceResourceProvider());
      myProviders.put("Composition", new CompositionResourceProvider());
      myProviders.put("Specimen", new SpecimenResourceProvider());
      myProviders.put("Location", new LocationResourceProvider());
      myProviders.put("ValueSet", new ValueSetResourceProvider());
      myProviders.put("ResearchStudy", new ResearchStudyResourceProvider());
      myProviders.put("Provenance", new ProvenanceResourceProvider());
      myProviders.put("Organization", new OrganizationResourceProvider());
      myProviders.put("List", new ListResourceProvider());
      myProviders.put("RelatedPerson", new RelatedPersonResourceProvider());
      
      resourceProviders.addAll(myProviders.values());
      
      List<Object> plainProviders = new ArrayList<Object>();
      
      plainProviders.add(new Transactions());
      plainProviders.add(new MessageProcessor());
      
      setResourceProviders(resourceProviders);
      //setInterceptors(new PaginationSupport());
      
      
      setPlainProviders(plainProviders);
      //setPagingProvider(new VirtualPaging());
      ResourceProvider.addPathWithVersion("Bundle.entry.fullUrl");
      ResourceProvider.addPathWithVersion("Bundle.entry.response.location");
      getFhirContext().getParserOptions().setDontStripVersionsFromReferencesAtPaths(ResourceProvider.pathesWithVersion);
      
      System.out.println("FHIR Servlet init end");
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