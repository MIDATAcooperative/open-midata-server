package models.enums;

/**
 * aggregation levels for summary queries
 *
 */
public enum AggregationType {
	
   /**
    * all records are summarized into one entry
    */
   ALL,
   
   /**
    * all records of the same group are summarized into one entry
    */
   GROUP,
   
   /**
    * all records with the same format are summarized into one entry
    */
   FORMAT,
   
   /**
    * all records with the same content type are summarized into one entry
    */
   CONTENT,
   
   /**
    * all records with the same content type, format and group are summarized into one entry
    */
   SINGLE   
}
