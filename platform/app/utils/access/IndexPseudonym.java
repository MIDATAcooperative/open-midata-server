package utils.access;

public class IndexPseudonym {
   private String pseudonym;
   private byte[] key;
   
   IndexPseudonym(String pseudonym, byte[] key) {
	   this.pseudonym = pseudonym;
	   this.key = key;
   }
   
   protected String getPseudonym() {
	   return pseudonym;
   }
   
   protected byte[] getKey() {
	   return key;
   }
   
}
