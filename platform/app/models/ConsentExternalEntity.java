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

public class ConsentExternalEntity implements JsonSerializable {

	public String name;
	
	public String getFirstname() {
		if (name == null) return "";
	    int i = name.lastIndexOf(' ');
	    if (i>0) return name.substring(0, i); else return "";		
	}
	
	public String getLastname() {
		if (name == null) return "";
	    int i = name.lastIndexOf(' ');
	    if (i>0) return name.substring(i+1); else return name;		
	}
	
	public String getName() {
		if (name == null) return "";
	    return name;		
	}
	
}
