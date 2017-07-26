package utils.fhir;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.dstu3.model.DomainResource;

import akka.japi.Pair;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.param.CompositeParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.QuantityParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.param.UriParamQualifierEnum;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import utils.AccessLog;
import utils.access.op.AndCondition;
import utils.access.op.CompareCaseInsensitive;
import utils.access.op.CompareCaseInsensitive.CompareCaseInsensitiveOperator;
import utils.access.op.CompareCondition.CompareOperator;
import utils.access.op.Condition;
import utils.access.op.EqualsSingleValueCondition;
import utils.access.op.ExistsCondition;
import utils.access.op.FieldAccess;
import utils.access.op.OrCondition;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.stats.Stats;

/**
 * Helper class to create a database Query from a FHIR SearchParameterMap 
 *
 */
public class QueryBuilder {

	public final static String TYPE_DATE = "date";
	public final static String TYPE_DATETIME = "DateTime";
	public final static String TYPE_PERIOD = "Period";
	public final static String TYPE_DATETIME_OR_PERIOD = "DateTime|Period";

	public final static String TYPE_CODEABLE_CONCEPT = "CodeableConcept";
	public final static String TYPE_CODING = "Coding";
	public final static String TYPE_CODE = "code";
	public final static String TYPE_IDENTIFIER = "Identifier";
	public final static String TYPE_CONTACT_POINT = "ContactPoint";
	
	public final static String TYPE_BOOLEAN = "boolean";
	public final static String TYPE_STRING = "string";
	public final static String TYPE_MARKDOWN = "string";
	public final static String TYPE_URI = "uri";
	
	public final static String TYPE_QUANTITY = "Quantity";
	public final static String TYPE_RANGE = "Range";
	public final static String TYPE_QUANTITY_OR_RANGE = "Quantity|Range";
	public final static String TYPE_AGE_OR_RANGE = "Age|Range";
	
	private SearchParameterMap params;
	private Query query;
	
	public QueryBuilder(SearchParameterMap params, Query query, String format) throws AppException {
		this.params = params;
		this.query = query;
		if (format != null) this.query.putAccount("format", format);
		handleCommon();
	}
	
	public void handleCommon() throws AppException {
		if (params.getLastUpdated() != null) {
			Date from = params.getLastUpdated().getLowerBoundAsInstant();
			Date to = params.getLastUpdated().getUpperBoundAsInstant();
			if (from != null) query.putAccount("updated-after", from);
			if (to != null) query.putAccount("updated-before", to);
		}
		if (params.getCount() != null) {
			query.putAccount("limit", params.getCount());
		}	
		query.initSort(params.getSortNames());
		
	}
	
	public void handleIdRestriction() throws AppException {
		if (params.containsKey("_id")) {
	           Set<String> ids = paramToStrings("_id");
	           if (ids != null) query.putAccount("_id", ids);
		}
	}
	
	public void restriction(String name, String path1, String path2, String type1, String type2) {
		List<List<? extends IQueryParameterType>> paramsAnd = params.get(name);
		if (paramsAnd == null) return;
		
		PredicateBuilder bld = new PredicateBuilder();
		for (List<? extends IQueryParameterType> paramsOr : paramsAnd) {
						
			for (IQueryParameterType param : paramsOr) {
	           CompositeParam compositeParam = (CompositeParam) param;
	           IQueryParameterType param1 = compositeParam.getLeftValue();
	           IQueryParameterType param2 = compositeParam.getRightValue();
	           
	           handleRestriction(param2, path2, type2, bld);
	           handleRestriction(param1, path1, type1, bld);
	           
	           
	           bld.or();
	           
			}
			bld.and();
		}
		
		Condition unOpt = bld.get();
		AccessLog.log("Before opt: "+unOpt.toString());
		Condition dataCondition = unOpt.optimize();
		AccessLog.log("After opt: "+dataCondition.toString());
		query.putDataCondition(dataCondition);
		
		//if (indexing) {
		  Condition indexCondition = dataCondition.indexExpression();
		  if (indexCondition != null) query.putIndexCondition(indexCondition);
		//} 
	}
		
	
	public void restriction(String name, boolean indexing, String t1, String p1) {
	  restriction(name, indexing, t1, p1, null, null, null, null);	
	}
	
	public void restriction(String name, boolean indexing, String t1, String p1, String t2, String p2) {
	  restriction(name, indexing, t1, p1, t2, p2, null, null);	
	}
	
	public void restriction(String name, boolean indexing, String t1, String p1, String t2, String p2, String t3, String p3) {
		SortOrderEnum sortOrder = params.hasSortParam(name);
		if (sortOrder != null) addSort(name, sortOrder, t1, p1, t2, p2, t3, p3);
		
		List<List<? extends IQueryParameterType>> paramsAnd = params.get(name);
		if (paramsAnd == null) return;
		
		PredicateBuilder bld = new PredicateBuilder();
		for (List<? extends IQueryParameterType> paramsOr : paramsAnd) {
						
			for (IQueryParameterType param : paramsOr) {
								
				handleRestriction(param, p1, t1, bld);								
			    if (p2 != null) {
			    	bld.or();
			    	handleRestriction(param, p2, t2, bld);
			    }
			    if (p3 != null) {
			    	bld.or();
			    	handleRestriction(param, p3, t3, bld);
			    }
			    bld.or();
			}
			bld.and();
		}
		
		Condition unOpt = bld.get();
		AccessLog.log("Before opt: "+unOpt.toString());
		Condition dataCondition = unOpt.optimize();
		AccessLog.log("After opt: "+dataCondition.toString());
		query.putDataCondition(dataCondition);
		
		if (indexing) {
		  Condition indexCondition = dataCondition.indexExpression();
		  if (indexCondition != null) query.putIndexCondition(indexCondition);
		} 
	}
	
	public void addSort(String name, SortOrderEnum order, String t1, String p1, String t2, String p2, String t3, String p3) {
		String path = "data."+sortPath(t1,p1);
		if (p2 != null) path+="|"+sortPath(t2, p2);
		if (p3 != null) path+="|"+sortPath(t3, p3);
		AccessLog.log("add sort name="+name+" path="+path);
		query.putSort(name, order, path);
	}
		
	public String sortPath(String t, String p) {
		if (t.equals(TYPE_DATETIME_OR_PERIOD)) {
			return p+"DateTime|"+p+"Period.start";
		} else if (t.equals(TYPE_STRING)) {
			return p;
		} else if (t.equals(TYPE_CODE)) {
			return p;
		} else if (t.equals(TYPE_CODEABLE_CONCEPT)) {
			return p+".coding.code";
		} else if (t.equals(TYPE_CODING)) {
			return p+".code";
		} else if (t.equals(TYPE_AGE_OR_RANGE)) {
			return p+"Age.value|"+p+"Range.low.value";
		} else if (t.equals(TYPE_CONTACT_POINT)) {
			return p+".value";
		} else if (t.equals(TYPE_DATE)) {
			return p;
		} else if (t.equals(TYPE_DATETIME)) {
			return p;
		} else if (t.equals(TYPE_IDENTIFIER)) {
			return p+".value";
		} else if (t.equals(TYPE_PERIOD)) {
			return p+".start";
		} else if (t.equals(TYPE_QUANTITY)) {
			return p+".value";
		} else if (t.equals(TYPE_QUANTITY_OR_RANGE)) {
			return p+"Quantity.value|"+p+"Range.low.value";
		} else if (t.equals(TYPE_RANGE)) {
			return p+".low.value";
		} else if (t.equals(TYPE_URI)) {
			return p;
		} else return "null";
	}
	
	public void restrictionMany(String name, boolean indexing, String type, String... paths) {
		List<List<? extends IQueryParameterType>> paramsAnd = params.get(name);
		if (paramsAnd == null) return;
		
		PredicateBuilder bld = new PredicateBuilder();
		for (List<? extends IQueryParameterType> paramsOr : paramsAnd) {
						
			for (IQueryParameterType param : paramsOr) {
				for (String path : paths) {				
				  handleRestriction(param, path, type, bld);								
			      bld.or();
				}
			}
			bld.and();
		}
		
		Condition unOpt = bld.get();
		AccessLog.log("Before opt: "+unOpt.toString());
		Condition dataCondition = unOpt.optimize();
		AccessLog.log("After opt: "+dataCondition.toString());
		query.putDataCondition(dataCondition);
		
		if (indexing) {
		  Condition indexCondition = dataCondition.indexExpression();
		  if (indexCondition != null) query.putIndexCondition(indexCondition);
		} 
	}
	
	private void handleRestriction(IQueryParameterType param, String path, String type, PredicateBuilder bld) {
		if (param.getMissing() != null) {
			boolean exist = !param.getMissing().booleanValue();
			if (type.equals(TYPE_DATETIME_OR_PERIOD)) {
				if (exist) bld.add(OrCondition.or(FieldAccess.path(path+"DateTime", new ExistsCondition(true)), FieldAccess.path(path+"Period", new ExistsCondition(true))));
				else bld.add(AndCondition.and(FieldAccess.path(path+"DateTime", new ExistsCondition(false)), FieldAccess.path(path+"Period", new ExistsCondition(false))));
			} else if (type.equals(TYPE_QUANTITY_OR_RANGE)) {
				if (exist) bld.add(OrCondition.or(FieldAccess.path(path+"Quantity", new ExistsCondition(true)), FieldAccess.path(path+"Range", new ExistsCondition(true))));
				else bld.add(AndCondition.and(FieldAccess.path(path+"Quantity", new ExistsCondition(false)), FieldAccess.path(path+"Range", new ExistsCondition(false))));
			}  else if (type.equals(TYPE_AGE_OR_RANGE)) {
				if (exist) bld.add(OrCondition.or(FieldAccess.path(path+"Age", new ExistsCondition(true)), FieldAccess.path(path+"Range", new ExistsCondition(true))));
				else bld.add(AndCondition.and(FieldAccess.path(path+"Age", new ExistsCondition(false)), FieldAccess.path(path+"Range", new ExistsCondition(false))));
			} else {
			   bld.addExists(path, exist);
			}
						
			return;
		}
		if (param instanceof TokenParam) {
			  TokenParam tokenParam = (TokenParam) param;
			  String system = tokenParam.getSystem();
			  boolean isText = tokenParam.isText();
			  if (type.equals(TYPE_CODEABLE_CONCEPT)) {
				if (isText) {
				  bld.add(
						  OrCondition.or(
						    FieldAccess.path(path+".text", new CompareCaseInsensitive(tokenParam.getValue(), CompareCaseInsensitiveOperator.CONTAINS)),
						    FieldAccess.path(path+".coding.display", new CompareCaseInsensitive(tokenParam.getValue(), CompareCaseInsensitiveOperator.CONTAINS))
                          ));						  
				} else 	if (system == null) {
			      bld.addEq(path+".coding.code", tokenParam.getValue(), CompareCaseInsensitiveOperator.EQUALS);
				} else {
				  bld.addEq(path+".coding", "system", system, "code", tokenParam.getValue(), CompareCaseInsensitiveOperator.EQUALS);
				}
			    //if (tokenParam.getSystem() != null) bld.addEq(path+".coding.code", tokenParam.getValue());
			  } else if (type.equals(TYPE_CODE)) {
				bld.addEq(path, tokenParam.getValue());
			  } else if (type.equals(TYPE_CODING)) {
				if (isText) {
					bld.addEq(path+".display",tokenParam.getValue(), CompareCaseInsensitiveOperator.CONTAINS);
				} else if (system == null) {
					bld.addEq(path+".code", tokenParam.getValue(), CompareCaseInsensitiveOperator.EQUALS);
				} else {
					bld.addEq(path, "system", system, "code", tokenParam.getValue(), CompareCaseInsensitiveOperator.EQUALS);
				}					
			  } else if (type.equals(TYPE_IDENTIFIER)) {
				if (isText) {
				   bld.addEq(path+".type.text",tokenParam.getValue(), CompareCaseInsensitiveOperator.CONTAINS);
				} else if (system == null) {
				   bld.addEq(path+".value", tokenParam.getValue(), CompareCaseInsensitiveOperator.EQUALS);
				} else {
				   bld.addEq(path, "system", system, "value", tokenParam.getValue(), CompareCaseInsensitiveOperator.EQUALS);
				}	
			  } else if (type.equals(TYPE_CONTACT_POINT)) {
				if (system == null) {
				   bld.addEq(path+".value", tokenParam.getValue(), CompareCaseInsensitiveOperator.EQUALS);
				} else {
				   bld.addEq(path, "use", system, "value", tokenParam.getValue(), CompareCaseInsensitiveOperator.EQUALS);
				}	
			  } else if (type.equals(TYPE_BOOLEAN)) {
				bld.addEq(path, tokenParam.getValue().equals("true"));
			  } else if (type.equals(TYPE_STRING)) {
				bld.addEq(path, tokenParam.getValue(), CompareCaseInsensitiveOperator.EQUALS);
			  } 
			} else if (param instanceof StringParam) {
			  StringParam stringParam = (StringParam) param;
			  
			  if (stringParam.isExact()) {
				  bld.addEq(path, stringParam.getValue());
			  } else if (stringParam.isContains()) {
				  bld.addEq(path, stringParam.getValue(), CompareCaseInsensitiveOperator.CONTAINS);
			  } else {
				  bld.addEq(path, stringParam.getValue(), CompareCaseInsensitiveOperator.STARTSWITH);
			  }
			} else if (param instanceof ReferenceParam) {
				ReferenceParam referenceParam = (ReferenceParam) param;
				
				if (referenceParam.getChain() != null) {
					List<ReferenceParam> resolved = followChain(referenceParam, type);
					if (resolved.isEmpty()) {
						bld.addEq(path+".reference", "__false");
					} else {
					
					for (ReferenceParam rp : resolved) {
					   
						String id = rp.getIdPart();
						String resType = rp.getResourceType();					
						if (id != null) {
							if (resType != null) {
								bld.addEq(path+".reference", resType+"/"+id);
								bld.or();
							} else {
								bld.addEq(path+".reference", "/"+id, CompareCaseInsensitiveOperator.ENDSWITH);
								bld.or();
							}
						}
					
					}
					}
				} else {
				
					String id = referenceParam.getIdPart();
					String resType = referenceParam.getResourceType();					
					if (id != null) {
						if (resType != null) {
							bld.addEq(path+".reference", resType+"/"+id);
						} else {
							bld.addEq(path+".reference", "/"+id, CompareCaseInsensitiveOperator.ENDSWITH);
						}
					} 
				}
			} else if (param instanceof DateParam) {
				DateParam dateParam = (DateParam) param;
				//Date comp = dateParam.getValue();
				ParamPrefixEnum prefix = dateParam.getPrefix();
				TemporalPrecisionEnum precision = dateParam.getPrecision();
				
				String lPath = null;
				String hPath = null;
				
				Date lDate = null;
				Date hDate = null;
				
				Calendar cal = dateParam.getValueAsDateTimeDt().getValueAsCalendar();
				//cal.setTime(comp);
				
				switch (precision) {					  
				case SECOND: 
					cal.set(Calendar.MILLISECOND, 0);
					lDate = cal.getTime();
					cal.set(Calendar.MILLISECOND, 1000);
					hDate = cal.getTime();
					break;
				case MINUTE: 
					cal.set(Calendar.MILLISECOND, 0);
					cal.set(Calendar.SECOND, 0);
					lDate = cal.getTime();
					cal.add(Calendar.MINUTE, 1);
					hDate = cal.getTime();
					break;
				case DAY: 
					cal.set(Calendar.MILLISECOND, 0);
					cal.set(Calendar.SECOND, 0);
					cal.set(Calendar.MINUTE, 0);
					cal.set(Calendar.HOUR_OF_DAY, 0);
					lDate = cal.getTime();
					cal.add(Calendar.DAY_OF_MONTH, 1);
					hDate = cal.getTime();
					break;
				case MONTH: 
					int month = cal.get(Calendar.MONTH);
					int year = cal.get(Calendar.YEAR);
					cal.set(year, month, 1, 0, 0, 0);
					lDate = cal.getTime();
					cal.add(Calendar.MONTH, 1);
					hDate = cal.getTime();
					break;
				case YEAR: 
					year = cal.get(Calendar.YEAR);
					cal.set(year, 0, 1, 0, 0, 0);
					lDate = cal.getTime();
					cal.add(Calendar.YEAR, 1);
					hDate = cal.getTime();
					break;
				case MILLI:
				default: 
                    lDate = cal.getTime();
                    cal.add(Calendar.MILLISECOND, 1);
                    hDate = cal.getTime();
                    break;
				}
				if (type.equals(TYPE_DATETIME) || type.equals(TYPE_DATE)) {
					lPath = path;
					hPath = path;
				} else if (type.equals(TYPE_PERIOD)) {
					lPath = path+".start|null";
					lPath = path+".end|null";
				} else if (type.equals(TYPE_DATETIME_OR_PERIOD)) {
					lPath = path+"DateTime|"+path+"Period.start|null";
					hPath = path+"DateTime|"+path+"Period.end|null";
				} else throw new NullPointerException();
				
				if (prefix==null) prefix = ParamPrefixEnum.EQUAL;
				
			    switch (prefix) {
				case GREATERTHAN: bld.addComp(hPath, CompareOperator.GE, hDate, true);break;
				case LESSTHAN: bld.addComp(lPath,CompareOperator.LT, lDate, true);break;
				case GREATERTHAN_OR_EQUALS:														
					bld.addCompOr(lPath, CompareOperator.GE, lDate, true);
					bld.addCompOr(hPath, CompareOperator.GE, hDate, true);					
					break;
				case LESSTHAN_OR_EQUALS:
										
					bld.addCompOr(lPath, CompareOperator.LE, lDate, true);
					bld.addCompOr(hPath, CompareOperator.LT, hDate, true);										
					break;
				case STARTS_AFTER:					
					bld.addComp(lPath, CompareOperator.GE, hDate, false);
					break;
				case ENDS_BEFORE:
					bld.addComp(hPath, CompareOperator.LT, lDate, false);
					break;
				case EQUAL:					
					bld.addComp(lPath, CompareOperator.GE, lDate, false, CompareOperator.LT, hDate, false);
					if (!lPath.equals(hPath)) {
                      bld.addComp(hPath, CompareOperator.LT, hDate, false, CompareOperator.GE, lDate, false);
					}
					break;
				case NOT_EQUAL:					
					bld.addCompOr(lPath, CompareOperator.LT, lDate, true);
					bld.addCompOr(hPath, CompareOperator.GE, hDate, true);
					break;
				default:throw new NullPointerException();
				}
			
			} else if (param instanceof QuantityParam) {
				
				QuantityParam quantityParam = (QuantityParam) param;
				ParamPrefixEnum prefix = quantityParam.getPrefix();
				if (prefix == null) prefix = ParamPrefixEnum.EQUAL;
				String units = quantityParam.getUnits();
				String system = quantityParam.getSystem();
				BigDecimal val = quantityParam.getValue();               
                
                String lPath = null;
				String hPath = null;
				boolean simple = true;
				
				if (type.equals(TYPE_QUANTITY)) {
				  lPath = path+".value";
				  hPath = path+".value";
				} else if (type.equals(TYPE_RANGE)){
				  lPath = path+".low.value";
				  hPath = path+".high.value";
				  simple = false;
				} else if (type.equals(TYPE_QUANTITY_OR_RANGE)) {
				  lPath = path+"Quantity.value|"+path+"Range.low.value|null";
				  hPath = path+"Quantity.value|"+path+"Range.high.value|null";
				  simple = false;
				} else if (type.equals(TYPE_AGE_OR_RANGE)) {
					  lPath = path+"Age.value|"+path+"Range.low.value|null";
					  hPath = path+"Age.value|"+path+"Range.high.value|null";
					  simple = false;	
				} else throw new NullPointerException();
				
			    switch (prefix) {
				case GREATERTHAN: bld.addComp(hPath, CompareOperator.GT, val.doubleValue(), true);break;
				case LESSTHAN: bld.addComp(lPath,CompareOperator.LT, val.doubleValue(), true);break;
				case GREATERTHAN_OR_EQUALS:														
					bld.addCompOr(lPath, CompareOperator.GE, val.doubleValue(), true);
					if (!simple) bld.addCompOr(hPath, CompareOperator.GE, val.doubleValue(), true);					
					break;
				case LESSTHAN_OR_EQUALS:									
					bld.addCompOr(lPath, CompareOperator.LE, val.doubleValue(), true);
					if (!simple) bld.addCompOr(hPath, CompareOperator.LE, val.doubleValue(), true);										
					break;			
				case EQUAL:					
					bld.addComp(lPath, CompareOperator.GE, val.doubleValue(), false);
                    bld.addComp(hPath, CompareOperator.LE, val.doubleValue(), false);					
					break;
				case NOT_EQUAL:					
					bld.addCompOr(lPath, CompareOperator.LT, val.doubleValue(), true);
					bld.addCompOr(hPath, CompareOperator.GT, val.doubleValue(), true);
					break;
				default:throw new NullPointerException();
				}
			    
			    if (system != null) bld.addEq(path+".system", system);
				if (units!=null) {						
					bld.add(OrCondition.or(FieldAccess.path(path+".unit", new EqualsSingleValueCondition((Comparable) units)), FieldAccess.path(path+".code", new EqualsSingleValueCondition((Comparable) units))));																
				}
                
				
			} else if (param instanceof UriParam) {
				UriParam uriParam = (UriParam) param;
				UriParamQualifierEnum qualifier = uriParam.getQualifier();
				String uri = uriParam.getValue();
				
				if (qualifier == null) {
				  bld.addEq(path, uri);
				} else if (qualifier == UriParamQualifierEnum.BELOW) {
				  bld.addEq(path, uri, CompareCaseInsensitiveOperator.STARTSWITH); // should be case sensitive
				} else throw new NotImplementedOperationException("ABOVE modifier not implemented.");
			}
	}
	
	public List<ReferenceParam> followChain(ReferenceParam r, String targetType) {
		SearchParameterMap params = new SearchParameterMap();
				         
        if (targetType == null) targetType = r.getResourceType();
        if (targetType == null) throw new UnprocessableEntityException("Reference search needs reference target type in query");
        
        params.add(r.getChain(), ResourceProvider.asQueryParameter(targetType, r.getChain(), r));
        
        ResourceProvider prov = FHIRServlet.myProviders.get(targetType);
        if (prov==null) throw new InvalidRequestException("Unknown resource type '"+targetType+"' during chaining.");
        List<BaseResource> resultList = prov.search(params);
        List<ReferenceParam> result = new ArrayList<ReferenceParam>();
        for (BaseResource br : resultList) {
        	result.add(new ReferenceParam(targetType+"/"+br.getId()));
        }
        return result;
	}
	
	public List<ReferenceParam> resolveReferences(String name, String targetType) throws AppException {
		List<List<? extends IQueryParameterType>> paramsAnd = params.get(name);
		if (paramsAnd == null) return null;
				
		List<ReferenceParam> orResult = null;
		Set<String> keep = null;
		for (List<? extends IQueryParameterType> paramsOr : paramsAnd) {
			
			if (orResult != null) {
				keep = new HashSet<String>();
				for (ReferenceParam r : orResult) keep.add(r.getIdPart());
			}
			
			orResult = new ArrayList<ReferenceParam>();
			
			for (IQueryParameterType param : paramsOr) {				
				if (param instanceof ReferenceParam) {
					
					ReferenceParam r = (ReferenceParam) param;
					
					if (r.getChain() != null) {
						
						SearchParameterMap params = new SearchParameterMap();						
                                                
                        List<BaseResource> resultList;
                        if (targetType == null) {
                           String rt = r.getResourceType();
                           if (rt == null) throw new BadRequestException("error.internal", "Target resource type for chaining not known.");
                           params.add(r.getChain(), ResourceProvider.asQueryParameter(rt, r.getChain(), r));
                           resultList = FHIRServlet.myProviders.get(rt).search(params);
                        } else {
                           params.add(r.getChain(), ResourceProvider.asQueryParameter(targetType, r.getChain(), r));
                           resultList = FHIRServlet.myProviders.get(targetType).search(params);
                        }
                        for (BaseResource br : resultList) {
                        	if (keep == null || keep.contains(br.getId())) orResult.add(new ReferenceParam(br.getId()));
                        }
						
						AccessLog.log("RT:"+r.getResourceType());
						AccessLog.log("CHAINXX"+r.getChain()); 	
					   AccessLog.log("CHAINXV "+r.toTokenParam(ResourceProvider.ctx).getSystem());
					   AccessLog.log("CHAINXV2 "+r.toTokenParam(ResourceProvider.ctx).getValue());
					} else
					if (r.getIdPart() != null) {
						if (keep == null || keep.contains(r.getIdPart())) orResult.add(r);					
					}
					
				}
			}
			

		}
		
		return orResult;		
	}
	
	public Set<String> tokensToCodeSystemStrings(String name) {
		List<List<? extends IQueryParameterType>> paramsAnd = params.get(name);
		if (paramsAnd == null) return null;
		
		Set<String> result = new HashSet<String>();
		for (List<? extends IQueryParameterType> paramsOr : paramsAnd) {
		  if (paramsOr == null) continue;
		  for (IQueryParameterType p2 : paramsOr) {
			TokenParam p = (TokenParam) p2;
			
			if (p == null) continue;
			//if (p.getMissing()) return null;
			if (p.getModifier() != null) return null;
			
			if (p.getSystem() != null && p.getValue() != null && p.getSystem().length()>0 && p.getValue().length()>0) {
			  result.add(p.getSystem()+" "+p.getValue());
			} else {
				Stats.addComment("Parameter "+name+": Always provide code system and code if possible");
				return null;
			}
		  }
		}
	
		return result;
	}
	
	public Set<String> paramToStrings(String name) {
		List<List<? extends IQueryParameterType>> paramsAnd = params.get(name);
		if (paramsAnd == null) return null;
		
		Set<String> result = new HashSet<String>();
		for (List<? extends IQueryParameterType> paramsOr : paramsAnd) {
		  if (paramsOr == null) continue;
		  for (IQueryParameterType p2 : paramsOr) {
			StringParam p = (StringParam) p2;
			
			if (p == null) continue;
			
			result.add(p.getValue());		
		  }
		}
	
		return result;
	}

	/**
	 * Use a FHIR query parameter to restrict the record owner
	 * @param name name of FHIR search parameter
	 * @param refType Resource referenced (Patient, Practitioner, ...)
	 */
	public boolean recordOwnerReference(String name, String refType, String emptyField) throws AppException {
		List<ReferenceParam> patients = resolveReferences(name, refType);
		if (patients != null && FHIRTools.areAllOfType(patients, Sets.create("Patient", "Practitioner", "Person"))) {
			query.putAccount("owner", FHIRTools.referencesToIds(patients));
			if (emptyField != null) query.putDataCondition(new FieldAccess(emptyField, new ExistsCondition(false)));
			return true;
		}
		if (Stats.enabled && patients == null && name.equals("patient") && !params.containsKey(name)) Stats.addComment("Restrict by patient?");
		return patients == null;
		
	}
	
	/**
	 * Use a FHIR query parameter to restrict the record creator
	 * @param name name of FHIR search parameter
	 * @param refType Resource referenced (Patient, Practitioner, ...)
	 */
	public void recordCreatorReference(String name, String refType) throws AppException {
		List<ReferenceParam> patients = resolveReferences(name, refType);
		if (patients != null) {
			query.putAccount("creator", FHIRTools.referencesToIds(patients));
		}
		
	}

	/**
	 * Use a FHIR query parameter to restrict the record code
	 * @param name name of FHIR search parameter
	 * @param path path of CodeableConcept in FHIR record 
	 */
	public void recordCodeRestriction(String name, String path) throws AppException {
		Set<String> codes = tokensToCodeSystemStrings(name);
		if (codes != null) {
			query.putAccount("code", codes);
			restriction(name, false, TYPE_CODEABLE_CONCEPT, path);
		} else {
			restriction(name, true, TYPE_CODEABLE_CONCEPT, path);
		}		
	}
}
