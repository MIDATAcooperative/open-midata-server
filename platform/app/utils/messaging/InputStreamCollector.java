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

package utils.messaging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class InputStreamCollector extends Thread {

    private final InputStream inputStream;
    private final StringBuffer out;
    
    public InputStreamCollector(InputStream inputStream) {
        this.inputStream = inputStream;
        this.out = new StringBuffer();
    }
    
    @Override
    public  void run() {

        try {
            // read until the process is running or the stream is empty
            byte[] buffer = new byte[8192];
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            char[] buf = new char[1024];            
            while (!Thread.currentThread().isInterrupted()) {
            	int c = reader.read(buf);
            	if (c>0) out.append(buf,0,c);
            	if (c<0) {
            		System.out.println("EOI");
            		return;
            	}
            }
            System.out.println("INTERRUPTED");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String getResult() {
    	return out.toString();
    }
}