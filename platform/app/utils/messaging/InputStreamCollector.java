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