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

package utils.exceptions;

import models.MidataId;

public class PluginException extends AppException {

	private static final long serialVersionUID = 1L;
	
	private MidataId pluginId;

	public PluginException(String localeKey, Throwable cause) {
		super(localeKey, cause);
	}
	
	public PluginException(MidataId pluginId, String localeKey, String msg) {
		super(localeKey, msg);
		this.pluginId = pluginId;
	}

	public MidataId getPluginId() {
		return pluginId;
	}

}
