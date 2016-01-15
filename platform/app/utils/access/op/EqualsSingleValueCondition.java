package utils.access.op;

public class EqualsSingleValueCondition implements Condition {

	private Object val;
	
	public EqualsSingleValueCondition(Object val) {
		this.val = val;
	}

	@Override
	public boolean satisfiedBy(Object obj) {
		return val.equals(obj);
	}
	
	
}
