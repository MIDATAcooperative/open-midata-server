package utils.rules;

import java.util.List;

import models.ModelException;
import models.Record;

public class FormatRule implements Rule {

	@Override
	public boolean qualifies(Record record, List<Object> params) throws ModelException {
		if (record.format == null) return false;
		for (Object obj : params) {
			if (record.format.equals(obj)) return true;
		}
		return false;
	}

}
