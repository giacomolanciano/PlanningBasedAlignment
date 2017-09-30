package org.processmining.planningbasedalignment.plugins.partialorder.maker.ui;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.util.ui.widgets.ProMTextField;
import org.processmining.planningbasedalignment.plugins.partialorder.maker.PartialOrderMakerPlugin;
import org.processmining.planningbasedalignment.plugins.partialorder.maker.parameters.GroupsWisePartialOrderMakerParameters;
import org.processmining.planningbasedalignment.utils.ConfigurationPanel;

/**
 * The configuration wizard for tuning the parameters to run the groups-wise variant of {@link PartialOrderMakerPlugin}.
 * 
 * @author Giacomo Lanciano
 *
 */
public class GroupsWisePartialOrderMakerConfiguration {

	private static final String DEFAULT_EXPECTED_GROUP_SIZE = "4";
	private static final String DEFAULT_INTERVAL_RADIUS = "1";
	
	/**
	 * Run the configuration wizard for tuning the parameters.
	 * 
	 * @param context The context where to run in.
	 * @return The parameters.
	 */
	public GroupsWisePartialOrderMakerParameters getParameters(UIPluginContext context) {
		
		GroupsWisePartialOrderMakerParameters result = new GroupsWisePartialOrderMakerParameters();
		
		// init UI
		int intervalRadius;
		int expectedGroupSize;
		ConfigurationPanel panel;
		ProMTextField intervalRadiusField;
		ProMTextField expectedGroupSizeField;
		do {
			panel = new ConfigurationPanel("");
			expectedGroupSizeField = new ProMTextField(DEFAULT_EXPECTED_GROUP_SIZE);
			intervalRadiusField = new ProMTextField(DEFAULT_INTERVAL_RADIUS);
			
			panel.addProperty("Expected Group Size", expectedGroupSizeField);
			expectedGroupSizeField.setToolTipText("The expected value of the size of the isochronous groups.");
			panel.addProperty("Interval Radius", intervalRadiusField);
			intervalRadiusField.setToolTipText(
					"The radius of the interval (centered in the expected size) where to pick the random values from.");
			
			// prompt user
			InteractionResult interactionResult = context.showConfiguration("Configuration", panel);
			
			if (interactionResult == InteractionResult.CANCEL) {
				return null;
			}
			
			try {
				intervalRadius = Integer.parseInt(intervalRadiusField.getText());
				expectedGroupSize = Integer.parseInt(expectedGroupSizeField.getText());
				
			} catch (NumberFormatException e) {
				// if an input is not an integer, force the integrity check to fail
				e.printStackTrace();
				intervalRadius = -1;
				expectedGroupSize = 0;
			}
			
		} while (!checkParametersIntegrity(expectedGroupSize, intervalRadius));
		
		result.setIntervalRadius(intervalRadius);
		result.setExpectedGroupSize(expectedGroupSize);
		return result;
	}

	/**
	 * Check whether the provided parameters are valid. 
	 * 
	 * @param expectedGroupSize The expected value of the size of the isochronous groups.
	 * @param intervalRadius The radius of the interval where to pick the random values from.
	 * @return true if the parameters are valid.
	 */
	private boolean checkParametersIntegrity(int expectedGroupSize, int intervalRadius) {
		
		if (expectedGroupSize > 0 && intervalRadius >= 0 && expectedGroupSize > intervalRadius)
			return true;
		
		JOptionPane.showMessageDialog(
				new JPanel(),
				"The inserted parameters are invalid.",
				"Invalid parameters", JOptionPane.ERROR_MESSAGE);
		
		return false;
	}

}
