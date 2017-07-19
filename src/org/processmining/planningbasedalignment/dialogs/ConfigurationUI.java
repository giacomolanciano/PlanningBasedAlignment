package org.processmining.planningbasedalignment.dialogs;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.framework.connections.Connection;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.connections.annotations.ConnectionObjectFactory;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.PluginExecutionResult;
import org.processmining.framework.plugin.PluginParameterBinding;
import org.processmining.framework.util.Pair;
import org.processmining.models.connections.petrinets.EvClassLogPetrinetConnection;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;
import org.processmining.planningbasedalignment.utils.PlannerSearchStrategy;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;

/**
 * Borrowed from PNetReplayer.
 * 
 * The GUI for setting the parameters of the Planning-based Alignment plug-in.
 */
public class ConfigurationUI {

	public static final int TRANSITIONS_EVENT_CLASSES_MAPPING = 0;
	public static final int PLANNER_SEARCH_STRATEGY = TRANSITIONS_EVENT_CLASSES_MAPPING + 1;
	public static final int TRACES_INTERVAL = PLANNER_SEARCH_STRATEGY + 1;
	public static final int TRACES_LENGTH_BOUNDS = TRACES_INTERVAL + 1;
	public static final int MOVES_ON_LOG_COSTS = TRACES_LENGTH_BOUNDS + 1;
	public static final int MOVES_ON_MODEL_COSTS = MOVES_ON_LOG_COSTS + 1;
	public static final int SYNCHRONOUS_MOVES_COSTS = MOVES_ON_MODEL_COSTS + 1;

	/**
	 * Number of required configuration steps.
	 */
	private static final int CONFIGURATION_STEPS_NUMBER = 2;

	/**
	 * The id of the current step.
	 */
	private int currentConfigurationStep;

	/** 
	 * The GUIs for each step.
	 */
	private JComponent[] configurationStepsDialogs;

	public PlanningBasedAlignmentParameters getPlanningBasedAlignmentParameters(
			UIPluginContext context, DataPetriNet petrinet, XLog log) throws ConnectionCannotBeObtained {
		
		// init local parameter
		PlanningBasedAlignmentParameters parameters = null;
		EvClassLogPetrinetConnection conn = null;

		checkInitialMarking(context, petrinet);
		checkFinalMarking(context, petrinet);

		// check connection in order to determine whether mapping step is needed of not
		try {
			// connection is found, no need for mapping step
			// connection is not found, another plugin to create such connection is automatically executed
			conn = context.getConnectionManager().getFirstConnection(EvClassLogPetrinetConnection.class, context, petrinet, log);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(new JPanel(), "No mapping can be constructed between the net and the log");
			return null;
		}

		// init gui for each step
		TransEvClassMapping mapping = (TransEvClassMapping) conn.getObjectWithRole(
				EvClassLogPetrinetConnection.TRANS2EVCLASSMAPPING);

		// check invisible transitions
		Set<Transition> unmappedTrans = new HashSet<Transition>();
		for (Entry<Transition, XEventClass> entry : mapping.entrySet()) {
			if (entry.getValue().equals(mapping.getDummyEventClass())) {
				if (!entry.getKey().isInvisible()) {
					unmappedTrans.add(entry.getKey());
				}
			}
		}
		if (!unmappedTrans.isEmpty()) {
			JList list = new JList(unmappedTrans.toArray());
			JPanel panel = new JPanel();
			BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
			panel.setLayout(layout);
			panel.add(new JLabel("The following transitions are not mapped to any event class:"));

			JScrollPane sp = new JScrollPane(list);
			panel.add(sp);
			panel.add(new JLabel("Do you want to consider these transitions as invisible (unlogged activities)?"));

			Object[] options = { "Yes, set them to invisible", "No, keep them as they are" };

			if (0 == JOptionPane.showOptionDialog(null, panel, "Configure transition visibility",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0])) {
				for (Transition t : unmappedTrans) {
					t.setInvisible(true);
				}
			}
		}

		configurationStepsDialogs = new JComponent[CONFIGURATION_STEPS_NUMBER];
		configurationStepsDialogs[0] = new PlannerSettingsDialog(log);
		currentConfigurationStep = 0;
		Object[] configurationResults = showConfiguration(context, log, petrinet, mapping);
		
		if (configurationResults != null) {
			parameters = new PlanningBasedAlignmentParameters();
			
			// at this point, the initial marking has been created
			InitialMarkingConnection initialConn = context.getConnectionManager().getFirstConnection(
					InitialMarkingConnection.class, context, petrinet);
			Marking initialMarking = (Marking) initialConn.getObjectWithRole(InitialMarkingConnection.MARKING);
			parameters.setInitialMarking(initialMarking);
			
			// at this point, the final marking has been created
			FinalMarkingConnection finalConn = context.getConnectionManager().getFirstConnection(
					FinalMarkingConnection.class, context, petrinet);
			Marking finalMarking = (Marking) finalConn.getObjectWithRole(FinalMarkingConnection.MARKING);
			parameters.setFinalMarking(finalMarking);
			
			parameters.setTransitionsEventsMapping((TransEvClassMapping) configurationResults[TRANSITIONS_EVENT_CLASSES_MAPPING]);
			parameters.setPlannerSearchStrategy((PlannerSearchStrategy) configurationResults[PLANNER_SEARCH_STRATEGY]);
			parameters.setTracesInterval((int[]) configurationResults[TRACES_INTERVAL]);
			parameters.setTracesLengthBounds((int[]) configurationResults[TRACES_LENGTH_BOUNDS]);
			parameters.setMovesOnLogCosts((Map<XEventClass, Integer>) configurationResults[MOVES_ON_LOG_COSTS]);
			parameters.setMovesOnModelCosts((Map<Transition, Integer>) configurationResults[MOVES_ON_MODEL_COSTS]);
			parameters.setSynchronousMovesCosts((Map<Transition, Integer>) configurationResults[SYNCHRONOUS_MOVES_COSTS]);
		}
		return parameters;
	}

	
	/**
	 * Check the existence of an initial marking for the given Petri net. If no, create one.
	 * 
	 * @param context
	 * @param petrinet
	 */
	private void checkInitialMarking(UIPluginContext context, DataPetriNet petrinet) {
		try {
			InitialMarkingConnection initCon = context.getConnectionManager().getFirstConnection(
					InitialMarkingConnection.class, context, petrinet);
			if (((Marking) initCon.getObjectWithRole(InitialMarkingConnection.MARKING)).isEmpty()) {
				JOptionPane.showMessageDialog(
						new JPanel(),
						"The initial marking is an empty marking. If this is not intended, remove the currently existing "
								+ "InitialMarkingConnection object and then use \"Create Initial Marking\" plugin to create a "
								+ "non-empty initial marking.",
								"Empty Initial Marking", JOptionPane.INFORMATION_MESSAGE);
			}
		} catch (ConnectionCannotBeObtained exc) {
			if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(new JPanel(),
					"No initial marking is found for this model. Do you want to create one?", "No Initial Marking",
					JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE)) {
				createMarking(context, petrinet, InitialMarkingConnection.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * Check the existence of an final marking for the given Petri net. If no, create one.
	 * 
	 * @param context
	 * @param petrinet
	 */
	private void checkFinalMarking(UIPluginContext context, DataPetriNet petrinet) {
		// check existence of final marking
		try {
			context.getConnectionManager().getFirstConnection(FinalMarkingConnection.class, context, petrinet);
		} catch (ConnectionCannotBeObtained exc) {
			if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(new JPanel(),
					"No final marking is found for this model. Do you want to create one?", "No Final Marking",
					JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE)) {
				createMarking(context, petrinet, FinalMarkingConnection.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Trigger the creation of a marking (initial or final) for the given Petri net.
	 * 
	 * @param context The current context.
	 * @param petrinet The Petri net.
	 * @param markingType The type of marking to be created.
	 * @return
	 */
	private boolean createMarking(UIPluginContext context, PetrinetGraph petrinet, Class<? extends Connection> markingType) {
		boolean result = false;
		Collection<Pair<Integer, PluginParameterBinding>> plugins = context.getPluginManager().find(
				ConnectionObjectFactory.class, markingType, context.getClass(), true, false, false, petrinet.getClass());
		PluginContext c2 = context.createChildContext("Creating connection of Type " + markingType);
		Pair<Integer, PluginParameterBinding> pair = plugins.iterator().next();
		PluginParameterBinding binding = pair.getSecond();
		try {
			PluginExecutionResult pluginResult = binding.invoke(c2, petrinet);
			pluginResult.synchronize();
			context.getProvidedObjectManager().createProvidedObjects(c2); // push the objects to main context
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			c2.getParentContext().deleteChild(c2);
		}
		return result;
	}

	/**
	 * 
	 * @param context
	 * @param log
	 * @param net
	 * @param mapping
	 * @return
	 */
	private Object[] showConfiguration(UIPluginContext context, XLog log, PetrinetGraph net, TransEvClassMapping mapping) {
		// init result variable
		InteractionResult result = InteractionResult.NEXT;

		// configure interaction with user
		while (true) {
			if (currentConfigurationStep < 0) {
				currentConfigurationStep = 0;
			}
			if (currentConfigurationStep >= CONFIGURATION_STEPS_NUMBER) {
				currentConfigurationStep = CONFIGURATION_STEPS_NUMBER - 1;
			}

			result = context.showWizard("Replay in Petri net",
					currentConfigurationStep == 0,
					currentConfigurationStep == CONFIGURATION_STEPS_NUMBER - 1,
					configurationStepsDialogs[currentConfigurationStep]);

			switch (result) {
			case NEXT :
				move(1, mapping);
				break;
			case PREV :
				move(-1, mapping);
				break;
			case FINISHED :
				return new Object[] {
					// the order must match with the constants defined at the beginning of the class
					mapping,
					((PlannerSettingsDialog) configurationStepsDialogs[0]).getChosenStrategy(),
					((PlannerSettingsDialog) configurationStepsDialogs[0]).getChosenTracesInterval(),
					((PlannerSettingsDialog) configurationStepsDialogs[0]).getChosenTracesLengthBounds(),
					((AlignmentSettingsDialog) configurationStepsDialogs[1]).getMovesOnLogCosts(),
					((AlignmentSettingsDialog) configurationStepsDialogs[1]).getMovesOnModelCosts(),
					((AlignmentSettingsDialog) configurationStepsDialogs[1]).getSynchronousMovesCosts()
				};
			default :
				return null;
			}
		}
	}

	/**
	 * Move to next/previous step in configuration.
	 * 
	 * @param direction
	 * @param mapping
	 * @return
	 */
	private int move(int direction, TransEvClassMapping mapping) {
		currentConfigurationStep += direction;

		// check which algorithm is selected and adjust parameter as necessary
		if (currentConfigurationStep == 1) {
			configurationStepsDialogs[1] = new AlignmentSettingsDialog(mapping.keySet(), mapping.values());
		}

		if ((currentConfigurationStep >= 0) && (currentConfigurationStep < CONFIGURATION_STEPS_NUMBER)) {
			return currentConfigurationStep;
		}
		return 0;
	}

}
