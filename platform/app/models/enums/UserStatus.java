package models.enums;

/**
 * User account status
 *
 */
public enum UserStatus {
  /**
   * This is a new user account. It is not fully usable yet.
   */
  NEW,
  
  /**
   * This user account has been validated and may be used.
   */
  ACTIVE,
  
  /**
   * This user account has been blocked.
   */
  BLOCKED,
  
  /**
   * THis user account has been deleted.
   */
  DELETED
}
