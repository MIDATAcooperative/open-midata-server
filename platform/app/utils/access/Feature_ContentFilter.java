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

package utils.access;

import utils.exceptions.AppException;

/**
 * filter records by format
 *
 */
public class Feature_ContentFilter extends Feature {

	private Feature next;
	
	public Feature_ContentFilter(Feature next) {
		this.next = next;
	}
	
	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		DBIterator<DBRecord> chain = next.iterator(q);
		
		if (q.restrictedBy("format")) chain = new ProcessingTools.FilterByMetaSet(chain, "format", q.getRestrictionOrNull("format"), false);
		if (q.restrictedBy("content")) chain = new ProcessingTools.FilterByMetaSet(chain, "content", q.getRestrictionOrNull("content"), false);
		if (q.restrictedBy("app")) chain = new ProcessingTools.FilterByMetaSet(chain, "app", q.getIdRestrictionDB("app"), false);	
		if (q.restrictedBy("public")) {
			String mode = q.getStringRestriction("public");
			
			// TODO Please remove once ally science is setup correctly
			if (mode.equals("only")) mode = "also";
			// END Remove
			
			if (mode.equals("only")) chain = new ProcessingTools.FilterByTag(chain, "security:public", true);
		} else {
			chain = new ProcessingTools.FilterByTag(chain, "security:public", false);			
		}
		
		return chain;
	}
		
}
