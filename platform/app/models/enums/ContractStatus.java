/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

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
   * the contract has been requested to be sent by the user
   */
  REQUESTED,
  
  /**
   * the contract has been printed and sent by MIDATA
   */
  PRINTED,
  
  /**
   * the contract has been signed and returned by the member
   */
  SIGNED
}
