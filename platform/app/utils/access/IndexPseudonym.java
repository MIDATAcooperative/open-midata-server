package utils.access;

public class IndexPseudonym {
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
