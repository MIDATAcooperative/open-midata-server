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

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import akka.stream.javadsl.Sink;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import play.libs.F.Either;
import play.libs.streams.Accumulator;
import play.mvc.BodyParser;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import utils.ErrorReporter;
import utils.access.EncryptedFileHandle;
import utils.access.RecordManager;
import utils.exceptions.AppException;

public class BinaryFileBodyParser implements BodyParser<EncryptedFileHandle> {

	protected final UploadUndoErrorHandler errors;
	
	@Inject
    public BinaryFileBodyParser(UploadUndoErrorHandler errorhandler) {
        super();
        this.errors = errorhandler;
    }
	 
	@Override
	public Accumulator<ByteString, Either<Result, EncryptedFileHandle>> apply(RequestHeader request) {
		final String contentType = request.contentType().orElse("application/octet-stream");
		final String filename = request.header("X-Filename").orElse("file");		
		
		final Sink<ByteString, InputStream> s1 = StreamConverters.asInputStream();
        final Sink<ByteString, CompletionStage<Either<Result,EncryptedFileHandle>>> p1 = s1.mapMaterializedValue(inputstream -> {
        	 return CompletableFuture.supplyAsync( () -> {  
        		 try {  
        			
        		    EncryptedFileHandle p = RecordManager.instance.addFile(inputstream, filename, contentType);
        		    errors.setFileToDeleteOnError(p);            		   
        		    return Either.Right(p);
        		 } catch (AppException e) {
        			 ErrorReporter.report("File Part Parser", null, e);            			 
        			 throw new NullPointerException();
        		 }
        	 });
        });
                
        return Accumulator.fromSink(p1);						
	}
	
}
