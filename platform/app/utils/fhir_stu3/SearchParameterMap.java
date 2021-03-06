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

package utils.fhir_stu3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ca.uhn.fhir.model.api.IQueryParameterAnd;
import ca.uhn.fhir.model.api.IQueryParameterOr;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortOrderEnum;
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
  	private Integer skip;  	
  	private String from;

  	
  	private Set<Include> myIncludes;
  	private DateRangeParam myLastUpdated;  	
  	private Set<Include> myRevIncludes;
  	private SortSpec mySort;
  	private Set<String> elements;
  	private SummaryEnum summary = SummaryEnum.FALSE;
    private String content;	    
    
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
 		if (theCount == null) theCount = 2000;
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
	
	public SortOrderEnum hasSortParam(String name) {
		SortSpec spec = mySort;
		while (spec != null) {
			if (spec.getParamName().equals(name)) return spec.getOrder();
			spec = spec.getChain();
		}
		return null;		
	}
	
	public String[] getSortNames() {
		if (mySort == null) return null;
		List<String> sorts = new ArrayList();
		SortSpec spec = mySort;
		while (spec != null) {
			sorts.add(spec.getParamName());
			spec = spec.getChain();
		}
		return sorts.toArray(new String[sorts.size()]);
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Integer getSkip() {
		return skip;
	}

	public void setSkip(Integer skip) {
		this.skip = skip;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;		
	}

	
	
	
	

}
