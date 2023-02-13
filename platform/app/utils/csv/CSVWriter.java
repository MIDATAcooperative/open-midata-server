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

package utils.csv;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CSVWriter {

	 private static final String COMMA = ",";
	    private static final String DEFAULT_SEPARATOR = COMMA;
	    private static final String DOUBLE_QUOTES = "\"";
	    private static final String EMBEDDED_DOUBLE_QUOTES = "\"\"";
	    private static final String NEW_LINE_UNIX = "\n";
	    private static final String NEW_LINE_WINDOWS = "\r\n";
	    
	    public String convertToCsvFormat(final List<String> line) {
	        return convertToCsvFormat(line, DEFAULT_SEPARATOR);
	    }

	    public String convertToCsvFormat(final List<String> line, final String separator) {
	        return convertToCsvFormat(line, separator, true);
	    }

	    // if quote = true, all fields are enclosed in double quotes
	    public String convertToCsvFormat(
	            final List<String> line,
	            final String separator,
	            final boolean quote) {

	        return line.stream()
	                .map(l -> formatCsvField(l, quote))         // format CSV field
	                .collect(Collectors.joining(separator));    // join with a separator

	    }

	   
	    private String formatCsvField(final String field, final boolean quote) {

	        String result = field;

	        if (result.contains(COMMA)
	                || result.contains(DOUBLE_QUOTES)
	                || result.contains(NEW_LINE_UNIX)
	                || result.contains(NEW_LINE_WINDOWS)) {

	            // if field contains double quotes, replace it with two double quotes \"\"
	            result = result.replace(DOUBLE_QUOTES, EMBEDDED_DOUBLE_QUOTES);

	            // must wrap by or enclosed with double quotes
	            result = DOUBLE_QUOTES + result + DOUBLE_QUOTES;

	        } else {
	            // should all fields enclosed in double quotes
	            if (quote) {
	                result = DOUBLE_QUOTES + result + DOUBLE_QUOTES;
	            }
	        }

	        return result;

	    }

	    
	    public void writeToCsvBuffer(List<String> columns, StringBuilder out) {
	    	String line = convertToCsvFormat(columns);
	        out.append(line);
	    	out.append(NEW_LINE_WINDOWS);	        
	    }

}
