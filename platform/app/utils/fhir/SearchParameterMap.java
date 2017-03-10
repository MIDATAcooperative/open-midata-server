package utils.fhir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ca.uhn.fhir.model.api.IQueryParameterAnd;
import ca.uhn.fhir.model.api.IQueryParameterOr;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;

/**
 * Class for storing search parameter for a FHIR search operation
 *
 */
public class SearchParameterMap extends HashMap<String, List<List<? extends IQueryParameterType>>> {
  
  	private static final long serialVersionUID = 1L;
  
  	private Integer myCount;  	
  	private Set<Include> myIncludes;
  	private DateRangeParam myLastUpdated;
  	private boolean myPersistResults = true;
//  	private RequestDetails myRequestDetails;
  	private Set<Include> myRevIncludes;
  	private SortSpec mySort;
  	private Set<String> elements;
  	private SummaryEnum summary = SummaryEnum.FALSE;
  
  	public void add(String theName, IQueryParameterAnd<?> theAnd) {
  		if (theAnd == null) {
  			return;
  		}
  		if (!containsKey(theName)) {
  			put(theName, new ArrayList<List<? extends IQueryParameterType>>());
  		}
  
  		for (IQueryParameterOr<?> next : theAnd.getValuesAsQueryTokens()) {
  			if (next == null) {
  				continue;
  			}
  			get(theName).add(next.getValuesAsQueryTokens());
		}
  	}
  
  	public void add(String theName, IQueryParameterOr<?> theOr) {
  		if (theOr == null) {
  			return;
  		}
  		if (!containsKey(theName)) {
  			put(theName, new ArrayList<List<? extends IQueryParameterType>>());
  		}
 
 		get(theName).add(theOr.getValuesAsQueryTokens());
  	}
  
  	public void add(String theName, IQueryParameterType theParam) {
  		  
  		if (theParam == null) {
  			return;
  		}
  		if (!containsKey(theName)) {
  			put(theName, new ArrayList<List<? extends IQueryParameterType>>());
  		}
  		ArrayList<IQueryParameterType> list = new ArrayList<IQueryParameterType>();
  		list.add(theParam);
  		get(theName).add(list);
  	}
  
  	public void addInclude(Include theInclude) {
  		getIncludes().add(theInclude);
  	}
  
  	public void addRevInclude(Include theInclude) {
 		getRevIncludes().add(theInclude);
 	}
  	
  	public void setElements(Set<String> elements) {
  		this.elements = elements;
  	}
  	
  	public boolean hasElement(String name) {
  		return this.elements == null || this.elements.contains(name);
  	}
  	
 	public SummaryEnum getSummary() {
		return summary;
	}

	public void setSummary(SummaryEnum summary) {
		if (summary == null) summary = SummaryEnum.FALSE;
		this.summary = summary;
	}

	public Integer getCount() {
 		return myCount;
 	}
 	
 	public Set<String> getAllValues(String name) {
 		List<List<? extends IQueryParameterType>> allValues = get(name);
 		if (allValues == null) return null;
 		
 		Set<String> result = new HashSet<String>();
 		
 		for (List<? extends IQueryParameterType> orValues : allValues) {
 			for (IQueryParameterType val : orValues) {
 				if (val instanceof TokenParam) {
 					result.add(((TokenParam) val).getValue());
 				}
 				if (val instanceof StringParam) {
 					result.add(((StringParam) val).getValue());
 				}
 			}
 		}
 		
 		return result;
 	}

 
 	public Set<Include> getIncludes() {
 		if (myIncludes == null) {
 			myIncludes = new HashSet<Include>();
 		}
 		return myIncludes;
	}
 
 	/**
	 * Returns null if there is no last updated value
 	 */
 	public DateRangeParam getLastUpdated() {
 		if (myLastUpdated != null) {
			if (myLastUpdated.isEmpty()) {
 				myLastUpdated = null;
 			}
 		}
 		return myLastUpdated;
	}


 	public Set<Include> getRevIncludes() {
 		if (myRevIncludes == null) {
 			myRevIncludes = new HashSet<Include>();
 		}
 		return myRevIncludes;
 	}

	public SortSpec getSort() {
 		return mySort;
	}


 	public void setCount(Integer theCount) {
		myCount = theCount;
 	}
 	

	public void setIncludes(Set<Include> theIncludes) {
		myIncludes = theIncludes;
 	}

	public void setLastUpdated(DateRangeParam theLastUpdated) {
	  myLastUpdated = theLastUpdated;
    }
 
 	public void setRevIncludes(Set<Include> theRevIncludes) {
		myRevIncludes = theRevIncludes;
 	}
 
	public void setSort(SortSpec theSort) {
		mySort = theSort;
	}

}
