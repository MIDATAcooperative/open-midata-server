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
	
	protected DBIterator<DBRecord> iterator(Query q) throws AppException {
		//throw new NullPointerException();
		List<DBRecord> result = query(q);
		return ProcessingTools.dbiterator("old-iterator",result.iterator());
	}
	
	
	public static abstract class MultiIterator<A,B> implements DBIterator<A> {

		protected DBIterator<B> chain;
		protected DBIterator<A> current;	
		protected int passed;
		
		@Override
		public boolean hasNext() throws AppException {
			return current != null && current.hasNext();
		}

		@Override
		public A next() throws AppException {
					
			  A result = current.next();
			  
			  if (result == null) throw new NullPointerException();
			  
			  advance();	
			  passed++;
			  return result;
			
		}
		
		public void init(Iterator<B> init) throws AppException {
			init(ProcessingTools.dbiterator("", init));
		}
		
		public void init(DBIterator<B> init) throws AppException {
		  this.chain = init;
		  if (chain.hasNext()) {
			  B next = chain.next();
			  current = advance(next);
			  //AccessLog.log("init:"+this.toString());
		  } else current = ProcessingTools.empty();
		  advance();
		}
		
		public void init(B first, Iterator<B> init) throws AppException {
			init(first, ProcessingTools.dbiterator("", init));
		}
		
		public void init(B first, DBIterator<B> init) throws AppException {
			  this.chain = init;
			  current = advance(first);
			  //AccessLog.log("init:"+this.toString());			  
			  advance();
		}
		
		public void advance() throws AppException {
			while (!current.hasNext() && chain.hasNext()) {				
				B next = chain.next();
				current = advance(next);
				//if (current.hasNext()) AccessLog.log("advance:"+this.toString());
			}  			
		}
		
		public abstract DBIterator<A> advance(B next) throws AppException;
		
		
		
	}
	
	public static abstract class MultiSource<B> extends MultiIterator<DBRecord, B> {
		protected Query query;						
		
		@Override
		public DBRecord next() throws AppException {
	
			  
			  DBRecord result = current.next();
			  DBRecord from = query.getFromRecord();
			  if (from != null && result._id.equals(from._id)) {				
				  query.setFromRecord(null);
			  }
			  advance();	
			  passed++;
			  return result;
			
		}				
		
	}
					
}
