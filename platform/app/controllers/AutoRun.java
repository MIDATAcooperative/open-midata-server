package controllers;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.types.ObjectId;

import models.Admin;
import models.Consent;
import models.Plugin;
import models.Space;
import models.User;
import play.Play;
import play.libs.F;
import play.libs.F.Callback;
import play.libs.F.Function;
import play.mvc.Result;
import utils.access.AccessLog;
import utils.access.RecordManager;
import utils.auth.KeyManager;
import utils.auth.SpaceToken;
import utils.collections.CMaps;
import utils.collections.ChainedMap;
import utils.collections.Sets;
import utils.exceptions.AppException;

public class AutoRun extends APIController {

	public static Result run() throws AppException {
		final String nodepath = Play.application().configuration().getString("node.path");
		final String visPath = Play.application().configuration().getString("visualizations.path");
		
		User autorunner = Admin.getByEmail("autorun-service", Sets.create("_id"));
		KeyManager.instance.unlock(autorunner._id, null);
		Set<Space> autoImports = Space.getAll(CMaps.map("autoImport", true), Sets.create("_id", "owner", "visualization"));
		try {
			for (Space space : autoImports) {
	
				final Plugin plugin = Plugin.getById(space.visualization, Sets.create("type", "filename", "name", "authorizationUrl", "scopeParameters", "accessTokenUrl", "consumerKey", "consumerSecret"));
				SpaceToken token = new SpaceToken(space._id, space.owner, null, null, autorunner._id);
				final String tokenstr = token.encrypt();
	
				if (plugin.type != null && plugin.type.equals("oauth2")) {
					BSONObject oauthmeta = RecordManager.instance.getMeta(autorunner._id, space._id, "_oauth");
					if (oauthmeta != null) {
						if (oauthmeta.containsField("refreshToken")) {						
	                        Thread.sleep(2000);
				
							Plugins.requestAccessTokenOAuth2FromRefreshToken(autorunner._id, plugin, space._id.toString(), oauthmeta.toMap()).onRedeem(new Callback<Boolean>() {
								public void invoke(Boolean success) throws AppException, IOException {
									AccessLog.logDB("Auth:"+success);
									if (success) {
										AccessLog.debug(nodepath+" "+visPath+"/"+plugin.filename+"/server.js"+" "+tokenstr);
										Process p = new ProcessBuilder(nodepath, visPath+"/"+plugin.filename+"/server.js", tokenstr).inheritIO().start();
									}
								}
							});
	
						}
					}
				}
	
			}
		} catch (InterruptedException e) {}
		return ok();

	}
}
