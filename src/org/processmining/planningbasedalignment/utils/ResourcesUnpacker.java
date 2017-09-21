package org.processmining.planningbasedalignment.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.planningbasedalignment.algorithms.PlanningBasedAlignment;

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
		} catch (Exception e) {
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
    
}
