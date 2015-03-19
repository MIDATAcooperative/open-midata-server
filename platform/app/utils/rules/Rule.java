package utils.rules;

import java.util.List;

import models.ModelException;
import models.Record;

public interface Rule {
	public boolean qualifies(Record record, List<Object> params) throws ModelException;		
}
