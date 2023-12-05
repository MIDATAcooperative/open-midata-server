/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

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
   * This user accounts validation took longer than timeout period. It is 'blocked' until process is finished.
   */
  TIMEOUT,
  
  /**
   * This user account has been blocked.
   */
  BLOCKED,
  
  /**
   * This user account has been deleted.
   */
  DELETED,
  
  /**
   * This user account has been marked as fake account
   */
  FAKE,
  
  /**
   * This user account has been deleted and access keys have been destroyed
   */
  WIPED;
  
  public boolean isDeleted() {
	  return this == DELETED || this == WIPED || this == FAKE;
  }
}
