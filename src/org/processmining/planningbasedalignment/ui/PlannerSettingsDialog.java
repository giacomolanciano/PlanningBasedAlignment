package org.processmining.planningbasedalignment.ui;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import javax.swing.table.DefaultTableModel;

import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.ui.widgets.ProMTable;
import org.processmining.planningbasedalignment.utils.PlannerSearchStrategy;

import com.fluxicon.slickerbox.factory.SlickerFactory;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

/**
 * The panel for setting the parameters of the planner.
 * 
 * @author Giacomo Lanciano
 *
 */
public class PlannerSettingsDialog extends JComponent {

	private static final long serialVersionUID = -60087716353524468L;
	private static final int TABLE_WIDTH = 400;
	private static final int TABLE_HEIGHT = 70;
	private static final int TABLE_FIELDS_NUM = 2;

	/**
	 * The radio button for selecting optimal strategy.
	 */
	private final JRadioButton optimalStrategy;
	
	/**
	 * The radio button for selecting sub-optimal strategy.
	 */
	private final JRadioButton subOptimalStrategy;
	
	/**
	 * The table holding data for the trace interval.
	 */
	private DefaultTableModel tracesIntervalModel;
	
	/**
	 * The table holding data for the trace length boundaries.
	 */
	private DefaultTableModel tracesLengthBoundsModel;
	
	
	public PlannerSettingsDialog(XLog log) {
		
		double size[][] = { 
				{ 30, TableLayoutConstants.FILL, 50 },					// cols
				{ 30, 30, 30, 60, TABLE_HEIGHT, 60, TABLE_HEIGHT} };	// rows
		
		setLayout(new TableLayout(size));
		setBackground(new Color(200, 200, 200));

		SlickerFactory slickerFactory = SlickerFactory.instance();
		
		// strategy choices
		ButtonGroup plannerSearchStrategySelection = new ButtonGroup();
		
		optimalStrategy = slickerFactory.createRadioButton("Optimal (Blind A*)");
		plannerSearchStrategySelection.add(optimalStrategy);
		optimalStrategy.setSelected(true);  // optimal strategy set by default
		
		subOptimalStrategy = slickerFactory.createRadioButton("Sub-optimal (Lazy Greedy)");
		plannerSearchStrategySelection.add(subOptimalStrategy);
		
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log);
		
		// trace ids interval
		Object[][] tracesInterval = new Object[1][TABLE_FIELDS_NUM];
		tracesInterval[0][0] = "1";
		tracesInterval[0][1] = ""+logInfo.getNumberOfTraces();
		tracesIntervalModel = new DefaultTableModel(tracesInterval, new Object[] { "From", "To" }) {
			private static final long serialVersionUID = -6019224467802441949L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return true;
			};
		};
		ProMTable tracesIntervalTable = new ProMTable(tracesIntervalModel);
		tracesIntervalTable.setPreferredSize(new Dimension(TABLE_WIDTH, TABLE_HEIGHT));
		tracesIntervalTable.setMinimumSize(new Dimension(TABLE_WIDTH, TABLE_HEIGHT));
		
		
		// traces length bounds
		Object[][] tracesLengthBounds = new Object[1][TABLE_FIELDS_NUM];
		tracesLengthBounds[0] = getActualTracesLengthBounds(log);
		tracesLengthBoundsModel = new DefaultTableModel(tracesLengthBounds, new Object[] { "Min length", "Max length" }) {
			private static final long serialVersionUID = -6019224467802441949L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return true;
			};
		};
		ProMTable tracesLengthBoundsTable = new ProMTable(tracesLengthBoundsModel);
		tracesLengthBoundsTable.setPreferredSize(new Dimension(TABLE_WIDTH, TABLE_HEIGHT));
		tracesLengthBoundsTable.setMinimumSize(new Dimension(TABLE_WIDTH, TABLE_HEIGHT));
		
		
		// add components to view
		int basicRowCounter = 0;
		
		add(slickerFactory.createLabel("<html><h2>Select Planner Search Strategy</h2></html>"), 
				"0," + basicRowCounter + ",1," + basicRowCounter + ",l,b");
		basicRowCounter++;
		
		add(optimalStrategy, "0," + basicRowCounter + ",1," + basicRowCounter + ",l,b");
		basicRowCounter++;
		
		add(subOptimalStrategy, "0," + basicRowCounter + ",1," + basicRowCounter + ",l,b");
		basicRowCounter++;
		
		add(slickerFactory.createLabel("<html><h2>Select the interval of trace to align</h2></html>"), 
				"0," + basicRowCounter + ",1," + basicRowCounter + ",l,b");
		basicRowCounter++;
		
		add(tracesIntervalTable, "0," + basicRowCounter + ",1," + basicRowCounter + ",l,t");
		basicRowCounter++;
		
		add(slickerFactory.createLabel("<html><h2>Select the bounds for traces length</h2></html>"), 
				"0," + basicRowCounter + ",1," + basicRowCounter + ",l,b");
		basicRowCounter++;
		
		add(tracesLengthBoundsTable, "0," + basicRowCounter + ",1," + basicRowCounter + ",l,t");
		basicRowCounter++;
		
	}
	
	
	/**
	 * Compute the minimum and maximum traces lengths of the given event log.
	 * 
	 * @param log The XLog representing the event log.
	 * @return An array of Object containing the minimum and maximum traces lengths respectively.
	 */
	private Object[] getActualTracesLengthBounds(XLog log) {
		int maxLength = 0;
		int minLength = log.get(0).size();
		int traceLength;
		for (XTrace trace : log) {
			traceLength = trace.size();
			if (traceLength > maxLength)
				maxLength = traceLength;
			if (traceLength < minLength)
				minLength = traceLength;
		}
		return new Object[] { ""+minLength, ""+maxLength };
	}


	/**
	 * Tells which search strategy has been chosen.
	 * 
	 * @return The chosen PlannerSearchStrategy.
	 */
	public PlannerSearchStrategy getChosenStrategy() {
		if (optimalStrategy.isSelected())
			return PlannerSearchStrategy.BLIND_A_STAR;
		if (subOptimalStrategy.isSelected())
			return PlannerSearchStrategy.LAZY_GREEDY;
		return null;
	}
	
	
	/**
	 * Returns the endpoints (trace ids) of the interval of traces to be aligned.
	 * 
	 * @return An array of two integers, respectively the start and the end of the interval.
	 */
	public int[] getChosenTracesInterval() {
		int start = Integer.parseInt((String) tracesIntervalModel.getValueAt(0, 0));
		int end = Integer.parseInt((String) tracesIntervalModel.getValueAt(0, 1));
		return new int[]{start, end};
	}
	
	
	/**
	 * Returns the length boundaries for the traces to be aligned.
	 * 
	 * @return An array of two integers, respectively the minimum and the maximum length.
	 */
	public int[] getChosenTracesLengthBounds() {
		int minLength = Integer.parseInt((String) tracesLengthBoundsModel.getValueAt(0, 0));
		int maxLength = Integer.parseInt((String) tracesLengthBoundsModel.getValueAt(0, 1));
		return new int[]{minLength, maxLength};
	}
	
	/**
	 * Check whether the inserted settings are valid.
	 * 
	 * @return true if the settings are valid.
	 */
	public boolean checkSettingsIntegrity() {
		return checkTracesIntervalIntegrity() && checkTracesLengthBoundsIntegrity();
	}
	
	/**
	 * Check whether the inserted traces interval is valid.
	 * 
	 * @return true if the traces interval is valid.
	 */
	private boolean checkTracesIntervalIntegrity() {
		int[] interval = getChosenTracesInterval();
		return interval[0] <= interval[1];
	}
	
	/**
	 * Check whether the inserted traces length boundaries are valid.
	 * 
	 * @return true if the traces length boundaries are valid.
	 */
	private boolean checkTracesLengthBoundsIntegrity() {
		int[] boundaries = getChosenTracesLengthBounds();
		return boundaries[0] <= boundaries[1];
	}

}
