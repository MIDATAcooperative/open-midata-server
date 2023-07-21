package utils.plugins;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import utils.AccessLog;
import utils.messaging.InputStreamCollector;

public abstract class AbstractLocalContainer extends AbstractContainer {

	public Pair<Boolean, String> process(File targetDir, List<String> command) {
		 AccessLog.log("Execute command "+command.toString());
		 System.out.println(command.toString());
		 try {
			  Process p = new ProcessBuilder(command).directory(targetDir).redirectErrorStream(true).start();
			  //System.out.println("Output...");
			  /*PrintWriter out = new PrintWriter(new OutputStreamWriter(p.getOutputStream()));		  
			  out.println(triggered.resource);
			  out.close();*/
			  //System.out.println("Output done...");
			  InputStreamCollector result = new InputStreamCollector(p.getInputStream());
			  result.start();
			  //System.out.println("Input...");
			  p.waitFor();
			  //System.out.println("Wait for finished...");
			  result.join();
			  AccessLog.log("Wait for input...");
			  AccessLog.log(result.getResult());	
			  int exit = p.exitValue();
			  AccessLog.log("EXIT VALUE = "+exit);
			  return Pair.of(exit==0, result.getResult());
		 } catch (IOException e) {
			 e.printStackTrace();
			 return Pair.of(false, "IO Exception");
		 } catch (InterruptedException e2) {
			 return Pair.of(false, "Interrupted Exception");
		 }		 
	}
}
