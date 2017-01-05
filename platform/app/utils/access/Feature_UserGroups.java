package utils.access;

import java.util.List;

import utils.exceptions.AppException;

public class Feature_UserGroups extends Feature {

	private Feature next;

	public Feature_UserGroups(Feature next) {
		this.next = next;
	}
	
	@Override
	protected List<DBRecord> query(Query q) throws AppException {
		// TODO Auto-generated method stub
		return null;
	}

}
