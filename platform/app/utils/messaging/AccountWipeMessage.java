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

package utils.messaging;

import java.io.Serializable;

import models.MidataId;

/**
 * message about account deletion
 * @author alexander
 *
 */
public class AccountWipeMessage implements Serializable {

	 /**
	 * 
	 */
	 private static final long serialVersionUID = -4421491319018937727L;
	
	 private final String handle;  
	 private MidataId accountToWipe;
	 private MidataId executorId;
	 private MidataId audit;
	 private int phase;
	 
	 public AccountWipeMessage(MidataId accountToWipe, String handle, MidataId executorId, int phase, MidataId audit) {
		 this.handle = handle;
		 this.accountToWipe = accountToWipe;
		 this.executorId = executorId;
		 this.phase = phase;
		 this.audit = audit;
	 }

	public String getHandle() {
		return handle;
	}

	public MidataId getAccountToWipe() {
		return accountToWipe;
	}

	public MidataId getExecutorId() {
		return executorId;
	}

	public int getPhase() {
		return phase;
	}
	
	public MidataId getAudit() {
		return audit;
	}	 	 
	 
}
