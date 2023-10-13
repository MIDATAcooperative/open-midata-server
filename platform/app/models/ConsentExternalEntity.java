package models;

public class ConsentExternalEntity implements JsonSerializable {

	public String name;
	
	public String getFirstname() {
		if (name == null) return "";
	    int i = name.lastIndexOf(' ');
	    if (i>0) return name.substring(0, i); else return "";		
	}
	
	public String getLastname() {
		if (name == null) return "";
	    int i = name.lastIndexOf(' ');
	    if (i>0) return name.substring(i+1); else return name;		
	}
	
	public String getName() {
		if (name == null) return "";
	    return name;		
	}
	
}
