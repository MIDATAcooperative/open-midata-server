package utils.rules;

import java.util.List;
import java.util.Map;

import utils.exceptions.ModelException;

import models.Record;

public interface Rule {
	public boolean qualifies(Record record, List<Object> params) throws ModelException;
	public void setup(Map<String, Object> query, List<Object> params) throws ModelException;
	public void merge(List<Object> params, List<Object> params2) throws ModelException;
}
