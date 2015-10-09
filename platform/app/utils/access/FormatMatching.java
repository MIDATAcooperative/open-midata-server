package utils.access;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import utils.exceptions.ModelException;


public class FormatMatching {
  
	class DetailFormat {
		String format;
		String subformat;
		String syntax;
		String subsyntax;
		
		DetailFormat(Matcher m) {
		  this.format = m.group(1);
		  this.subformat = m.group(2);
		  this.syntax = m.group(3);
		  this.subsyntax = m.group(4);
		}
		
		boolean matches(DetailFormat other) {
			if (!format.equals("*") && !format.equals(other.format)) return false;
			if (!syntax.equals("*") && !syntax.equals(other.syntax)) return false;			
			if (subformat!=null && other.subformat!=null && !subformat.equals("*") && !subformat.equals(other.format)) return false;			
			if (subsyntax!=null && other.subsyntax!=null && !subsyntax.equals("*") && !subsyntax.equals(other.syntax)) return false;
			return true;
		}
	}
	private Set<String> acceptedFormats;
	private List<DetailFormat> acceptedWildcards;
	private static Pattern resolve = Pattern.compile("([\\w\\-\\*]+)(/[\\w\\-\\*]+)?\\[([\\w\\-\\*]+)(/[\\w\\-\\*]+)?\\]");
	
	
	public FormatMatching(Set<String> acceptedFormats) throws ModelException {
		this.acceptedFormats = acceptedFormats;
		for (String ac : acceptedFormats) {
			if (ac.contains("*")) {
				if (acceptedWildcards == null) acceptedWildcards = new ArrayList<DetailFormat>();
				Matcher matcher = resolve.matcher(ac);
				if (!matcher.matches()) throw new ModelException("error.internal", "Bad format expression: "+ac);
				acceptedWildcards.add(new DetailFormat(matcher));
			}
		}
	}
	
	public boolean isSimple() {
		return acceptedWildcards == null;
	}
	
	public boolean matches(String format) throws ModelException {
		if (acceptedFormats.contains(format)) return true;
		if (acceptedWildcards != null) {
			Matcher matcher = resolve.matcher(format);
			if (!matcher.matches()) throw new ModelException("error.internal", "Bad format:"+format);
			for (DetailFormat df : acceptedWildcards)
				if (df.matches(new DetailFormat(matcher))) return true;
		}
		return false;
	}
}
