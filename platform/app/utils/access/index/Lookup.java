package utils.access.index;

import utils.access.op.Condition;

public class Lookup extends BaseLookup<IndexKey> {

	private Condition[] condition;
	
	public Lookup(Condition[] condition) {
		this.condition = condition;
	}
	
	public boolean conditionCompare(IndexKey inkey) {
		Comparable[] idxKey = inkey.getKey();
		for (int i=0;i<condition.length;i++) {
			if (!condition[i].satisfiedBy(idxKey[i])) return false;
		}		
		return true;
	}
	
	public boolean conditionCompare(IndexKey lk, IndexKey hk) {
		Comparable[] lowkey = lk == null ? null :lk.getKey();
		Comparable[] highkey = hk == null ? null : hk.getKey();
		for (int i=0;i<condition.length;i++) {
			if (!condition[i].isInBounds(lowkey==null ? null : lowkey[i],  highkey == null ? null: highkey[i]))  return false;
		}		
		return true;
	}
}
