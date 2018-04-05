package utils.access;

import utils.exceptions.AppException;

public interface DBIterator<A> {

	public A next() throws AppException;
	
	public boolean hasNext() throws AppException;
	
}
