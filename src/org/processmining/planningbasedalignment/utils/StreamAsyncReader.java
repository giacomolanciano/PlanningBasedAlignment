package org.processmining.planningbasedalignment.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Class for reading and printing on stdout an {@link InputStream} in a separated thread.
 * 
 * @author Giacomo Lanciano
 *
 */
public class StreamAsyncReader extends Thread {
	
	/**
	 * The {@link InputStream} to be handled.
	 */
	InputStream is;
	
	/**
	 * The tag to be associated to each prints.
	 */
	String type;

	public StreamAsyncReader(InputStream is, String type) {
		this.is = is;
		this.type = type;
	}

	@Override
	public void run() {
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line=null;
			while ( (line = br.readLine()) != null)
				System.out.println(type + ">" + line);    
		} catch (IOException ioe) {
			ioe.printStackTrace();  
		}
	}
}