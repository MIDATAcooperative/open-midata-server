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
    * all records with the same content type and owner are summarized into one entry
    */
   CONTENT_PER_OWNER,
   
   /**
    * all records with the same content type and app are summarized into one entry
    */
   CONTENT_PER_APP,
   
   /**
    * all records with the same content type, format, group and owner are summarized into one entry
    */
   SINGLE   
}
