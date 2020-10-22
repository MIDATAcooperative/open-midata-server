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

package utils.access;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class DuplicateEliminator<A> implements Iterator<A> {

	private Set<A> encountered;
	private A next;
	private Iterator<A> chain;
	
	public DuplicateEliminator(Iterator<A> chain) {
		this.chain = chain;
		if (chain.hasNext()) {
			next = chain.next();
			if (chain.hasNext()) {
				encountered = new HashSet<A>();
				encountered.add(next);
			}
		}		
	}
	
	@Override
	public boolean hasNext() {
		return next != null;
	}

	@Override
	public A next() {
		A result = next;
		boolean first = false;
		if (!first && chain.hasNext()) {
			next = chain.next();
			first = encountered.add(next);
		}		
		return result;
	}

}
