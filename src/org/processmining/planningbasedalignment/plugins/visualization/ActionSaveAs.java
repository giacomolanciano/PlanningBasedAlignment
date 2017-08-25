package org.processmining.planningbasedalignment.plugins.visualization;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.processmining.framework.util.ui.widgets.helper.ProMUIHelper;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceList;
import org.processmining.xesalignmentextension.XAlignmentExtension.XAlignment;

/**
 * Borrowed from DataAwareReplayer.
 *
 */
final class ActionSaveAs implements ActionListener {
	
	static final Preferences PREFS = Preferences.userRoot().node("org.processmining.dataawarereplayer");
	static final String LAST_USED_FOLDER = "lastUsedFolder";
	
	private final ProMTraceList<XAlignment> listView;
	private final Comparator<XAlignment> sortOrder;

	public ActionSaveAs(ProMTraceList<XAlignment> listView, Comparator<XAlignment> sortOrder) {
		this.listView = listView;
		this.sortOrder = sortOrder;
	}

	public void actionPerformed(ActionEvent e) {
		
		JFileChooser chooser = new JFileChooser(PREFS.get(LAST_USED_FOLDER, new File(".").getAbsolutePath()));
		FileNameExtensionFilter pdfFilter = new FileNameExtensionFilter("PDF", "pdf");
		FileNameExtensionFilter emfFilter = new FileNameExtensionFilter("EMF", "emf");
		FileNameExtensionFilter epsFilter = new FileNameExtensionFilter("EPS", "eps");
		chooser.addChoosableFileFilter(pdfFilter);
		chooser.addChoosableFileFilter(emfFilter);
		chooser.addChoosableFileFilter(epsFilter);
		chooser.setFileFilter(pdfFilter);
		chooser.setAcceptAllFileFilterUsed(false);
		int returnVal = chooser.showSaveDialog(listView);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			PREFS.put(LAST_USED_FOLDER, file.getParent());
	
		
			List<XAlignment> traces = new ArrayList<>();
			@SuppressWarnings("deprecation")
			Object[] selection = listView.getList().getSelectedValues();
			for (Object o : selection) {
				traces.add((XAlignment) o);
			}
			
			ProMTraceList<XAlignment> listForPrinting = new ProMTraceList<>(traces, listView.getTraceBuilder(), sortOrder);
			
			listForPrinting.setOpaque(true);
			listForPrinting.setBackground(Color.WHITE);
			listForPrinting.setForeground(Color.BLACK);
			
			if (chooser.getFileFilter() == pdfFilter) {
				if (!file.getAbsolutePath().endsWith(".pdf")) {
					file = new File(file.getAbsolutePath() + ".pdf");
				}
				try {
					ProMTraceList.saveAsPDF(listForPrinting, "", file);
				} catch (IOException e1) {
					ProMUIHelper.showErrorMessage(listView, e1.getMessage(), "Error saving");
				}	
			} else if (chooser.getFileFilter() == emfFilter) {
				if (!file.getAbsolutePath().endsWith(".emf")) {
					file = new File(file.getAbsolutePath() + ".emf");
				}
				try {
					ProMTraceList.saveAsEMF(listForPrinting, file);
				} catch (IOException e1) {
					ProMUIHelper.showErrorMessage(listView, e1.getMessage(), "Error saving");
				}
			} else if (chooser.getFileFilter() == epsFilter) {
				if (!file.getAbsolutePath().endsWith(".eps")) {
					file = new File(file.getAbsolutePath() + ".eps");
				}
				try {
					ProMTraceList.saveAsEPS(listForPrinting, file);
				} catch (IOException e1) {
					ProMUIHelper.showErrorMessage(listView, e1.getMessage(), "Error saving");
				}
			}

		}

	}
}