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

import java.util.List;

import models.enums.FilterRuleType;

/**
 * data model for a filter rule that is used to find participants for a study.
 * Currently not used.
 *
 */
public class FilterRule implements JsonSerializable {			
	public FilterRuleType type; //Does this filter select data records or members from the database?
	public String name; //Name of filter class implementation to use in order to evaluate this filter
    public List<Object> params; //Parameters to be used by filter class. Semantic depends on used class
	public String group; //Name of result group, selected records or members will belong to. Required for studies with multiple groups of participants.
	public MidataId aps; //APS to which this rule provides records 	
	public boolean negate;
}
