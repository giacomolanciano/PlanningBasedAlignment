package org.processmining.planningbasedalignment.utils;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JPanel;

/**
 * A dialog for choosing a directory from the file system.
 * 
 * @author Giacomo Lanciano
 *
 */
public class DirectoryChooser extends JPanel {

	private static final long serialVersionUID = -1674408935899704180L;

	public File chooseDirectory() {
		JFileChooser chooser;
		
		chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
		chooser.setDialogTitle("Select a directory");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		// disable the "All files" option.
		chooser.setAcceptAllFileFilterUsed(false);
				
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			return chooser.getSelectedFile();
		}
		
		return null;
	}
	
}
