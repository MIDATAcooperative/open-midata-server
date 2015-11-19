package models.enums;

/**
 * the status of the contract that each MIDATA member needs to sign
 *
 */
public enum ContractStatus {
	
  /**
   * no action about the contract has been taken
   */
  NEW,
  
  /**
   * the contract has been printed and sent by MIDATA
   */
  PRINTED,
  
  /**
   * the contract has been signed and returned by the member
   */
  SIGNED
}
