package org.processmining.planningbasedalignment.plugins.planningbasedalignment.ui;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.plugin.PluginDescriptor;
import org.processmining.framework.util.ui.widgets.WidgetColors;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.models.PlannerSearchStrategy;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.models.PlanningBasedReplayResult;
import org.processmining.planningbasedalignment.utils.ConfigurationPanel;

import com.fluxicon.slickerbox.components.NiceIntegerSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * The panel for setting the parameters of the planner.
 * 
 * @author Giacomo Lanciano
 *
 */
public class PlannerSettingsDialog extends ConfigurationPanel {

	private static final long serialVersionUID = -60087716353524468L;
	private static final int LABEL_WIDTH = 50;
	private static final int LABEL_HEIGHT = 16;
	private static final int PADDING = 200;

	/**
	 * The radio button for selecting optimal strategy.
	 */
	private JRadioButton optimalStrategy;

	/**
	 * The radio button for selecting sub-optimal strategy.
	 */
	private JRadioButton subOptimalStrategy;
	
	/**
	 * The slider for selecting the starting point of the interval of traces to align.
	 */
	private NiceIntegerSlider startSlider;
	
	/**
	 * The slider for selecting the ending point of the interval of traces to align.
	 */
	private NiceIntegerSlider endSlider;
	
	/**
	 * The slider for selecting the minimum length of the traces to align.
	 */
	private NiceIntegerSlider minLenghtSlider;
	
	/**
	 * The slider for selecting the maximum length of the traces to align.
	 */
	private NiceIntegerSlider maxLenghtSlider;

	public PlannerSettingsDialog(XLog log, PluginDescriptor pluginDescriptor) {
		
		super("");
		
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log);
		int tracesAmount = logInfo.getNumberOfTraces();
		int[] tracesLengthBounds = getActualTracesLengthBounds(log);
		
		// trace ids interval
		Box tracesIntervalBox = Box.createVerticalBox();
		this.startSlider = createFormattedIntegerSlider("start", 1, tracesAmount, 1);
		this.endSlider = createFormattedIntegerSlider("end", 1, tracesAmount, tracesAmount);
		tracesIntervalBox.add(this.startSlider);
		tracesIntervalBox.add(this.endSlider);
		
		// traces length bounds
		Box tracesLengthBox = Box.createVerticalBox();
		this.minLenghtSlider = createFormattedIntegerSlider(
				"min", tracesLengthBounds[0], tracesLengthBounds[1], tracesLengthBounds[0]);
		this.maxLenghtSlider = createFormattedIntegerSlider(
				"max", tracesLengthBounds[0], tracesLengthBounds[1], tracesLengthBounds[1]);
		tracesLengthBox.add(this.minLenghtSlider);
		tracesLengthBox.add(this.maxLenghtSlider);
				
		// check whether the planner will be executed after configuration		
		// show strategy selection if planner will be executed
		if (pluginDescriptor.getReturnTypes().contains(PlanningBasedReplayResult.class)) {
			
			// search strategy selection
			ButtonGroup plannerSearchStrategySelection = new ButtonGroup();
			this.optimalStrategy = SlickerFactory.instance().createRadioButton("Optimal (Blind A*)");
			this.optimalStrategy.setForeground(WidgetColors.TEXT_COLOR);
			plannerSearchStrategySelection.add(this.optimalStrategy);
			this.optimalStrategy.setSelected(true);  // optimal strategy set by default
			subOptimalStrategy = SlickerFactory.instance().createRadioButton("Sub-optimal (Lazy Greedy)");
			this.subOptimalStrategy.setForeground(WidgetColors.TEXT_COLOR);
			plannerSearchStrategySelection.add(this.subOptimalStrategy);
			
			Box searchStrategyBox = Box.createVerticalBox();
			searchStrategyBox.add(this.optimalStrategy);
			searchStrategyBox.add(this.subOptimalStrategy);
			
			// adapt box dimension to other components
			searchStrategyBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, PADDING));
			
			// add component to view
			addProperty("Select Planner Search Strategy", searchStrategyBox);
		}

		// add components to view
		addProperty("Select the interval of trace to align", tracesIntervalBox);
		addProperty("Select the bounds for traces length", tracesLengthBox);

	}

	/**
	 * Tells which search strategy has been chosen.
	 * 
	 * @return The chosen PlannerSearchStrategy.
	 */
	public PlannerSearchStrategy getChosenStrategy() {
		if (optimalStrategy != null && optimalStrategy.isSelected())
			return PlannerSearchStrategy.BLIND_A_STAR;
		if (subOptimalStrategy != null && subOptimalStrategy.isSelected())
			return PlannerSearchStrategy.LAZY_GREEDY;
		return null;
	}

	/**
	 * Returns the endpoints (trace ids) of the interval of traces to be aligned.
	 * 
	 * @return An array of two integers, respectively the start and the end of the interval.
	 */
	public int[] getChosenTracesInterval() {
		return new int[]{ this.startSlider.getValue(), this.endSlider.getValue() };
	}

	/**
	 * Returns the length boundaries for the traces to be aligned.
	 * 
	 * @return An array of two integers, respectively the minimum and the maximum length.
	 */
	public int[] getChosenTracesLengthBounds() {
		return new int[]{ this.minLenghtSlider.getValue(), this.maxLenghtSlider.getValue() };
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
	 * Compute the minimum and maximum traces lengths of the given event log.
	 * 
	 * @param log The XLog representing the event log.
	 * @return An array of integers containing the minimum and maximum traces lengths respectively.
	 */
	private int[] getActualTracesLengthBounds(XLog log) {
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
		return new int[] { minLength, maxLength };
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
	
	/**
	 * Build a slider with predefined format for title and value fields.
	 * 
	 * @param title The title of the slider.
	 * @param min The minimum value.
	 * @param max The maximum value.
	 * @param initial The initial value.
	 * @return A {@link NiceIntegerSlider}.
	 */
	private NiceIntegerSlider createFormattedIntegerSlider(String title, int min, int max, int initial) {
		Dimension labelDimension = new Dimension(LABEL_WIDTH, LABEL_HEIGHT);
		NiceIntegerSlider slider = SlickerFactory.instance().createNiceIntegerSlider(
				title, min, max, initial, Orientation.HORIZONTAL);
		
		// set title field format
		JLabel titleLabel = (JLabel) slider.getComponent(0); 
		titleLabel.setForeground(WidgetColors.TEXT_COLOR);
		titleLabel.setMinimumSize(labelDimension);
		titleLabel.setPreferredSize(labelDimension);
		titleLabel.setHorizontalAlignment(JLabel.LEFT);
		
		// set value field format
		JLabel valueLabel = (JLabel) slider.getComponent(1);
		valueLabel.setForeground(WidgetColors.TEXT_COLOR);
		valueLabel.setMinimumSize(labelDimension);
		valueLabel.setPreferredSize(labelDimension);
		
		return slider;
	}

}
