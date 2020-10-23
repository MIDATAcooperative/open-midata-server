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

package models;

import java.util.Set;

import models.enums.ConsentType;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.InternalServerException;

/**
 * data model for consents that are created if a healthcare provider wants to share data with a MIDATA member.
 * Needed for records like surveys that do not contain data of the patient. 
 *
 */
public class HCRelated extends Consent {

	public HCRelated() {
		this.type = ConsentType.HCRELATED;
	}
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);	
	}
	
	public static Set<HCRelated> getByAuthorizedAndOwner(MidataId memberId, MidataId hcId) throws InternalServerException {
		return Model.getAll(HCRelated.class, collection, CMaps.map("authorized", memberId).map("owner", hcId).map("type", ConsentType.HCRELATED).map("status", NOT_DELETED), Sets.create("name", "owner"));
	}
}
