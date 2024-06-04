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

package models;

import java.util.Map;
import java.util.Set;

import models.enums.ConsentStatus;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class SubProjectGroupMember extends UserGroupMember {
    
    public Map<String, String> projectGroupMapping;
    
    public @NotMaterialized Study study;
    
    public static final @NotMaterialized Set<String> ALL = Sets.create("userGroup", "member", "entityType", "status", "user", "entityName", "startDate", "endDate", "role", "confirmedUntil", "projectGroupMapping", "study");

    public SubProjectGroupMember() {}
    
    public SubProjectGroupMember(Map<String, String> projectGroupMapping) {
        this.projectGroupMapping = projectGroupMapping;
    }
    
    public static Set<SubProjectGroupMember> getSubProjectsActiveByMember(Set<MidataId> members) throws InternalServerException {
        return Model.getAll(SubProjectGroupMember.class, collection, CMaps.map("member", members).map("status", ConsentStatus.ACTIVE), ALL);
    }
    
    public static SubProjectGroupMember getById(MidataId id) throws InternalServerException {
		return Model.get(SubProjectGroupMember.class, collection, CMaps.map("_id", id), ALL);
	}
        

}
