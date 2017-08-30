package org.processmining.planningbasedalignment.plugins.visualization.alignment;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceList;
import org.processmining.log.utils.XUtils;
import org.processmining.plugins.DataConformance.visualization.alignment.XTraceResolver;
import org.processmining.plugins.utils.ProvidedObjectHelper;
import org.processmining.xesalignmentextension.XAlignmentExtension.XAlignment;

/**
 * Borrowed from DataAwareReplayer.
 *
 */
final class ActionExportTraces implements ActionListener {
	
	private final ProMTraceList<XAlignment> listView;
	private final PluginContext context;
	private XTraceResolver traceResolver;

	public ActionExportTraces(PluginContext context, ProMTraceList<XAlignment> listView, XTraceResolver traceResolver) {
		this.listView = listView;
		this.context = context;
		this.traceResolver = traceResolver;
	}

	public void actionPerformed(ActionEvent e) {
		@SuppressWarnings("deprecation")
		Object[] selection = listView.getList().getSelectedValues();
									
		String proposedName = String.format("Exported %s traces", selection.length);
		String exportName = JOptionPane.showInputDialog(listView, "Specify the name of the new ProM log!", proposedName);
		
		if (exportName != null) {
			XLog newLog = XFactoryRegistry.instance().currentDefault().createLog();
			XConceptExtension.instance().assignName(newLog, exportName);
			for (Object o : selection) {
				XAlignment a = (XAlignment) o;
				XTrace trace = traceResolver.getOriginalTrace(XUtils.getConceptName(a.getTrace()));
				if (trace != null) {
					newLog.add((XTrace) trace.clone());	
				}								
			}
			if (!newLog.isEmpty()) {
				ProvidedObjectHelper.publish(context, exportName, newLog, XLog.class, true);
			} else {
				Object[] options = { "OK" };
				JOptionPane.showOptionDialog(listView, "Could not find original traces.", "Missing Traces",
						JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
			}
		}
	}
}