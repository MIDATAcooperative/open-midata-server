package models.enums;

public enum APSSecurityLevel {
    NONE,   // APS Not encrypted, Records not encrypted
    LOW,    // APS encrypted, Records not encrypted
    MEDIUM, // APS encrypted, all Records encrypted with same key
    HIGH    // APS encrypted, Records encrypted
}
