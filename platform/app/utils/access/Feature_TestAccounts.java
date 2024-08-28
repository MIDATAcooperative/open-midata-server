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

import java.util.Date;

import org.bson.BasicBSONObject;

import controllers.Circles;
import utils.AccessLog;
import utils.TestAccountTools;
import utils.context.AccessContext;
import utils.context.ConsentAccessContext;
import utils.exceptions.AppException;

public class Feature_TestAccounts extends Feature {

	private Feature next;
	
	public Feature_TestAccounts(Feature next) {
		this.next = next;
	}

	@Override
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		AccessContext context = q.getContext();
		if (context instanceof ConsentAccessContext) {
			ConsentAccessContext c = (ConsentAccessContext) context;
			if (c.getConsent().testUserApp != null) {
				if (!TestAccountTools.doesAcceptTestUsers(c, c.getConsent().testUserApp)) return ProcessingTools.empty();
			}
		}
		return next.iterator(q);
	}
	
	
}
