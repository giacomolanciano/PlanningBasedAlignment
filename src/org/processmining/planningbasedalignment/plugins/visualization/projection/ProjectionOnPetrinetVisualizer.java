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
	returnTypes = { JComponent.class }, parameterLabels = { "Matching Instances" },
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
					
					
//					float value;
//					if (actArray[0]+actArray[1]==0 || ((PNWDTransition)node).getWriteOperations().size()==0)
//						value=(actArray[0]+actArray[1])/(actArray[0]+actArray[1]+actArray[2]+actArray[3]);
//					else {
//						float dataFlowValue=actArray[0]/(actArray[0]+actArray[1]);
//						float controlFlowValue=(actArray[0]+actArray[1])/(actArray[0]+actArray[1]+actArray[2]+actArray[3]);
//						value=2*dataFlowValue*controlFlowValue/(dataFlowValue+controlFlowValue);
//					}
//					Color fillColor=getColorForValue(value);
					
					
					float controlFlowValue=(actArray[0]+actArray[1])/(actArray[0]+actArray[1]+actArray[2]+actArray[3]);
					Color fillColor=getColorForValue(controlFlowValue);
					Color textColor=new Color(255-fillColor.getRed(),255-fillColor.getGreen(),255-fillColor.getBlue());
					node.getAttributeMap().put(AttributeMap.FILLCOLOR, fillColor);
					node.getAttributeMap().put(AttributeMap.LABELCOLOR, textColor);
					StringBuffer tooltip=new StringBuffer("<html><table><tr><td><b>Number moves in both without incorrect write operations:</b> ");
					tooltip.append((int)actArray[0]);
					tooltip.append("</td></tr><tr><td><b>Number moves in both with incorrect write operations:</b> ");
					tooltip.append((int)actArray[1]);
					tooltip.append("</td></tr><tr><td><b>Number moves in log:</b> ");
					tooltip.append((int)actArray[2]);
					tooltip.append("</td></tr><tr><td><b>Number moves in model:</b> ");
					tooltip.append((int)actArray[3]);
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
				} catch (InstantiationException e) {
				} catch (IllegalAccessException e) {
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
