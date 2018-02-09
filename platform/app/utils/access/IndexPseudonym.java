package utils.access;

import java.io.Serializable;

public class IndexPseudonym implements Serializable {
  
   private static final long serialVersionUID = 5612134842436725319L;
   
   private String pseudonym;
   private byte[] key;
   
   IndexPseudonym(String pseudonym, byte[] key) {
	   this.pseudonym = pseudonym;
	   this.key = key;
   }
   
   public String getPseudonym() {
	   return pseudonym;
   }
   
   public byte[] getKey() {
	   return key;
   }
   
}
