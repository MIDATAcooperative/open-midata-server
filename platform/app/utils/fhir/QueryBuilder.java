package utils.fhir;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utils.AccessLog;
import utils.access.op.Condition;
import utils.access.op.CompareCaseInsensitive.CompareCaseInsensitiveOperator;

import com.ctc.wstx.dtd.TokenModel;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;

public class QueryBuilder {

	private SearchParameterMap params;
	private Query query;
	
	public QueryBuilder(SearchParameterMap params, Query query, String format) {
		this.params = params;
		this.query = query;
		this.query.putAccount("format", format);
		handleCommon();
	}
	
	public void handleCommon() {
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
	
	public void restriction(String name, String path, String type, boolean indexing) {
		List<List<? extends IQueryParameterType>> paramsAnd = params.get(name);
		if (paramsAnd == null) return;
		
		PredicateBuilder bld = new PredicateBuilder();
		for (List<? extends IQueryParameterType> paramsOr : paramsAnd) {
			bld.and();
			for (IQueryParameterType param : paramsOr) {
				bld.or();
				if (param instanceof TokenParam) {
				  TokenParam tokenParam = (TokenParam) param;
				  
				  if (type.equals("CodeableConcept")) {
				    bld.addEq(path+".coding.code", tokenParam.getValue());
				  } else if (type.equals("code")) {
					bld.addEq(path, tokenParam.getValue());
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
					String id = referenceParam.getIdPart();
					String resType = referenceParam.getResourceType();					
					if (id != null) {
						if (resType != null) {
							bld.addEq(path+".reference", resType+"/"+id);
						} else {
							bld.addEq(path+".reference", "/"+id, CompareCaseInsensitiveOperator.ENDSWITH);
						}
					}
				} else if (param instanceof DateParam) {
					DateParam dateParam = (DateParam) param;
					Date comp = dateParam.getValue();
					ParamPrefixEnum prefix = dateParam.getPrefix();
					
					switch (prefix) {
					case GREATERTHAN: bld.addComp(path, ">", comp);break;
					case LESSTHAN: bld.addComp(path,"<", comp);break;
					case GREATERTHAN_OR_EQUALS: bld.addComp(path, ">=", comp);break;
					case LESSTHAN_OR_EQUALS: bld.addComp(path,"<=", comp);break;
					case EQUAL: bld.addEq(path, comp);break;
					}
				}
			}
		}
		
		Condition dataCondition = bld.get().optimize();
		query.putDataCondition(dataCondition);
		
		if (indexing) {
		  Map<String, Condition> indexCondition = dataCondition.indexExpression();
		  if (indexCondition != null) query.putIndexCondition(indexCondition);
		} 
	}
	
	public List<ReferenceParam> resolveReferences(String name, String targetType) {
		List<List<? extends IQueryParameterType>> paramsAnd = params.get(name);
		if (paramsAnd == null) return null;
				
		for (List<? extends IQueryParameterType> paramsOr : paramsAnd) {
			
			List<ReferenceParam> orResult = new ArrayList<ReferenceParam>();
			
			for (IQueryParameterType param : paramsOr) {				
				if (param instanceof ReferenceParam) {
					
					ReferenceParam r = (ReferenceParam) param;
					if (r.getIdPart() != null) orResult.add(r);					
					else if (r.getChain() != null) {
					   	
						
					}
				}
			}
			
			// TODO AND SUPPOPT IS MISSING
			return orResult;
		}
		
		return null;		
	}
	
	public Set<String> tokensToCodeSystemStrings(String name) {
		List<List<? extends IQueryParameterType>> paramsAnd = params.get(name);
		if (paramsAnd == null) return null;
		
		Set<String> result = new HashSet<String>();
		for (List<? extends IQueryParameterType> paramsOr : paramsAnd) {
		  if (paramsOr == null) continue;
		  for (IQueryParameterType p2 : paramsOr) {
			TokenParam p = (TokenParam) p2;
			AccessLog.log("tp="+p);
			AccessLog.log(p.toString());
			if (p == null) continue;
			//if (p.getMissing()) return null;
			if (p.getModifier() != null) return null;
			AccessLog.log("A");
			if (p.getSystem() != null && p.getValue() != null) {
			  result.add(p.getSystem()+" "+p.getValue());
			} else return null;
		  }
		}
		AccessLog.log("B");
		return result;
	}
}
