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
