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
