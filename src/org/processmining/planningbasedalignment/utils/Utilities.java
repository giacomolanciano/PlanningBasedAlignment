package org.processmining.planningbasedalignment.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Vector;

public class Utilities {

	/**
	 * Create a file with the given name and write the given contents on it.
	 * 
	 * @param fileName The name of the file to be created.
	 * @param buffer The contents to be written.
	 * @return The newly create file.
	 */
	public static File writeFile(String fileName, StringBuffer buffer) {
		File file = null;
		FileWriter fileWriter = null;

		try {
			file = new File(fileName);
			file.setExecutable(true);
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
	 * Format string to be a valid PDDL identifier.
	 * 
	 * @param string The string to be formatted.
	 * @return The correctly formatted string.
	 */
	public static String getCorrectPddlFormat(String string)  {

		if(string.contains(" "))
			string = string.replaceAll(" ", "_");

		if(string.contains("/"))
			string = string.replaceAll("\\/", "");

		if(string.contains("("))
			string = string.replaceAll("\\(", "");

		if(string.contains(")"))
			string = string.replaceAll("\\)", "");

		if(string.contains("<"))
			string = string.replaceAll("\\<", "");

		if(string.contains(">"))
			string = string.replaceAll("\\>", "");

		if(string.contains("."))
			string = string.replaceAll("\\.", "");

		if(string.contains(","))
			string = string.replaceAll("\\,", "_");

		if(string.contains("+"))
			string = string.replaceAll("\\+", "_");

		if(string.contains("-"))
			string = string.replaceAll("\\-", "_");

		return string;
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

	/**
	 * 
	 * 
	 * @param activityName
	 * @param moveType
	 * @return
	 */
	public static String getCostOfActivity(String activityName, AlignmentMove moveType) {
		for(Vector<String> entry : Globals.getActivitiesCostsVector()) {
			if(entry.elementAt(0).equalsIgnoreCase(activityName)) {
				if(moveType == AlignmentMove.MOVE_IN_MODEL)
					return(entry.elementAt(1));
				else if(moveType == AlignmentMove.MOVE_IN_LOG)
					return(entry.elementAt(2));
			}
		}
		return null;
	}

}
