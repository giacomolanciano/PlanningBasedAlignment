package org.processmining.planningbasedalignment.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;

public class Utilities {

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
	 * Delete all contents from the given directory
	 * 
	 * @param folder The File object representing the folder.
	 */
	public static void deleteFolderContents(File folder) {
		File[] files = folder.listFiles();
		if(files!=null) { 
			for(File file : files) {
				if(file.isDirectory()) {
					deleteFolderContents(file);
				} else {
					file.delete();
				}
			}
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
		
		return osArch != null && osArch.endsWith("64") || winArch != null && winArch.endsWith("64") || wow64Arch != null && wow64Arch.endsWith("64");
	}

	/**
	 * Check whether the given string is the textual representation of an integer.
	 * 
	 * @param string The string to be checked.
	 * @return true if the string represents an integer.
	 */
	public static boolean isInteger(String string) {
		try { 
			Integer.parseInt(string); 
		} catch(NumberFormatException e) { 
			return false; 
		} catch(NullPointerException e) {
			return false;
		}
		return true;
	}

	/**
	 * Compute the current timestamp.
	 * 
	 * @return the current timestamp.
	 */
	public static Timestamp getCurrentTimestamp() {
		java.util.Date date = new java.util.Date();
		return new Timestamp(date.getTime());
	}

	/**
	 * Check whether there is an upper case character in the given string.
	 * 
	 * @param string The string to be checked.
	 * @return true if there is at least an upper case character.
	 */
	public static boolean isAnyCharUpperCase(String string){
		for(int i=0; i < string.length(); i++){
			char c = string.charAt(i);
			if(Character.isUpperCase(c))
				return true;
		}
		return false;
	}
	
}
