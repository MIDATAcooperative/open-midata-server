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

package utils.access.index;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import models.MidataId;

public abstract class BaseIndexKey<A,B> implements Comparable<A> {
	
	public abstract B toMatch();
	public abstract A copy();
	public abstract void fetchValue(A otherKey);
	public abstract boolean matches(B match);
	public abstract void writeObject(ObjectOutputStream s, A last) throws IOException;
	public abstract void readObject(ObjectInputStream s, A last) throws IOException, ClassNotFoundException;
	
}
