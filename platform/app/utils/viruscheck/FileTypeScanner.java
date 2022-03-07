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

package utils.viruscheck;

import java.util.HashMap;
import java.util.Map;

import utils.AccessLog;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;

public class FileTypeScanner {

	private Map<String, String> extensions;
	
	public static FileTypeScanner instance = new FileTypeScanner();
	
	public static FileTypeScanner getInstance() {
		return instance;
	}
	
	private void r(String mt, String ext) {
		extensions.put(ext, mt);
	}
	public FileTypeScanner() {
		extensions = new HashMap<String, String>();
		r("application/gzip","gz");
		r("application/json","json");
		r("application/msexcel","xls");
		r("application/msexcel","xla");
		r("application/mspowerpoint","ppt");
		r("application/mspowerpoint","ppz");
		r("application/mspowerpoint","pps");
		r("application/mspowerpoint","pot");
		r("application/msword","doc");
		r("application/msword","dot");
		r("application/octet-stream","file");
		r("application/octet-stream","ini");
		r("application/pdf","pdf");
		r("application/postscript","ai");
		r("application/postscript","eps");
		r("application/postscript","ps");
		r("application/rtc","rtc");
		r("application/rtf","rtf");
		r("vnd.openxmlformats-officedocument","xlsx");
		r("vnd.openxmlformats-officedocument","docx");
		r("application/xml","xml");
		r("application/x-compress","z");
		r("application/x-dvi","dvi");
		r("application/x-gtar","gtar");
		r("application/x-tar","tar");
		r("application/x-tex","tex");
		r("application/zip","zip");
		r("audio/basic","au");
		r("audio/basic","snd");
		r("audio/mpeg","mp3");
		r("audio/mp4","mp4");
		r("audio/voxware","vox");
		r("audio/wav","wav");
		r("audio/x-aiff","aif");
		r("audio/x-aiff","aiff");
		r("audio/x-aiff","aifc");
		r("audio/x-dspeeh","dus");
		r("audio/x-dspeeh","cht");
		r("audio/x-midi","mid");
		r("audio/x-midi","midi");
		r("audio/x-mpeg","mp2");
		r("audio/x-pn-realaudio","ram");
		r("audio/x-pn-realaudio","ra");
		r("image/","bmp");
		r("image/gif","gif");
		r("image/jpeg","jpeg");
		r("image/jpeg","jpg");
		r("image/jpeg","jpe");
		r("image/png","png");
		r("image/svg+xml","svg");
		r("image/tiff","tiff");
		r("image/tiff","tif");
		r("image/x-icon","ico");
		r("image/x-rgb","rgb");
		r("model/vrml","wrl");
		r("text/comma-separated-values","csv");
		r("text/plain","txt");
		r("text/richtext","rtx");
		r("text/rtf","rtf");
		r("text/tab-separated-values","tsv");
		r("text/xml","xml");
		r("video/mpeg","mpeg");
		r("video/mpeg","mpg");
		r("video/mpeg","mpe");
		r("video/mp4","mp4");
		r("video/quicktime","qt");
		r("video/quicktime","mov");
		r("video/x-msvideo","avi");
		r("video/x-sgi-movie","movie");
		
		r("application/vnd.stardivision.chart","sds");
		r("application/vnd.stardivision.calc","sdc");
		r("application/vnd.stardivision.writer","sdw");
		r("application/vnd.stardivision.writer-global","sgl");
		r("application/vnd.stardivision.draw","sda");
		r("application/vnd.stardivision.impress","sdd");
		r("application/vnd.stardivision.math","sdf");
		r("application/vnd.sun.xml.writer","sxw");
		r("application/vnd.sun.xml.writer.template","stw");
		r("application/vnd.sun.xml.writer.global","sxg");
		r("application/vnd.sun.xml.calc","sxc");
		r("application/vnd.sun.xml.calc.template","stc");
		r("application/vnd.sun.xml.impress","sxi");
		r("application/vnd.sun.xml.impress.template","sti");
		r("application/vnd.sun.xml.draw","sxd");
		r("application/vnd.sun.xml.draw.template","std");
		r("application/vnd.sun.xml.math","sxm");
		r("application/vnd.oasis.opendocument.text","odt");
		r("application/vnd.oasis.opendocument.text-template","ott");
		r("application/vnd.oasis.opendocument.text-web","oth");
		r("application/vnd.oasis.opendocument.text-master","odm");
		r("application/vnd.oasis.opendocument.graphics","odg");
		r("application/vnd.oasis.opendocument.graphics-template","otg");
		r("application/vnd.oasis.opendocument.presentation","odp");
		r("application/vnd.oasis.opendocument.presentation-template","otp");
		r("application/vnd.oasis.opendocument.spreadsheet","ods");
		r("application/vnd.oasis.opendocument.spreadsheet-template","ots");
		r("application/vnd.oasis.opendocument.chart","odc");
		r("application/vnd.oasis.opendocument.formula","odf");
		r("application/vnd.oasis.opendocument.database","odb");
		r("application/vnd.oasis.opendocument.image","odi");
	}
	
	public boolean isValidFile(String filename, String mimeType) throws AppException {		
		if (filename.length() > 255) throw new BadRequestException("error.invalid.filename", "Filename too long.");
		
		AccessLog.log("check ", filename, " mimeType=", mimeType);
		int p = filename.lastIndexOf('.');
		
		if (p>=0) {
			String extension = filename.substring(p+1).toLowerCase();			
			String mt = extensions.get(extension);			
			if (mt == null) throw new BadRequestException("error.invalid.content", "File type not supported.");			
						
			if (!mimeType.toLowerCase().startsWith(mt)) throw new BadRequestException("error.invalid.content", "File type not supported. ext="+extension+" mt="+mimeType);
		} else throw new InternalServerException("error.invalid.content", "File type not supported. file="+filename+" mt="+mimeType);
		
		return true;
	}
}
