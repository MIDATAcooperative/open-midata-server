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

package models;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

/**
 * Data model for terms of use, privacy policy, etc.
 *
 */
public class TermsOfUse extends Model {
	
	protected @NotMaterialized static final String collection = "termsofuse";
	
	/**
	 * constant for all fields of a TermsOfUse
	 */
	public @NotMaterialized final static Set<String> ALL = Sets.create("name", "version", "language", "createdAt", "creatorLogin", "creator", "title", "text");
	
	/**
	 * (internal) Name of TermsOfUse 
	 */
	public String name;
	
	/**
	 * version of document
	 */
	public String version;
	
	/**
	 * language code of document
	 */
	public String language;
	
	/**
	 * time of creation
	 */
	public Date createdAt;
	
	/**
	 * person who uploaded TermsOfUse (email)
	 */
	public String creatorLogin;
	
	/**
	 * person who uploaded TermsOfUse (id)
	 */
	public MidataId creator;
	
	/**
	 * title of TermsOfUse to be displayed to the user
	 */
	public String title;
	
	/**
	 * html text of TermsOfUse
	 */
	public String text;
	
	public static TermsOfUse getByNameVersionLanguage(String name, String version, String language) throws InternalServerException {
		return Model.get(TermsOfUse.class, collection, CMaps.map("name", name).map("version", version).map("language", language), ALL);
	}
	
	public static Set<TermsOfUse> getAllByName(String name) throws InternalServerException {
		return Model.getAll(TermsOfUse.class, collection, CMaps.map("name", name), ALL);
	}
	
	public static Set<TermsOfUse> getAll(Map<String, Object> properties,  Set<String> fields) throws InternalServerException {
		return Model.getAll(TermsOfUse.class, collection, properties, fields);
	}
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);		
	}

	public void upsert() throws InternalServerException {
		Model.upsert(collection, this);		
	}

}
