package org.processmining.planningbasedalignment.help;

public class HelpMessages {

	public final static String PLANNING_BASED_ALIGNMENT_PACKAGE = "PlanningBasedAlignment";
	
	public final static String PLANNING_BASED_ALIGNMENT_ARTICLE = 
		"'Aligning Real Process Executions and Prescriptive Process Models through Automated Planning' "
		+ "by M. de Leoni and A. Marrella";
	
	public final static String PLANNING_BASED_ALIGNMENT_HELP = ""
		+ "Given an event log and a Petri net, this plug-in computes the (optimal) traces alignments using "
		+ "Automated Planning as depicted in the article " + PLANNING_BASED_ALIGNMENT_ARTICLE + ".";
	
	public final static String PDDL_FILES_HELP = ""
			+ "Notice that each generated file refers to the trace whose id is indicated in the file name. "
			+ "Besides, \"domain0.pddl\" and \"problem0.pddl\" refer to the alignment of the empty trace, "
			+ "whose cost is needed to compute fitness.";
	
	public final static String ALIGNMENT_PDDL_ENCODING_HELP = ""
		+ "Given an event log and a Petri net, this plug-in generates the PDDL encoding of the alignment problem "
		+ "Automated Planning as depicted in the article " + PLANNING_BASED_ALIGNMENT_ARTICLE + ". "
		+ PDDL_FILES_HELP;

}
