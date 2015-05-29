package models.enums;

public enum APSSecurityLevel {
    NONE,   // APS Not encrypted, Records not encrypted
    LOW,    // APS encrypted, Records not encrypted    
    HIGH    // APS encrypted, Records encrypted
}
