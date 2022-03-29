/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.largerequests;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Provider;

import com.typesafe.config.Config;

import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import utils.access.EncryptedFileHandle;

public class UploadUndoErrorHandler extends DefaultHttpErrorHandler  {

	
	private volatile EncryptedFileHandle fileToDeleteOnError;
		
	
	public EncryptedFileHandle getFileToDeleteOnError() {
		return fileToDeleteOnError;
	}

	public void setFileToDeleteOnError(EncryptedFileHandle fileToDeleteOnError) {
		this.fileToDeleteOnError = fileToDeleteOnError;
	}

	
	
	@Override
	public CompletionStage<Result> onClientError(RequestHeader request, int statusCode, String message) {
		if (fileToDeleteOnError != null) fileToDeleteOnError.removeAfterFailure();	
		return super.onClientError(request, statusCode, message);
	}

	@Override
	public CompletionStage<Result> onServerError(RequestHeader request, Throwable exception) {
		if (fileToDeleteOnError != null) fileToDeleteOnError.removeAfterFailure();
		return super.onServerError(request, exception);
	}

	@Inject
	public UploadUndoErrorHandler(Config config, Environment environment, OptionalSourceMapper sourceMapper, Provider<Router> routes) {
		super(config, environment, sourceMapper, routes);
	}
		

}