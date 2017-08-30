package org.processmining.planningbasedalignment.plugins.visualization.projection;

import java.awt.Color;
import java.util.concurrent.Executors;

import javax.swing.JComponent;

import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithDataFactory;
import org.processmining.planningbasedalignment.models.PlanningBasedReplayResult;

import weka.gui.GenericObjectEditor;

/**
 * Borrowed from DataAwareReplayer.
 *
 */
@Plugin(
	name = "00 Projection on the Petri net",
	returnLabels = { "Projection of the Planning-based Alignment onto the Petri Net" },
	returnTypes = { JComponent.class },
	parameterLabels = { "Matching Instances" },
	userAccessible = true
)
@Visualizer
public class ProjectionOnPetrinetVisualizer {

	@PluginVariant(requiredParameterLabels = { 0 })
	public JComponent visualize(PluginContext context, final PlanningBasedReplayResult replayResult) throws Exception {
		PetrinetGraph original = replayResult.getPetrinet();
		PetriNetWithDataFactory factory=new PetriNetWithDataFactory(original, original.getLabel());

		final DataPetriNet cloneNet = factory.getRetValue();
		
		for(Transition node : cloneNet.getTransitions()) {
			if (node.isInvisible()) {
				node.getAttributeMap().remove(AttributeMap.TOOLTIP);
				node.getAttributeMap().put(AttributeMap.FILLCOLOR, new Color(0,0,0,127));
			}
			else {
				float actArray[] = replayResult.actArray.get(node.getLabel());
				if (actArray!=null) {
					float syncMoves = actArray[0] + actArray[1];
					float logMoves = actArray[2];
					float modelMoves = actArray[3];
					float controlFlowDeviation = syncMoves / (syncMoves + logMoves + modelMoves);
					
					// add coloring
					Color fillColor=getColorForValue(controlFlowDeviation);
					Color textColor=new Color(255-fillColor.getRed(),255-fillColor.getGreen(),255-fillColor.getBlue());
					node.getAttributeMap().put(AttributeMap.FILLCOLOR, fillColor);
					node.getAttributeMap().put(AttributeMap.LABELCOLOR, textColor);
					
					// add activity tooltip
					StringBuffer tooltip=new StringBuffer("<html><table><tr><td><b>Number of synchronous moves:</b> ");
					tooltip.append((int) syncMoves);
					tooltip.append("</td></tr><tr><td><b>Number of moves in log:</b> ");
					tooltip.append((int) logMoves);
					tooltip.append("</td></tr><tr><td><b>Number of moves in model:</b> ");
					tooltip.append((int) modelMoves);
					tooltip.append("</td></tr></table></html>");
					node.getAttributeMap().put(AttributeMap.TOOLTIP,tooltip.toString());
				}
			}
		}

		// Load Weka on a separate thread as this takes quite some time
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					GenericObjectEditor.class.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
				}				
			}
		});

		return new AnalyzePanel(context, cloneNet, replayResult);
	}

	/**
	 * Associate the value with a Color. Value 0 corresponds to RED colour and Value 1 to GREEN. 
	 * @param value A float value between 0 and 1
	 * @return A Color
	 */
	public Color getColorForValue(float value) {
		assert(value<=1 && value>=0);
		value=0.2F+value*0.8F;
		float red=(float) (Math.min(value, 0.3333)*3);
		value=(float) Math.max(value-0.3333, 0);
		float green=(float) (Math.min(value, 0.3333)*3);
		value=(float) Math.max(value-0.3333, 0);		
		float blue=(float) (Math.min(value, 0.3333)*3);
		return new Color(red,green,blue);
	}

}
