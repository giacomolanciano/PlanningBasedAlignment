package org.processmining.planningbasedalignment.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utility functions to deal with OS-related stuff.
 * 
 * @author Giacomo Lanciano
 *
 */
public class OSUtils {

	/**
	 * Create a file with the given name and write the given contents on it. If the file name contains a path, the
	 * parent directories are automatically created.
	 * 
	 * @param fileName The name of the file to be created (possibly a path).
	 * @param buffer The contents to be written.
	 * @return The newly create file.
	 */
	public static File writeFile(String fileName, StringBuffer buffer) {
		File file = null;
		FileWriter fileWriter = null;

		try {
			file = new File(fileName);
			file.setExecutable(true);
			file.getParentFile().mkdirs();
			fileWriter = new FileWriter(file);
			fileWriter.write(buffer.toString());
			fileWriter.close();
			return file;
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Delete all contents from the given directory or create it if not existing.
	 * 
	 * @param folder The File object representing the folder.
	 */
	public static void cleanFolder(File folder) {
		File[] files = folder.listFiles();
		if(files!=null) { 
			for(File file : files) {
				if(file.isDirectory()) {
					cleanFolder(file);
				} else {
					file.delete();
				}
			}
		} else {
			folder.mkdir();
		}
	}
	
	/**
	 * Check whether the OS is 64 bits.
	 * 
	 * @return true if OS is 64 bits.
	 */
	public static boolean is64bitsOS() {
		String osArch = System.getProperty("os.arch");
		String winArch = System.getenv("PROCESSOR_ARCHITECTURE");
		String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
		
		return osArch != null && osArch.endsWith("64")
				|| winArch != null && winArch.endsWith("64")
				|| wow64Arch != null && wow64Arch.endsWith("64");
	}
	
}
