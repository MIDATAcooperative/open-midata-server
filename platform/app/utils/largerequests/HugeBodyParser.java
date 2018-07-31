package utils.largerequests;

import akka.stream.IOResult;
import akka.stream.Materializer;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import models.MidataId;
import play.core.parsers.Multipart;
import play.http.DefaultHttpErrorHandler;
import play.libs.streams.Accumulator;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import scala.Option;
import utils.ErrorReporter;
import utils.access.EncryptedFileHandle;
import utils.access.RecordManager;
import utils.exceptions.AppException;

import javax.inject.Inject;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.lang3.tuple.Pair;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

public class HugeBodyParser extends DelegatingMultipartFormDataBodyParser<EncryptedFileHandle> {

	
	
    @Inject
    public HugeBodyParser(Materializer materializer, play.api.http.HttpConfiguration config, UploadUndoErrorHandler errorhandler) {
        super(materializer, config.parser().maxMemoryBuffer(), errorhandler);       
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
