package org.processmining.planningbasedalignment.plugins;

import java.io.File;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;

/**
 * The ProM plug-in for generating PDDL encodings for Planning-based Alignment.
 * 
 * @author Giacomo Lanciano
 *
 */
@Plugin(
	name = "Generate PDDL encodings for Planning-based Alignment",
	parameterLabels = { "Event Log", "Petri Net" }, 
	returnLabels = { "PDDL Files" },
	returnTypes = { File.class },
	userAccessible = true,
	categories = PluginCategory.ConformanceChecking,
	keywords = {"conformance", "alignment", "planning"}
)
public class PddlEncodingPlugin {

	private static final String AFFILIATION = "Sapienza University of Rome";
	private static final String AUTHOR = "Giacomo Lanciano";
	private static final String EMAIL = "lanciano.1487019@studenti.uniroma1.it";
	
	@UITopiaVariant(affiliation = AFFILIATION, author = AUTHOR, email = EMAIL)
	@PluginVariant(variantLabel = "Generate PDDL encodings for Planning-based Alignment",
	requiredParameterLabels = { 0, 1 })
	public File run(UIPluginContext context, XLog log, PetrinetGraph petrinet) {
		
		// TODO
		File a = new File("a/");
		a.mkdir();
		
		return a;

	}

}
