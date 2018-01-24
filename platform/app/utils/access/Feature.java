package utils.access;

import java.util.Iterator;
import java.util.List;

import utils.AccessLog;
import utils.exceptions.AppException;

/**
 * abstract super-class for additional features of the query engine
 *
 */
public abstract class Feature {
				
	/* Either query or iterator MUST be implemented by subclass ! */
	
	protected List<DBRecord> query(Query q) throws AppException {
		return ProcessingTools.collect(iterator(q));
	}
	
	protected Iterator<DBRecord> iterator(Query q) throws AppException {
		List<DBRecord> result = query(q);
		return result.iterator();
	}
	
	
	public static abstract class MultiIterator<A,B> implements Iterator<A> {

		protected Iterator<B> chain;
		protected Iterator<A> current;		
		
		@Override
		public boolean hasNext() {
			return current != null && current.hasNext();
		}

		@Override
		public A next() {
			try {			
			  A result = current.next();			
			  advance();			
			  return result;
			} catch (AppException e) {
				throw new RuntimeException(e);
			}
		}
		
		public void init(Iterator<B> init) throws AppException {
		  this.chain = init;
		  if (chain.hasNext()) {
			  B next = chain.next();
			  current = advance(next);
		  }
		  advance();
		}
		
		public void advance() throws AppException {
			while (!current.hasNext() && chain.hasNext()) {				
				B next = chain.next();
				current = advance(next);
			}            
		}
		
		public abstract Iterator<A> advance(B next) throws AppException;
		
		
		
	}
	
	public static abstract class MultiSource<B> extends MultiIterator<DBRecord, B> {
		protected Query query;						
		
		@Override
		public DBRecord next() {
			try {
			  
			  DBRecord result = current.next();
			  DBRecord from = query.getFromRecord();
			  if (from != null && result._id.equals(from._id)) {				
				  query.setFromRecord(null);
			  }
			  advance();			
			  return result;
			} catch (AppException e) {
				throw new RuntimeException(e);
			}
		}				
		
	}
					
}
