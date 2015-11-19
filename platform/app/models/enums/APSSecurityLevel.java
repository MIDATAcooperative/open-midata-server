package models.enums;

/**
 * security level for an access permission set
 *
 */
public enum APSSecurityLevel {
	
	/**
	 * APS Not encrypted, records not encrypted
	 */
    NONE,  
    
    /**
     * APS encrypted, records not encrypted
     */
    LOW,   
    
    /**
     * APS encrypted, all records encrypted with same key
     */
    MEDIUM, 
    
    /**
     * APS encrypted, each records encrypted with own key
     */
    HIGH   
}
