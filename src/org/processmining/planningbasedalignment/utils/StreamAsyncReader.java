package org.processmining.planningbasedalignment.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A worker thread for reading and printing on stdout an {@link InputStream}.
 * 
 * @author Giacomo Lanciano
 *
 */
public class StreamAsyncReader extends Thread {
	
	/**
	 * The {@link InputStream} to be handled.
	 */
	InputStream inputStream;
	
	/**
	 * The tag to be associated to each prints.
	 */
	String type;

	public StreamAsyncReader(InputStream inputStream, String type) {
		this.inputStream = inputStream;
		this.type = type;
	}

	@Override
	public void run() {
		try {
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			String line = null;
			while ((line = bufferedReader.readLine()) != null)
				System.out.println(type + ">" + line);
			
		} catch (IOException e) {
			e.printStackTrace();  
		}
	}
}