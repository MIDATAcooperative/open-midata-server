package utils.access.index;

public abstract class BaseLookup<A> {

	public abstract boolean conditionCompare(A inkey);
	
	public abstract boolean conditionCompare(A lk, A hk);
}
