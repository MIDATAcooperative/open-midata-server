package utils.access.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.MidataId;
import models.Model;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * Data model of an index definition 
 *
 */
public class IndexDefinition extends IndexPageModel {
    public @NotMaterialized static final Set<String> ALL = Sets.create("owner", "formats", "fields", "enc", "version");
	
	
	public String owner;
	
		
	/**
	 * For which record formats does this index apply?
	 */
	public List<String> formats;
	
	/**
	 * Which fields are included in the index?
	 */
	public List<String> fields;
	
	private @NotMaterialized List<String[]> fieldsSplit;
	
	public List<String[]> getFieldsSplit() {
		if (fieldsSplit == null) {
		  fieldsSplit = new ArrayList<String[]>(fields.size());
		  for (String f : fields) fieldsSplit.add(f.split("\\|"));
		}
		return fieldsSplit;
	}
		
	public static Set<IndexDefinition> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws InternalServerException {
		return Model.getAll(IndexDefinition.class, collection, properties, fields);
	}
	
	public static void delete(MidataId id) throws InternalServerException {		
		Model.delete(IndexDefinition.class, collection, CMaps.map("_id", id));
	}
	
	public static IndexDefinition getById(MidataId pageId) throws InternalServerException {
		return Model.get(IndexDefinition.class, collection, CMaps.map("_id", pageId), ALL);
	}
		
}
