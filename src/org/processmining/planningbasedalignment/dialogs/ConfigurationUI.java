package org.processmining.planningbasedalignment.dialogs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.datapetrinets.ui.ImprovedEvClassLogMappingUI;
import org.processmining.datapetrinets.utils.MarkingsHelper;
import org.processmining.framework.connections.Connection;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.connections.annotations.ConnectionObjectFactory;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.PluginExecutionResult;
import org.processmining.framework.plugin.PluginParameterBinding;
import org.processmining.framework.util.Pair;
import org.processmining.framework.util.ui.widgets.helper.UserCancelledException;
import org.processmining.models.connections.petrinets.EvClassLogPetrinetConnection;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.utils.ConnectionManagerHelper;
import org.processmining.plugins.utils.ProvidedObjectHelper;

/**
 * Borrowed from DataAwareReplayer and PNetReplayer.
 * 
 * The GUI for setting the parameters of the Planning-based Alignment plug-in.
 */
public class ConfigurationUI {

	private static final int NEXT_CONFIGURATION_STEP = 1;
	private static final int PREVIOUS_CONFIGURATION_STEP = -1;
	
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

	/**
	 * Build the configuration parameters for Planning-based Alignment plug-in.
	 * 
	 * @param context The context to run in.
	 * @param log The event log to replay.
	 * @param petrinet The Petri net on which the log has to be replayed.
	 * @return The configuration parameters for Planning-based Alignment plug-in.
	 * @throws ConnectionCannotBeObtained
	 * @throws UserCancelledException 
	 */
	public PlanningBasedAlignmentParameters getPlanningBasedAlignmentParameters(
			UIPluginContext context, XLog log, DataPetriNet petrinet) {
		
		// init local parameter
		PlanningBasedAlignmentParameters parameters = null;

		// set Petri net markings
		configureInitialMarking(context, petrinet);
		configureFinalMarking(context, petrinet);

		// define event-classes/activities mapping
		EvClassLogPetrinetConnection conn = null;
		TransEvClassMapping mapping = null;
		try {
			// if a connection between log and Petri net already exists, return the stored mapping
			conn = ConnectionManagerHelper.safeGetFirstConnection(
					context.getConnectionManager(), EvClassLogPetrinetConnection.class, log, petrinet);
			mapping = conn.getObjectWithRole(EvClassLogPetrinetConnection.TRANS2EVCLASSMAPPING);
			
		} catch(ConnectionCannotBeObtained e) {			
			// the connection does not exist, create a new one
			try {
				mapping = defineNewTransEvClassMapping(context, petrinet, log);
			} catch (UserCancelledException e1) {
				// mapping definition has been interrupted, abort execution
				return null;
			}
		}

		// check invisible transitions
		configureInvisibleTransitions(mapping);

		// init gui for each step
		currentConfigurationStep = 0;
		configurationStepsDialogs = new JComponent[CONFIGURATION_STEPS_NUMBER];
		configurationStepsDialogs[0] = new PlannerSettingsDialog(log);
		configurationStepsDialogs[1] = new AlignmentCostsSettingsDialog(mapping.keySet(), mapping.values());
		parameters = runConfigurationSteps(context, log, petrinet, mapping);
		
		if (parameters != null) {			
			// add missing parameters
			parameters.setInitialMarking(getInitialMarking(context, petrinet));
			parameters.setFinalMarking(getFinalMarking(context, petrinet));
			parameters.setTransitionsEventsMapping(mapping);
		}
		return parameters;
	}
	
	/**
	 * Prompt the user to define a mapping between the events classes of the given event log and the activities of the 
	 * given Petri net.
	 * 
	 * @param context The context to run in.
	 * @param log The event log to replay.
	 * @param petrinet The Petri net on which the log has to be replayed.
	 * @return The mapping.
	 * @throws UserCancelledException
	 */
	private static TransEvClassMapping defineNewTransEvClassMapping(
			UIPluginContext context, PetrinetGraph petrinet, XLog log) throws UserCancelledException {

		// list possible classifiers
		List<XEventClassifier> classList = new ArrayList<>(log.getClassifiers());

		// add default classifiers
		if (!classList.contains(XLogInfoImpl.RESOURCE_CLASSIFIER)) {
			classList.add(XLogInfoImpl.RESOURCE_CLASSIFIER);
		}
		if (!classList.contains(XLogInfoImpl.STANDARD_CLASSIFIER)) {
			classList.add(XLogInfoImpl.STANDARD_CLASSIFIER);
		}
		if (!classList.contains(XLogInfoImpl.NAME_CLASSIFIER)) {
			// place Event Name classifier on top of list
			classList.add(0, XLogInfoImpl.NAME_CLASSIFIER);
		}

		Object[] availableEventClass = classList.toArray(new Object[classList.size()]);

		ImprovedEvClassLogMappingUI mappingUi = new ImprovedEvClassLogMappingUI(log, petrinet, availableEventClass);
		InteractionResult result = context.showConfiguration(
				"Mapping between Events from Log and Transitions of the Data Petri Net", mappingUi);

		if (result == InteractionResult.CANCEL) {
			throw new UserCancelledException();
		}

		String logName = XConceptExtension.instance().extractName(log);
		String label = "Connection between " + petrinet.getLabel() + " and " + (logName != null ? logName : "unnamed log");
		EvClassLogPetrinetConnection con = new EvClassLogPetrinetConnection(label, petrinet, log,
				mappingUi.getSelectedClassifier(), mappingUi.getMap());
		context.addConnection(con);

		return mappingUi.getMap();
	}
	
	/**
	 * Ask user whether unmapped transitions have to be considered as invisible.
	 * 
	 * @param mapping The {@link TransEvClassMapping} between the events classes and the Petri net activities.
	 */
	private void configureInvisibleTransitions(TransEvClassMapping mapping) {
		Set<Transition> unmappedTrans = new HashSet<>();
		for (Entry<Transition, XEventClass> entry : mapping.entrySet()) {
			if (entry.getValue().equals(mapping.getDummyEventClass())) {
				if (!entry.getKey().isInvisible()) {
					unmappedTrans.add(entry.getKey());
				}
			}
		}
		if (!unmappedTrans.isEmpty()) {
			JList<Transition> list = new JList<Transition>((Transition[]) unmappedTrans.toArray());
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
			;
		}
	}
	
	/**
	 * Check whether an initial marking for the given Petri net exists. If no, create one.
	 * 
	 * @param context
	 * @param petrinet
	 */
	private void configureInitialMarking(UIPluginContext context, DataPetriNet petrinet) {
		try {	
			InitialMarkingConnection initCon = ConnectionManagerHelper
					.safeGetFirstConnection(context.getConnectionManager(), InitialMarkingConnection.class, petrinet);
			
			if (((Marking) initCon.getObjectWithRole(InitialMarkingConnection.MARKING)).isEmpty()) {
				JOptionPane.showMessageDialog(
						new JPanel(),
						"The initial marking is an empty marking. If this is not intended, remove the currently "
						+ "existing InitialMarkingConnection object and then use \"Create Initial Marking\" plugin "
						+ "to create a non-empty initial marking.",
						"Empty Initial Marking", JOptionPane.INFORMATION_MESSAGE);
			}
		} catch (ConnectionCannotBeObtained exc) {			
			Marking guessedInitialMarking = MarkingsHelper.guessInitialMarkingByStructure(petrinet);
			if (guessedInitialMarking != null) {
				String[] options = new String[] { "Keep guessed", "Create manually" };
				int result = JOptionPane.showOptionDialog(context.getGlobalContext().getUI(),
						"<HTML>No initial marking is found for this model. Based on the net structure the intial "
						+ "marking should be: [<B>" + guessedInitialMarking.iterator().next()
						+ "</B>].<BR/>Do you want to use the guessed marking, or manually create a new one?</HTML>",
						"No Initial Marking", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
						options[0]);
				if (result == 1) {
					createMarking(context, petrinet, InitialMarkingConnection.class);
				} else {
					publishInitialMarking(context, petrinet, guessedInitialMarking);
				}
			} else {
				createMarking(context, petrinet, InitialMarkingConnection.class);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Check whether an final marking for the given Petri net exists. If no, create one.
	 * 
	 * @param context The context to run in.
	 * @param petrinet The Petri net.
	 */
	private void configureFinalMarking(UIPluginContext context, DataPetriNet petrinet) {
		// check existence of final marking
		try {			
			FinalMarkingConnection finalConn = ConnectionManagerHelper
					.safeGetFirstConnection(context.getConnectionManager(), FinalMarkingConnection.class, petrinet);
			Marking finalMarking = finalConn.getObjectWithRole(FinalMarkingConnection.MARKING);
			if (finalMarking.isEmpty()) {
				JOptionPane.showMessageDialog(new JPanel(),
						"The final marking is an empty marking. If this is not intended, remove the currently existing "
						+ "FinalMarkingConnection object and, then, try again!",
						"Empty Final Marking", JOptionPane.WARNING_MESSAGE);
			}
		} catch (ConnectionCannotBeObtained exc) {			
			Marking guessedFinalMarking = MarkingsHelper.guessFinalMarkingByStructure(petrinet);
			if (guessedFinalMarking != null) {
				String[] options = new String[] { "Keep guessed", "Create manually" };
				int result = JOptionPane.showOptionDialog(context.getGlobalContext().getUI(),
						"<HTML>No final marking is found for this model. Based on the net structure place the final "
						+ "marking should be: [<B>"
						+ guessedFinalMarking.iterator().next()
						+ "</B>].<BR/>Do you want to use the guessed marking, or manually create a new one?</HTML>",
						"No Final Marking", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
						options[0]);
				if (result == 1) {
					createMarking(context, petrinet, FinalMarkingConnection.class);
				} else {
					publishFinalMarking(context, petrinet, guessedFinalMarking);
				}
			} else {
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
	 * @return true if the {@link Marking} is successfully created (false otherwise).
	 */
	private boolean createMarking(
			UIPluginContext context, PetrinetGraph petrinet, Class<? extends Connection> markingType) {
		
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
	
	private void publishInitialMarking(UIPluginContext context, PetrinetGraph petrinet, Marking initialMarking) {
		ProvidedObjectHelper.publish(
				context, "Initial Marking of " + petrinet.getLabel(), initialMarking, Marking.class, false);
		Connection connection = new InitialMarkingConnection(petrinet, initialMarking);
		context.getConnectionManager().addConnection(connection);
	}

	private void publishFinalMarking(UIPluginContext context, PetrinetGraph petrinet, Marking finalMarking) {
		ProvidedObjectHelper.publish(
				context, "Final Marking of " + petrinet.getLabel(), finalMarking, Marking.class, false);
		Connection connection = new FinalMarkingConnection(petrinet, finalMarking);
		context.getConnectionManager().addConnection(connection);
	}
	
	private Marking getInitialMarking(PluginContext context, PetrinetGraph net) {
		// check connection between petri net and marking
		Marking initMarking = null;
		try {
			initMarking = context.getConnectionManager()
					.getFirstConnection(InitialMarkingConnection.class, context, net)
					.getObjectWithRole(InitialMarkingConnection.MARKING);
		} catch (ConnectionCannotBeObtained exc) {
			// no final marking provided, give an empty marking
			initMarking = new Marking();
		}
		return initMarking;
	}

	private Marking getFinalMarking(PluginContext context, PetrinetGraph net) {
		// check if final marking exists
		Marking finalMarking = null;
		try {
			finalMarking = context.getConnectionManager()
					.getFirstConnection(FinalMarkingConnection.class, context, net)
					.getObjectWithRole(FinalMarkingConnection.MARKING);
		} catch (ConnectionCannotBeObtained exc) {
			// no final marking provided, give an empty marking
			finalMarking = new Marking();
		}
		return finalMarking;
	}

	/**
	 * Run the configuration process.
	 * 
	 * @param context The context to run in.
	 * @param log The event log.
	 * @param net The Petri net.
	 * @param mapping The {@link TransEvClassMapping} between the events classes and the Petri net activities.
	 * @return The {@link PlanningBasedAlignmentParameters} needed to align the event log and the Petri net.
	 */
	private PlanningBasedAlignmentParameters runConfigurationSteps(
			UIPluginContext context, XLog log, PetrinetGraph net, TransEvClassMapping mapping) {
		
		// init result variable
		InteractionResult interactionResult = InteractionResult.NEXT;

		// configure interaction with user
		while (true) {
			if (currentConfigurationStep < 0) {
				currentConfigurationStep = 0;
			}
			if (currentConfigurationStep >= CONFIGURATION_STEPS_NUMBER) {
				currentConfigurationStep = CONFIGURATION_STEPS_NUMBER - 1;
			}

			interactionResult = context.showWizard("Replay in Petri net",
					currentConfigurationStep == 0,
					currentConfigurationStep == CONFIGURATION_STEPS_NUMBER - 1,
					configurationStepsDialogs[currentConfigurationStep]);

			switch (interactionResult) {
			case NEXT :
				move(NEXT_CONFIGURATION_STEP);
				break;
			case PREV :
				move(PREVIOUS_CONFIGURATION_STEP);
				break;
			case FINISHED :				
				PlannerSettingsDialog plannerSettingsStep = (PlannerSettingsDialog) configurationStepsDialogs[0];
				AlignmentCostsSettingsDialog alignmentCostsSettingsStep = 
						(AlignmentCostsSettingsDialog) configurationStepsDialogs[1];
				
				PlanningBasedAlignmentParameters result = new PlanningBasedAlignmentParameters();
				result.setPlannerSearchStrategy(plannerSettingsStep.getChosenStrategy());
				result.setTracesInterval(plannerSettingsStep.getChosenTracesInterval());
				result.setTracesLengthBounds(plannerSettingsStep.getChosenTracesLengthBounds());
				result.setMovesOnLogCosts(alignmentCostsSettingsStep.getMovesOnLogCosts());
				result.setMovesOnModelCosts(alignmentCostsSettingsStep.getMovesOnModelCosts());
				result.setSynchronousMovesCosts(alignmentCostsSettingsStep.getSynchronousMovesCosts());
				return result;
			default :
				return null;
			}
		}
	}

	/**
	 * Move to next/previous step in configuration.
	 * 
	 * @param direction An int indicating the direction to move toward (either {@link NEXT_CONFIGURATION_STEP} or 
	 * {@link PREVIOUS_CONFIGURATION_STEP}).
	 * @return The index of the current configuration step.
	 */
	private int move(int direction) {
		currentConfigurationStep += direction;

		if (currentConfigurationStep == 1) {
			if (!((PlannerSettingsDialog) configurationStepsDialogs[0]).checkSettingsIntegrity()) {
				JOptionPane.showMessageDialog(new JPanel(),
						"Invalid traces interval and/or length boundaries inserted. Review settings to continue.",
						"Invalid settings", JOptionPane.ERROR_MESSAGE);
				currentConfigurationStep -= 1;
			}
		}

		if ((currentConfigurationStep >= 0) && (currentConfigurationStep < CONFIGURATION_STEPS_NUMBER)) {
			return currentConfigurationStep;
		}
		
		return 0;
	}

}
