package utils.fhir;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.BaseResource;

import akka.japi.Pair;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.CompositeParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.QuantityParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import utils.AccessLog;
import utils.access.op.CompareCaseInsensitive.CompareCaseInsensitiveOperator;
import utils.access.op.CompareCondition.CompareOperator;
import utils.access.op.Condition;
import utils.access.op.EqualsSingleValueCondition;
import utils.access.op.FieldAccess;
import utils.access.op.OrCondition;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;

/**
 * Helper class to create a database Query from a FHIR SearchParameterMap 
 *
 */
public class QueryBuilder {

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
		  Map<String, Condition> indexCondition = dataCondition.indexExpression();
		  if (indexCondition != null) query.putIndexCondition(indexCondition);
		//} 
	}
	
	/*
	public void restriction(String name, String path, String type, boolean indexing) {
		List<List<? extends IQueryParameterType>> paramsAnd = params.get(name);
		if (paramsAnd == null) return;
		
		PredicateBuilder bld = new PredicateBuilder();
		for (List<? extends IQueryParameterType> paramsOr : paramsAnd) {
						
			for (IQueryParameterType param : paramsOr) {
								
				handleRestriction(param, path, type, bld);								
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
		  Map<String, Condition> indexCondition = dataCondition.indexExpression();
		  if (indexCondition != null) query.putIndexCondition(indexCondition);
		} 
	}*/
	
	public void restriction(String name, boolean indexing, String t1, String p1) {
	  restriction(name, indexing, t1, p1, null, null, null, null);	
	}
	
	public void restriction(String name, boolean indexing, String t1, String p1, String t2, String p2) {
	  restriction(name, indexing, t1, p1, t2, p2, null, null);	
	}
	
	public void restriction(String name, boolean indexing, String t1, String p1, String t2, String p2, String t3, String p3) {
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
		  Map<String, Condition> indexCondition = dataCondition.indexExpression();
		  if (indexCondition != null) query.putIndexCondition(indexCondition);
		} 
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
		  Map<String, Condition> indexCondition = dataCondition.indexExpression();
		  if (indexCondition != null) query.putIndexCondition(indexCondition);
		} 
	}
	
	private void handleRestriction(IQueryParameterType param, String path, String type, PredicateBuilder bld) {
		if (param instanceof TokenParam) {
			  TokenParam tokenParam = (TokenParam) param;
			  String system = tokenParam.getSystem();
			  if (type.equals("CodeableConcept")) {
				if (system == null) {
			      bld.addEq(path+".coding.code", tokenParam.getValue(), CompareCaseInsensitiveOperator.EQUALS);
				} else {
				  bld.addEq(path+".coding", "system", system, "code", tokenParam.getValue(), CompareCaseInsensitiveOperator.EQUALS);
				}
			    //if (tokenParam.getSystem() != null) bld.addEq(path+".coding.code", tokenParam.getValue());
			  } else if (type.equals("code")) {
				bld.addEq(path, tokenParam.getValue());
			  } else if (type.equals("Coding")) {
				if (system == null) {
					bld.addEq(path+".code", tokenParam.getValue(), CompareCaseInsensitiveOperator.EQUALS);
				} else {
					bld.addEq(path, "system", system, "code", tokenParam.getValue(), CompareCaseInsensitiveOperator.EQUALS);
				}					
			  } else if (type.equals("Identifier")) {
				if (system == null) {
				   bld.addEq(path+".value", tokenParam.getValue(), CompareCaseInsensitiveOperator.EQUALS);
				} else {
				   bld.addEq(path, "system", system, "value", tokenParam.getValue(), CompareCaseInsensitiveOperator.EQUALS);
				}	
			  } else if (type.equals("ContactPoint")) {
				if (system == null) {
				   bld.addEq(path+".value", tokenParam.getValue(), CompareCaseInsensitiveOperator.EQUALS);
				} else {
				   bld.addEq(path, "use", system, "value", tokenParam.getValue(), CompareCaseInsensitiveOperator.EQUALS);
				}	
			  } else if (type.equals("boolean")) {
				bld.addEq(path, tokenParam.getValue().equals("true"));
			  } else if (type.equals("string")) {
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
							} else {
								bld.addEq(path+".reference", "/"+id, CompareCaseInsensitiveOperator.ENDSWITH);
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
				Date comp = dateParam.getValue();
				ParamPrefixEnum prefix = dateParam.getPrefix();
				TemporalPrecisionEnum precision = dateParam.getPrecision();
				String compStr;
				SimpleDateFormat format;
				switch (precision) {					  
				case SECOND: format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");break;
				case MINUTE: format = new SimpleDateFormat("yyyy-MM-dd HH:mm");break;
				//case HOURS: format = new SimpleDateFormat("yyyy-MM-dd HH");break;
				case DAY: format = new SimpleDateFormat("yyyy-MM-dd");break;
				case MONTH: format = new SimpleDateFormat("yyyy-MM");break;
				case YEAR: format = new SimpleDateFormat("yyyy");break;
				case MILLI:
				default: format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");break;
				}
				compStr = format.format(comp); 
				if (type.equals("Period")) {
					throw new NotImplementedOperationException("Date search on Period not implemented");
				} else {
				    switch (prefix) {
					case GREATERTHAN: bld.addComp(path, CompareOperator.GT, compStr);break;
					case LESSTHAN: bld.addComp(path,CompareOperator.LT, compStr);break;
					case GREATERTHAN_OR_EQUALS: bld.addComp(path, CompareOperator.GE, compStr);break;
					case LESSTHAN_OR_EQUALS: bld.addComp(path, CompareOperator.LE, compStr);break;
					case EQUAL: 
						bld.addComp(path, CompareOperator.GE, compStr);
						bld.addComp(path, CompareOperator.LE, compStr);
						break;
					}
				}
			} else if (param instanceof QuantityParam) {
				
				QuantityParam quantityParam = (QuantityParam) param;
				ParamPrefixEnum prefix = quantityParam.getPrefix();
				if (prefix == null) prefix = ParamPrefixEnum.EQUAL;
				String units = quantityParam.getUnits();
				String system = quantityParam.getSystem();
				BigDecimal val = quantityParam.getValue();
                if (type.equals("Range")) {
                	throw new NotImplementedOperationException("Search on Range fields not yet implemented.");
                	/*switch(prefix) {
    				case GREATERTHAN: bld.addComp(path+".value", CompareOperator.GT, val.doubleValue());break;
    				case LESSTHAN: bld.addComp(path+".value", CompareOperator.LT, val.doubleValue());break;
    				case GREATERTHAN_OR_EQUALS: bld.addComp(path+".value", CompareOperator.GE, val.doubleValue());break;
    				case LESSTHAN_OR_EQUALS: bld.addComp(path+".value", CompareOperator.LE, val.doubleValue());break;
    				case EQUAL: bld.addComp(path+".value", CompareOperator.EQ, val.doubleValue());break;
    				case NOT_EQUAL: bld.addComp(path+".value", CompareOperator.NE, val.doubleValue());break;						
    				}
    				if (system != null) bld.addEq(path+".system", system);
    				if (units!=null) {						
    					bld.add(OrCondition.or(FieldAccess.path(path+".unit", new EqualsSingleValueCondition((Comparable) units)), FieldAccess.path(path+".code", new EqualsSingleValueCondition((Comparable) units))));																
    				}*/
				} else {
					switch(prefix) {
					case GREATERTHAN: bld.addComp(path+".value", CompareOperator.GT, val.doubleValue());break;
					case LESSTHAN: bld.addComp(path+".value", CompareOperator.LT, val.doubleValue());break;
					case GREATERTHAN_OR_EQUALS: bld.addComp(path+".value", CompareOperator.GE, val.doubleValue());break;
					case LESSTHAN_OR_EQUALS: bld.addComp(path+".value", CompareOperator.LE, val.doubleValue());break;
					case EQUAL: bld.addComp(path+".value", CompareOperator.EQ, val.doubleValue());break;
					case NOT_EQUAL: bld.addComp(path+".value", CompareOperator.NE, val.doubleValue());break;						
					}
					if (system != null) bld.addEq(path+".system", system);
					if (units!=null) {						
						bld.add(OrCondition.or(FieldAccess.path(path+".unit", new EqualsSingleValueCondition((Comparable) units)), FieldAccess.path(path+".code", new EqualsSingleValueCondition((Comparable) units))));																
					}	
				}
				
			} 
	}
	
	public List<ReferenceParam> followChain(ReferenceParam r, String targetType) {
		SearchParameterMap params = new SearchParameterMap();
		
		
        params.add(r.getChain(), new StringParam(r.getIdPart()));
        
        if (targetType == null) targetType = r.getResourceType();
        if (targetType == null) throw new UnprocessableEntityException("Reference search needs reference target type in query");
        
        List<BaseResource> resultList = FHIRServlet.myProviders.get(targetType).search(params);
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
                        params.add(r.getChain(), new StringParam(r.getIdPart())); // XXX YOU DO NOT KNOW IF ITS STRING
                        
                        List<BaseResource> resultList;
                        if (targetType == null) {
                           String rt = r.getResourceType();
                           if (rt == null) throw new BadRequestException("error.internal", "Target resource type for chaining not known.");
                           resultList = FHIRServlet.myProviders.get(rt).search(params);
                        } else {
                           resultList = FHIRServlet.myProviders.get(targetType).search(params);
                        }
                        for (BaseResource br : resultList) {
                        	if (keep == null || keep.contains(br.getId())) orResult.add(new ReferenceParam(br.getId()));
                        }
						
						/*AccessLog.log("RT:"+r.getResourceType());
						AccessLog.log("CHAINXX"+r.getChain()); 	
					   AccessLog.log("CHAINXV"+r.getIdPart());*/
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
			
			if (p.getSystem() != null && p.getValue() != null) {
			  result.add(p.getSystem()+" "+p.getValue());
			} else return null;
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
	public boolean recordOwnerReference(String name, String refType) throws AppException {
		List<ReferenceParam> patients = resolveReferences(name, refType);
		if (patients != null && FHIRTools.areAllOfType(patients, Sets.create("Patient", "Practitioner", "Person"))) {
			query.putAccount("owner", FHIRTools.referencesToIds(patients));
			return true;
		}
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
			restriction(name, false, "CodeableConcept", path);
		} else {
			restriction(name, true, "CodeableConcept", path);
		}		
	}
}
