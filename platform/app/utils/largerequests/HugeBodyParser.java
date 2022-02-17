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
import java.util.function.Function;

import javax.inject.Inject;

import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import play.core.parsers.Multipart;
import play.libs.streams.Accumulator;
import play.mvc.BodyParser.DelegatingMultipartFormDataBodyParser;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData.FilePart;
import utils.ErrorReporter;
import utils.access.EncryptedFileHandle;
import utils.access.RecordManager;
import utils.exceptions.AppException;

public class HugeBodyParser extends DelegatingMultipartFormDataBodyParser<EncryptedFileHandle> {

	private final UploadUndoErrorHandler errors;
	
    @Inject
    public HugeBodyParser(Materializer materializer, play.api.http.HttpConfiguration config, UploadUndoErrorHandler errorhandler) {
        super(materializer, config.parser().maxMemoryBuffer(), config.parser().maxDiskBuffer(), errorhandler);
        errors =  errorhandler;       
    }

    /**
     * Creates a file part handler that uses a custom accumulator.
     */
    @Override
    public Function<Multipart.FileInfo, Accumulator<ByteString, Http.MultipartFormData.FilePart<EncryptedFileHandle>>> createFilePartHandler() {
        return (Multipart.FileInfo fileInfo) -> {
            final String filename = fileInfo.fileName();
            final String partname = fileInfo.partName();
            final String contentType = fileInfo.contentType().getOrElse(null);
            System.out.println("filename="+filename);
            //final File file = g8enerateTempFile();

            final Sink<ByteString, InputStream> s1 = StreamConverters.asInputStream();
            final Sink<ByteString, CompletionStage<FilePart<EncryptedFileHandle>>> p1 = s1.mapMaterializedValue(inputstream -> {
            	 return CompletableFuture.supplyAsync( () -> {  
            		 try {  
            			
            		    EncryptedFileHandle p = RecordManager.instance.addFile(inputstream, filename, contentType);
            		    errors.setFileToDeleteOnError(p);            		   
            		    return new Http.MultipartFormData.FilePart(partname,
                           filename,
                           contentType,
                           p);
            		 } catch (AppException e) {
            			 ErrorReporter.report("File Part Parser", null, e);            			 
            			 throw new NullPointerException();
            		 }
            	 });
            });
           
            
     
            return Accumulator.fromSink(p1);
                    
            		
            	
        };
    }
  
}
