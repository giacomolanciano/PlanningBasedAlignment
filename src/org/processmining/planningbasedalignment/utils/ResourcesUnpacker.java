package org.processmining.planningbasedalignment.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.algorithms.PlanningBasedAlignment;

/**
 * A worker thread for unpacking the resources needed to run the planner. 
 * 
 * @author Giacomo Lanciano
 *
 */
public class ResourcesUnpacker extends Thread {

	/**
     * Size of the buffer to read/write data.
     */
    private static final int BUFFER_SIZE = 4096;
    
    /**
     * The name of the archive to extract the resources from.
     */
    private static final String PLANNING_ARCHIVE = "planning.zip";
    
    /**
     * The name of the directory containing the FD builds.
     */
    private static final String FD_BUILDS_DIR = "fast-downward/builds/";
    
    /**
     * The name of the directory containing the FD executables for 32-bits OS.
     */
    private static final String RELEASE_32_BUILD_DIR = "release32/";
    
    /**
     * The name of the directory containing the FD executables for 64-bits OS.
     */
    private static final String RELEASE_64_BUILD_DIR = "release64/";

    /**
     * The name of the directory containing the FD binary files.
     */
    private static final String BIN_DIR = "bin/";
    
    /**
     * The name of the FD executable file.
     */
    private static final String FD_EXECUTABLE = "downward";
    
    /**
     * The name of the directory containing the FD executables for Mac OSX.
     */
    private static final String OSX_BUILDS_DIR = "osx_builds/";
    
    /**
     * The context of the UI.
     */
	private UIPluginContext context;

	public ResourcesUnpacker(UIPluginContext context) {
		super();
		this.context = context;
	}

	@Override
	public void run() {
		try {
			unpackResources();
			
			// substitute FD executables if running on MAC OSX
			if (SystemUtils.IS_OS_MAC_OSX) {
				System.out.println("The plug-in is running on Mac OSX, unpacking proper FD executables...");
				substituteExecutablesForOSX();
			}
			
			// remove useless stuff
			FileUtils.deleteQuietly(new File(OSX_BUILDS_DIR));
			
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
			context.getFutureResult(0).cancel(true);
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Unpack the archive in the current working directory.
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	private void unpackResources() throws IOException, URISyntaxException {
		
		Class<PlanningBasedAlignment> planningBasedAlignment = PlanningBasedAlignment.class;
		
		String packageName = planningBasedAlignment.getPackage().getName();
		packageName = packageName.replaceAll("\\.", "/") + "/";
		
		InputStream planningArchive = planningBasedAlignment.getClassLoader().getResourceAsStream(
				packageName + PLANNING_ARCHIVE);
		
		ZipInputStream zis = new ZipInputStream(planningArchive);
		ZipEntry entry;
		while ((entry = zis.getNextEntry()) != null) {
			
			//String filePath = destDirectory + File.separator + entry.getName();
			String filePath = entry.getName();
			
			if (!entry.isDirectory()) {
				// if the entry is a file, extracts it
				extractFile(zis, filePath);
			} else {
				// if the entry is a directory, make the directory
				File dir = new File(filePath);
				dir.mkdir();
			}
			
		}
	}
	
	
	/**
     * Extracts a file entry.
     * 
     * @param zipIn The input stream of the .zip archive entry.
     * @param filePath The path of the file to be created.
     * @throws IOException
     */
    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
    	File file = new File(filePath);
    	
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
        
        /*
         *  set the executable flag (for every user) to prevent issues when running on Unix-like OSs.
         *  It has to be done AFTER the file has been written.
         */
        file.setExecutable(true, false);
    }
    
    /**
     * Substitute the FD Linux executables with Mac OSX ones. 
     * 
     * @throws IOException 
     */
    private void substituteExecutablesForOSX() throws IOException {
    	
    	// FD bin dirs
    	File release32BinDir = new File(FD_BUILDS_DIR + RELEASE_32_BUILD_DIR + BIN_DIR);
    	File release64BinDir = new File(FD_BUILDS_DIR + RELEASE_64_BUILD_DIR + BIN_DIR);
    	
    	// FD executables for Linux
    	File release32ExecutableLinux = new File(release32BinDir, FD_EXECUTABLE);
    	File release64ExecutableLinux = new File(release64BinDir, FD_EXECUTABLE);
    	
    	// FD executables for Mac OSX
    	File release32ExecutableOSX = new File(OSX_BUILDS_DIR + RELEASE_32_BUILD_DIR + FD_EXECUTABLE);
    	File release64ExecutableOSX = new File(OSX_BUILDS_DIR + RELEASE_64_BUILD_DIR + FD_EXECUTABLE);
    	
    	// delete Linux executables
    	System.out.println("Deleting FD Linux executables...");
    	FileUtils.deleteQuietly(release32ExecutableLinux);
    	FileUtils.deleteQuietly(release64ExecutableLinux);
    	
    	// move Mac OSX executables to proper location
    	System.out.println("Moving FD Mac OSX executables to bin directory...");
    	FileUtils.moveFileToDirectory(release32ExecutableOSX, release32BinDir, true);
    	FileUtils.moveFileToDirectory(release64ExecutableOSX, release64BinDir, true);
    }
    
}
