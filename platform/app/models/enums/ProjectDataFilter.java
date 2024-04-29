package models.enums;

public enum ProjectDataFilter {

    /**
     * Remove all time information from resources
     */
    NO_TIME,
    
    /**
     * Remove time and day information from resources. Keep only year and month.
     */
    ONLY_MONTH_YEAR,
    
    /**
     * Remove all practitioner references
     */
    NO_PRACTITIONER,
    
    /**
     * Remove all narratives
     */
    NO_NARRATIVES,
    
}
