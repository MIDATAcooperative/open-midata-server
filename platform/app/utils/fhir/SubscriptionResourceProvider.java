package utils.fhir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Subscription;
import org.hl7.fhir.dstu3.model.Subscription.SubscriptionChannelComponent;
import org.hl7.fhir.dstu3.model.Subscription.SubscriptionStatus;
import org.hl7.fhir.dstu3.model.codesystems.SubscriptionChannelType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import com.mongodb.BasicDBObject;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import models.ContentCode;
import models.MidataId;
import models.Plugin;
import models.SubscriptionData;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.access.Feature_FormatGroups;
import utils.auth.ExecutionInfo;
import utils.auth.KeyManager;
import utils.collections.CMaps;
import utils.db.ObjectIdConversion;
import utils.exceptions.AppException;
import utils.exceptions.AuthException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;
import utils.messaging.ServiceHandler;
import utils.messaging.SubscriptionManager;

public class SubscriptionResourceProvider extends ReadWriteResourceProvider<Subscription, SubscriptionData> implements IResourceProvider {

	public SubscriptionResourceProvider() {
		registerSearches("Subscription", getClass(), "getSubscription");
	}
	
	@Override
	public Class<Subscription> getResourceType() {
		return Subscription.class;
	}

	/**
	 * Default implementation to retrieve a FHIR resource by id.
	 * @param theId ID of resource to be retrieved
	 * @return Resource read from database
	 * @throws AppException
	 */
	@Read()
	public Subscription getResourceById(@IdParam IIdType theId) throws AppException {
		if (!info().context.mayAccess("Subscription", "fhir/Subscription")) return null;
		SubscriptionData subscription = SubscriptionData.getByIdAndOwner(MidataId.from(theId.getIdPart()), info().ownerId, SubscriptionData.ALL);	
		if (subscription == null) return null;
		return readSubscriptionFromMidataSubscription(subscription);
	}
	    
    
	/**
	 * Convert a MIDATA Subscription object into a FHIR Subscription object
	 * @param userToConvert user to be converted into a FHIR object
	 * @return FHIR person
	 * @throws AppException
	 */
	public Subscription readSubscriptionFromMidataSubscription(SubscriptionData subscriptionToConvert) throws AppException {		
		IParser parser = ctx().newJsonParser();		
		Subscription result = parser.parseResource(getResourceType(), subscriptionToConvert.fhirSubscription.toString());
				
		return result;
	}
	
	public static Subscription subscription(SubscriptionData data) throws AppException {
	  SubscriptionResourceProvider subscriptionProvider = (SubscriptionResourceProvider) FHIRServlet.myProviders.get("Subscription");
	  //SubscriptionResourceProvider.setExecutionInfo(inf);
	  return subscriptionProvider.readSubscriptionFromMidataSubscription(data);
	}
	
	public static void update(Subscription subscription) throws AppException {
		  SubscriptionResourceProvider subscriptionProvider = (SubscriptionResourceProvider) FHIRServlet.myProviders.get("Subscription");
		  //SubscriptionResourceProvider.setExecutionInfo(inf);
		  subscriptionProvider.update(subscription.getIdElement(), subscription);
		}
	
	/*
	public static void updateMidataConsent(models.Consent consentToConvert) throws AppException {
		org.hl7.fhir.dstu3.model.Consent c = new org.hl7.fhir.dstu3.model.Consent();

		c.setId(consentToConvert._id.toString());
		
		switch (consentToConvert.status) {
		case ACTIVE:c.setStatus(org.hl7.fhir.dstu3.model.Consent.ConsentState.ACTIVE);break;
		case UNCONFIRMED:c.setStatus(org.hl7.fhir.dstu3.model.Consent.ConsentState.PROPOSED);break;
		case REJECTED:c.setStatus(org.hl7.fhir.dstu3.model.Consent.ConsentState.REJECTED);break;
		case EXPIRED:c.setStatus(org.hl7.fhir.dstu3.model.Consent.ConsentState.INACTIVE);break;
		}
		
		String categoryCode = consentToConvert.categoryCode;
		if (categoryCode == null) categoryCode = "default";
		c.addCategory().addCoding().setCode(categoryCode).setSystem("http://midata.coop/codesystems/consent-category");

		if (consentToConvert.owner != null) {
		  c.setPatient(FHIRTools.getReferenceToUser(User.getById(consentToConvert.owner, Sets.create("role", "firstname", "lastname", "email"))));
		} else if (consentToConvert.externalOwner != null) {
		  c.setPatient(new Reference().setIdentifier(new Identifier().setSystem("http://midata.coop/identifier/patient-login-or-invitation").setValue(consentToConvert.externalOwner)));
		}
		
		if (consentToConvert.validUntil != null) {
		  c.setPeriod(new Period().setEnd(consentToConvert.validUntil));	
		}
		if (consentToConvert.createdBefore != null) {
		  c.setDataPeriod(new Period().setEnd(consentToConvert.createdBefore));
		}
		if (consentToConvert.dateOfCreation != null) {
		  c.setDateTime(consentToConvert.dateOfCreation);
		}
		c.setPolicyRule("http://hl7.org/fhir/ConsentPolicy/opt-in");
		c.addPurpose(new Coding("http://midata.coop/codesystems/consent-type", consentToConvert.type.toString(), null));
		
		c.addExtension().setUrl("http://midata.coop/extensions/consent-name").setValue(new StringType(consentToConvert.name));
		
		String encoded = ctx.newJsonParser().encodeResourceToString(c);		
		consentToConvert.fhirConsent = (DBObject) JSON.parse(encoded);				
	}
		*/	
	   @Search()
	    public List<IBaseResource> getSubscription(
	    		@Description(shortDefinition="The ID of the resource")
	  			@OptionalParam(name="_id")
	  			TokenAndListParam the_id, 
	    
	  			@Description(shortDefinition="The language of the resource")
	  			@OptionalParam(name="_language")
	  			StringAndListParam the_language, 
	    
	  			@Description(shortDefinition="A tag to be added to the resource matching the criteria")
	  			@OptionalParam(name="add-tag")
	  			TokenAndListParam theAdd_tag, 
	    
	  			@Description(shortDefinition="Contact details for the subscription")
	  			@OptionalParam(name="contact")
	  			TokenAndListParam theContact, 
	    
	  			@Description(shortDefinition="The search rules used to determine when to send a notification")
	  			@OptionalParam(name="criteria")
	  			StringAndListParam theCriteria, 
	    
	  			@Description(shortDefinition="The mime-type of the notification payload")
	  			@OptionalParam(name="payload")
	  			StringAndListParam thePayload, 
	    
	  			@Description(shortDefinition="The current state of the subscription")
	  			@OptionalParam(name="status")
	  			TokenAndListParam theStatus, 
	    
	  			@Description(shortDefinition="The type of channel for the sent notifications")
	  			@OptionalParam(name="type")
	  			TokenAndListParam theType, 
	    
	  			@Description(shortDefinition="The uri that will receive the notifications")
	  			@OptionalParam(name="url")
	  			UriAndListParam theUrl, 
	  	  				  
	  			@IncludeParam(reverse=true)
	  			Set<Include> theRevIncludes,
	  			@Description(shortDefinition="Only return resources which were last updated as specified by the given range")
	 			@OptionalParam(name="_lastUpdated")
	 			DateRangeParam theLastUpdated, 
	 
	 			@IncludeParam(allow= {
	 				"*"
	 			}) 
	 			Set<Include> theIncludes,
	 			
	 			@Sort 
	 			SortSpec theSort,
	 			
	 			@ca.uhn.fhir.rest.annotation.Count
	 			Integer theCount
	    						    			    
    		) throws AppException {
	    	
	    	SearchParameterMap paramMap = new SearchParameterMap();
	    		    	
	    	paramMap.add("_id", the_id);
	    	paramMap.add("_language", the_language);
	    	paramMap.add("add-tag", theAdd_tag);
	    	paramMap.add("contact", theContact);
	    	paramMap.add("criteria", theCriteria);
	    	paramMap.add("payload", thePayload);
	    	paramMap.add("status", theStatus);
	    	paramMap.add("type", theType);
	    	paramMap.add("url", theUrl);
	    	
	    	paramMap.setRevIncludes(theRevIncludes);
	    	paramMap.setLastUpdated(theLastUpdated);
	    	paramMap.setIncludes(theIncludes);
	    	paramMap.setSort(theSort);
	    	paramMap.setCount(theCount);
	    	//paramMap.setElements(theElements);
	    	//paramMap.setSummary(theSummary);
	    	    		    	
	    	return search(paramMap);    	    	    	
	    }
	
	@Override
	public List<SubscriptionData> searchRaw(SearchParameterMap params) throws AppException {		
		ExecutionInfo info = info();
		if (!info().context.mayAccess("Subscription", "fhir/Subscription")) return Collections.emptyList();
		Query query = new Query();		
		QueryBuilder builder = new QueryBuilder(params, query, null);
			
		builder.handleIdRestriction();
		//builder.recordOwnerReference("patient", "Patient", null);
		
		builder.restriction("add-tag", false, QueryBuilder.TYPE_CODING, "fhirSubscription.tag");
		builder.restriction("contact", false, QueryBuilder.TYPE_CONTACT_POINT, "fhirSubscription.contact");
		builder.restriction("criteria", false, QueryBuilder.TYPE_STRING, "fhirSubscription.criteria");
		builder.restriction("payload", false, QueryBuilder.TYPE_STRING, "fhirSubscription.channel.payload");	
		builder.restriction("status", false, QueryBuilder.TYPE_CODE, "fhirSubscription.status");
		builder.restriction("type", false, QueryBuilder.TYPE_CODE, "fhirSubscription.channel.type");
		builder.restriction("url", false, QueryBuilder.TYPE_URI, "fhirSubscription.channel.endpoint");
						
		builder.restriction("_lastUpdated", false, QueryBuilder.TYPE_DATETIME, "lastUpdated");
												
		Map<String, Object> properties = query.retrieveAsNormalMongoQuery();
					
		//if (!info.context.mayAccess("Subscription", "fhir/Subscription")) properties.put("_id", info.context.getTargetAps());
		
		ObjectIdConversion.convertMidataIds(properties, "_id", "owner");
		
		return SubscriptionData.getByOwner(info.ownerId, properties, SubscriptionData.ALL);				
	} 	
	
	@Create
	@Override
	public MethodOutcome createResource(@ResourceParam Subscription theSubscription) {
		return super.createResource(theSubscription);
	}
	
	@Update
	@Override
	public MethodOutcome updateResource(@IdParam IdType theId, @ResourceParam Subscription theSubscription) {		
		try {			
			return update(theId, theSubscription);
		} catch (BaseServerResponseException e) {
			throw e;
		} catch (BadRequestException e2) {
			throw new InvalidRequestException(e2.getMessage());
		} catch (InternalServerException e3) {
			ErrorReporter.report("FHIR (update resource)", null, e3);
			throw new InternalErrorException(e3);
		} catch (Exception e4) {
			ErrorReporter.report("FHIR (update resource)", null, e4);
			throw new InternalErrorException(e4);
		}	
	}
				
	
	private static void mayShare(MidataId pluginId, String format, String content) throws AppException {
		Plugin plugin = Plugin.getById(pluginId);
		if (plugin == null || !plugin.resharesData) throw new ForbiddenOperationException("Plugin is not allowed to share data.");
		if (!isSubQuery(plugin.defaultQuery, CMaps.map("format", format).mapNotEmpty("content", content))) throw new ForbiddenOperationException("Plugin is not allowed to share this type of data.");				
	}
	
	
	private static boolean isSubQuery(Map<String, Object> masterQuery, Map<String, Object> subQuery) throws AppException {
		
		if (masterQuery.containsKey("$or")) {
			Collection<Map<String, Object>> parts = (Collection<Map<String, Object>>) masterQuery.get("$or");			
			for (Map<String, Object> part :parts) {
				boolean match = isSubQuery(part, subQuery);
				if (match) return true;
			}
		}
		
		masterQuery = new HashMap<String, Object>(masterQuery);
		subQuery = new HashMap<String, Object>(subQuery);
								
		Feature_FormatGroups.convertQueryToContents(masterQuery);		
		Feature_FormatGroups.convertQueryToContents(subQuery);
		
		masterQuery.remove("group-system");
		subQuery.remove("group-system");
		AccessLog.log("newtest:");
	    for (Map.Entry<String, Object> entry : masterQuery.entrySet()) {
	    	AccessLog.log("test subquery:"+entry.getKey());
	    	if (!subQuery.containsKey(entry.getKey())) return false;
	    	Set<String> master = utils.access.Query.getRestriction(entry.getValue(), entry.getKey());
	    	Set<String> sub = utils.access.Query.getRestriction(subQuery.get(entry.getKey()), entry.getKey());
	    	AccessLog.log("master="+master.toString()+" sub="+sub.toString());
	    	if (!master.containsAll(sub)) return false;	    	
	    }
		return true;
	}
		

	@Override
	public void createPrepare(SubscriptionData subscriptionData, Subscription theResource) throws AppException {
		if (!info().context.mayAccess("Subscription", "fhir/Subscription")) throw new ForbiddenOperationException("Plugin is not allowed to create subscriptions (Access Query)."); 
		
        String crit = theResource.getCriteria();
        populateSubscriptionCriteria(subscriptionData, crit);
        
        if (theResource.getStatus().equals(SubscriptionStatus.ACTIVE) || theResource.getStatus().equals(SubscriptionStatus.REQUESTED)) mayShare(info().pluginId, subscriptionData.format, subscriptionData.content);
		if (theResource.getStatus().equals(SubscriptionStatus.REQUESTED)) theResource.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionData.active = theResource.getStatus().equals(SubscriptionStatus.ACTIVE);
        
        subscriptionData.endDate = theResource.getEnd();
        subscriptionData.lastUpdated = System.currentTimeMillis();       
        theResource.setId(subscriptionData._id.toString());
        String encoded = ctx.newJsonParser().encodeResourceToString(theResource);	
        subscriptionData.fhirSubscription = BasicDBObject.parse(encoded);
	}
	
	public static void fillInFhirForAutorun(SubscriptionData data) {
		Subscription s = new Subscription();
		s.setId(data._id.toString());
		s.setStatus(SubscriptionStatus.ACTIVE);
		SubscriptionChannelComponent channel = new SubscriptionChannelComponent();
		channel.setEndpoint("server.js");
		channel.setType(Subscription.SubscriptionChannelType.MESSAGE);		
		s.setChannel(channel);
		s.setCriteria("time");
		String encoded = ctx.newJsonParser().encodeResourceToString(s);	
        data.fhirSubscription = BasicDBObject.parse(encoded);
	}
	
	
	
	public static void populateSubscriptionCriteria(SubscriptionData subscriptionData, String crit) throws InternalServerException {
		 int p = crit.indexOf("?");
	        if (p>0) {
	        	
	        	subscriptionData.format = "fhir/"+crit.substring(0, p);
	        	
	        	String q = crit.substring(p+1);
	        	if (q.startsWith("code=")) {
	        		String cnt = q.substring("code=".length());
	        		String content = ContentCode.getContentForSystemCode(cnt.replace('|', ' '));
	        		if (content != null) subscriptionData.content = content;
	        		else throw new InvalidRequestException("Not supported subscription criteria. Restrict using format 'system|code'");
	        	} else if (crit.equals("Observation")) subscriptionData.content = null; 
	        	else throw new InvalidRequestException("Not supported subscription criteria.");
	        } else {
	        	if (crit.startsWith("time")) {
		          subscriptionData.format="time";
		          subscriptionData.content = null;
		        } else {
	              subscriptionData.format = "fhir/"+crit;
	              subscriptionData.content = crit;
		        }
	           
	        }
	}

	@Override
	public void createExecute(SubscriptionData subscriptionData, Subscription theResource) throws AppException {
        subscriptionData.add();	
        SubscriptionManager.subscriptionChange(subscriptionData);
	}

	@Override
	public SubscriptionData init() {
		SubscriptionData subscriptionData = new SubscriptionData();
		subscriptionData._id = new MidataId();
		subscriptionData.owner = info().ownerId;
		subscriptionData.app = info().pluginId;
		subscriptionData.instance = info().targetAPS;
		try {
		  subscriptionData.session = ServiceHandler.encrypt(KeyManager.instance.currentHandle(info().ownerId));
		} catch (AuthException e) {
			throw new InvalidRequestException("Authorization problem.");
		} catch (InternalServerException e2) {
			throw new InvalidRequestException("Background service not running.");
		}
		return subscriptionData;
	}

	@Override
	public void updatePrepare(SubscriptionData subscriptionData, Subscription theResource) throws AppException {
        createPrepare(subscriptionData, theResource);						
	}

	@Override
	public void updateExecute(SubscriptionData subscriptionData, Subscription theResource) throws AppException {		
		subscriptionData.update();	
		SubscriptionManager.subscriptionChange(subscriptionData);
	}

	@Override
	public SubscriptionData fetchCurrent(IIdType theId) throws AppException {
		return SubscriptionData.getByIdAndOwner(MidataId.from(theId.getIdPart()), info().ownerId, SubscriptionData.ALL);	
	}

	@Override
	public void processResource(SubscriptionData record, Subscription resource) throws AppException {		
		
	}

	@Override
	public List<Subscription> parse(List<SubscriptionData> subscriptions, Class<Subscription> resultClass) throws AppException {
		List<Subscription> result = new ArrayList<Subscription>();
		for (SubscriptionData subscription : subscriptions) {
			result.add(readSubscriptionFromMidataSubscription(subscription));
		}
		
		return result;
	}

	@Override
	public String getVersion(SubscriptionData record) {
		return "";
	}

	@Override
	public Date getLastUpdated(SubscriptionData record) {
		return new Date(record.lastUpdated);
	}	
	
	
}