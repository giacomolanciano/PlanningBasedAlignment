package org.processmining.planningbasedalignment.plugins.visualization.alignment;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.deckfour.xes.model.XEvent;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.ui.widgets.ProMTableWithoutPanel;
import org.processmining.framework.util.ui.widgets.ProMTextField;
import org.processmining.framework.util.ui.widgets.helper.ProMUIHelper;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceList;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceList.ClickListener;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceList.TraceBuilder;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceView.Event;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceView.Trace;
import org.processmining.framework.util.ui.widgets.traceview.masterdetail.DetailView;
import org.processmining.framework.util.ui.widgets.traceview.model.FilteredListModelImpl;
import org.processmining.framework.util.ui.widgets.traceview.model.FilteredListModelImpl.ListModelFilter;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.algorithms.PlanningBasedAlignment;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.models.PlanningBasedReplayResult;
import org.processmining.plugins.DataConformance.visualization.alignment.AlignmentListView;
import org.processmining.plugins.DataConformance.visualization.alignment.AlignmentListView.XAlignmentOrdering;
import org.processmining.plugins.DataConformance.visualization.alignment.AlignmentQueryPredicate;
import org.processmining.plugins.DataConformance.visualization.alignment.AlignmentTrace;
import org.processmining.plugins.DataConformance.visualization.alignment.AlignmentTrace.DeviationsSetting;
import org.processmining.plugins.DataConformance.visualization.alignment.AlignmentTrace.InvisibleSetting;
import org.processmining.plugins.DataConformance.visualization.alignment.AttributesComparisonPanel;
import org.processmining.plugins.DataConformance.visualization.alignment.ColorTheme;
import org.processmining.plugins.DataConformance.visualization.alignment.XTraceResolver;
import org.processmining.xesalignmentextension.XAlignmentExtension;
import org.processmining.xesalignmentextension.XAlignmentExtension.XAlignment;
import org.processmining.xesalignmentextension.XAlignmentExtension.XAlignmentMove;
import org.processmining.xesalignmentextension.XDataAlignmentExtension;
import org.processmining.xesalignmentextension.XDataAlignmentExtension.DataMoveType;
import org.processmining.xesalignmentextension.XDataAlignmentExtension.XDataAlignmentExtensionException;
import org.processmining.xeslite.query.syntax.ParseException;

import com.fluxicon.slickerbox.factory.SlickerFactory;
import com.google.common.base.Predicate;

/**
 * Borrowed from DataAwareReplayer.
 *
 */
public class StrippedDownAlignmentView extends JPanel implements DetailView<XAlignment> {

	private final class SearchAction implements ActionListener {
		private final JPanel searchPanel;
		private final ProMTextField searchQueryField;

		private SearchAction(JPanel searchPanel, ProMTextField searchQueryField) {
			this.searchPanel = searchPanel;
			this.searchQueryField = searchQueryField;
		}

		public void actionPerformed(ActionEvent e) {
			try {
				String query = searchQueryField.getText();
				if (query.isEmpty()) {
					listView.filter(new ListModelFilter<XAlignment>() {

						public boolean accept(XAlignment a) {
							return true;
						}

					});
				} else {
					final Predicate<XAlignment> predicate = new AlignmentQueryPredicate(searchQueryField.getText());
					;
					listView.filter(new ListModelFilter<XAlignment>() {

						public boolean accept(XAlignment a) {
							return predicate.apply(a);
						}
					});
				}
				// TODO disable stats update after query (for the time being)
				//updateStatistics();
			} catch (ParseException e1) {
				ProMUIHelper.showErrorMessage(searchPanel, e1.getMessage(), "Error parsing query");
			}
		}
	}

	public enum Layout {
		TWOCOLUMN, ONECOLUMN
	}

	private static final long serialVersionUID = 1L;

	private final XTraceResolver traceResolver;
	private final Map<String, Color> activityColorMap;
	private Comparator<XAlignment> currentOrder = XAlignmentOrdering.FITNESS_DESC;

	private final AlignmentListView listView;
	private DefaultTableModel statisticsModel;
	private PlanningBasedReplayResult replayResult;

	public StrippedDownAlignmentView(
			Layout layout, PluginContext context, XTraceResolver traceMap, Map<String, Color> activityColorMap,
			PlanningBasedReplayResult replayResult) {
		
		super();
		this.traceResolver = traceMap;
		this.activityColorMap = activityColorMap;
		this.replayResult = replayResult;

		if (layout == Layout.TWOCOLUMN) {

			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setOpaque(true);
			listView = createListView(context);
			listView.showToolbar();
			
			JPanel mainPanel = new JPanel();
			preparePanel(mainPanel, BoxLayout.X_AXIS);

			JPanel leftPanel = new JPanel();
			leftPanel.setAlignmentY(Component.TOP_ALIGNMENT);
			preparePanel(leftPanel, BoxLayout.Y_AXIS);
			leftPanel.add(createHeading("ALIGNMENTS"));
			leftPanel.add(listView);

			listView.getListModel().addListDataListener(new ListDataListener() {

				public void intervalRemoved(ListDataEvent e) {
					// TODO disable stats update after query (for the time being)
					//updateStatistics();
				}

				public void intervalAdded(ListDataEvent e) {
					updateStatistics();
				}

				public void contentsChanged(ListDataEvent e) {
					// TODO disable stats update after query (for the time being)
					//updateStatistics();
				}
			});

			JPanel rightPanel = new JPanel();
			rightPanel.setAlignmentY(Component.TOP_ALIGNMENT);
			preparePanel(rightPanel, BoxLayout.Y_AXIS);
			rightPanel.add(new StrippedDownAlignmentLegend());

			rightPanel.add(createHeading("ALIGNMENT STATISTICS"));
			rightPanel.add(Box.createVerticalStrut(5));
			rightPanel.add(createStatisticsPanel());

			rightPanel.add(Box.createVerticalStrut(10));

			rightPanel.add(createHeading("SEARCH IN ALIGNMENTS"));
			rightPanel.add(Box.createVerticalStrut(5));
			rightPanel.add(createDetailFilterPanel());
			rightPanel.add(Box.createVerticalStrut(200));  // to force panel to show all contents


			JPanel bottomPanel = new JPanel();
			preparePanel(bottomPanel, BoxLayout.X_AXIS);
			bottomPanel.add(createHeading("COLOR THEME"));
			bottomPanel.add(Box.createHorizontalStrut(5));
			bottomPanel.add(createColorPicker());
			bottomPanel.add(Box.createHorizontalStrut(10));
			bottomPanel.add(createHeading("SORTING"));
			bottomPanel.add(Box.createHorizontalStrut(5));
			bottomPanel.add(createDetailSortPanel());
			bottomPanel.add(Box.createHorizontalGlue());
			JCheckBox colorActivitiesCheckBox = createColorCodeCheckbox();
			bottomPanel.add(colorActivitiesCheckBox);
			JCheckBox deviationsCheckBox = createHighlightDeviationsCheckbox();
			bottomPanel.add(deviationsCheckBox);
			JCheckBox unobserveavleCheckBox = createObservableCheckbox();
			bottomPanel.add(unobserveavleCheckBox);

			VisualizationOptionsListener listener = new VisualizationOptionsListener(deviationsCheckBox,
					unobserveavleCheckBox, colorActivitiesCheckBox);
			unobserveavleCheckBox.addActionListener(listener);
			deviationsCheckBox.addActionListener(listener);
			colorActivitiesCheckBox.addActionListener(listener);

			mainPanel.add(leftPanel);
			mainPanel.add(rightPanel);
			add(mainPanel);
			add(bottomPanel);

			setColor(ColorTheme.BRIGHT);

		} else {

			preparePanel(this, BoxLayout.Y_AXIS);
			listView = createListView(context);
			add(listView);

			add(Box.createVerticalStrut(10));
		}
	}

	private final class VisualizationOptionsListener implements ActionListener {

		private final JCheckBox deviationsCheckBox;
		private final JCheckBox unobserveavleCheckBox;
		private final JCheckBox colorActivitiesCheckBox;

		private VisualizationOptionsListener(JCheckBox deviationsCheckBox, JCheckBox unobserveavleCheckBox,
				JCheckBox colorActivitiesCheckBox) {
			this.deviationsCheckBox = deviationsCheckBox;
			this.unobserveavleCheckBox = unobserveavleCheckBox;
			this.colorActivitiesCheckBox = colorActivitiesCheckBox;
		}

		public void actionPerformed(ActionEvent e) {
			getListView().setTraceBuilder(new TraceBuilder<XAlignment>() {

				public Trace<? extends Event> build(XAlignment a) {
					InvisibleSetting invisible = unobserveavleCheckBox.isSelected() ? InvisibleSetting.HIDDEN
							: InvisibleSetting.VISIBLE;
					DeviationsSetting deviations = deviationsCheckBox.isSelected() ? DeviationsSetting.HIGHLIGHTED
							: DeviationsSetting.NORMAL;
					boolean colorCode = colorActivitiesCheckBox.isSelected();
					return new AlignmentTrace(a, activityColorMap, invisible, deviations, colorCode);
				}

			});
			if (deviationsCheckBox.getModel().isSelected()) {
				getListView().setFixedWedgeLimit(Integer.MAX_VALUE);
			} else {
				getListView().setFixedWedgeLimit(10000);
			}
			getListView().repaint();

		}
	}

	private JCheckBox createHighlightDeviationsCheckbox() {
		final JCheckBox observableCheckBox = SlickerFactory.instance().createCheckBox("Highlight Deviations", false);
		observableCheckBox.setAlignmentX(Component.CENTER_ALIGNMENT);
		return observableCheckBox;
	}

	private JCheckBox createColorCodeCheckbox() {
		final JCheckBox observableCheckBox = SlickerFactory.instance().createCheckBox("Color Code Activities", false);
		observableCheckBox.setAlignmentX(Component.CENTER_ALIGNMENT);
		return observableCheckBox;
	}

	private JCheckBox createObservableCheckbox() {
		final JCheckBox observableCheckBox = SlickerFactory.instance().createCheckBox("Hide unobservable events",
				false);
		observableCheckBox.setAlignmentX(Component.CENTER_ALIGNMENT);
		return observableCheckBox;
	}

	public void setColor(ColorTheme theme) {
		setBackground(theme.getBackground());
		setForeground(theme.getForeground());
	}

	@SuppressWarnings("rawtypes")
	private JComboBox createColorPicker() {
		final JComboBox colorCbx = SlickerFactory.instance().createComboBox(ColorTheme.values());
		colorCbx.setPreferredSize(null);
		colorCbx.setMinimumSize(null);
		colorCbx.setMaximumSize(new Dimension(200, 30));
		colorCbx.setSelectedItem(ColorTheme.BRIGHT);
		colorCbx.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				setColor((ColorTheme) colorCbx.getSelectedItem());
			}
		});
		return colorCbx;
	}

	private void preparePanel(JPanel panel, int orientation) {
		panel.setOpaque(false);
		panel.setBackground(null);
		panel.setForeground(null);
		panel.setLayout(new BoxLayout(panel, orientation));
	}

	private AlignmentListView createListView(final PluginContext context) {
		final AlignmentListView listView = new AlignmentListView(Collections.<XAlignment>emptyList(), activityColorMap);
		listView.setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
		listView.setMaxWedgeWidth(120);
		listView.setCollapsedLabelLength(4);
		listView.addTraceClickListener(new ClickListener<XAlignment>() {

			public void traceMouseDoubleClicked(XAlignment a, int traceIndex, int eventIndex, MouseEvent e) {

				if (eventIndex != -1) {

					eventIndex = correctEventIndex(listView, a, eventIndex);

					XEvent alignmentEvent = a.getTrace().get(eventIndex);
					XAlignmentMove move = XAlignmentExtension.instance().extendEvent(alignmentEvent);
					DataMoveType dataMoveType = XDataAlignmentExtension.instance().extractDataMoveType(alignmentEvent);

					String eventName = move.getModelMove() != null ? move.getModelMove() : move.getLogMove();

					AttributesComparisonPanel comparisonPanel = new AttributesComparisonPanel(context);
					try {
						comparisonPanel.displayAlignmentMove(move);

						JDialog popupDialog = new JDialog();
						popupDialog.setPreferredSize(new Dimension(600, 300));
						if (dataMoveType == null) {
							popupDialog.setTitle(eventName + "(" + move.getType() + ")");
						} else {
							popupDialog.setTitle(eventName + "(" + move.getType() + "/" + dataMoveType + ")");
						}
						popupDialog.add(comparisonPanel);
						popupDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
						popupDialog.pack();
						popupDialog.setLocationRelativeTo(listView);
						popupDialog.setAlwaysOnTop(true);
						popupDialog.setVisible(true);

					} catch (XDataAlignmentExtensionException e1) {
						Object[] options = { "OK" };
						JOptionPane.showOptionDialog(listView, e1.getMessage(), "Error", JOptionPane.PLAIN_MESSAGE,
								JOptionPane.ERROR_MESSAGE, null, options, options[0]);
					}

				}
			}

			public void traceMouseClicked(XAlignment a, int traceIndex, int eventIndex, MouseEvent e) {
				// do nothing
			}

			private final int correctEventIndex(final AlignmentListView listView, XAlignment a, int eventIndex) {
				Trace<? extends Event> t = listView.getTraceBuilder().build(a);
				assert t instanceof AlignmentTrace;
				AlignmentTrace trace = (AlignmentTrace) t;
				if (trace.getInvisible() == InvisibleSetting.HIDDEN) {
					Iterator<XAlignmentMove> it = a.iterator();
					int indexWithoutInv = -1;
					int realIndex = -1;
					while (it.hasNext()) {
						XAlignmentMove move = it.next();
						if (move.isObservable()) {
							indexWithoutInv++;
						}
						realIndex++;
						if (indexWithoutInv == eventIndex) {
							return realIndex;
						}
					}
				}
				return eventIndex;
			}

		});

		listView.getList().addMouseListener(new MouseAdapter() {

			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger())
					showMenu(e);
			}

			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger())
					showMenu(e);
			}

			private void showMenu(MouseEvent e) {
				JPopupMenu menu = new JPopupMenu();
				if (listView.getList().getSelectedIndex() != -1) {

					if (traceResolver.hasOriginalTraces()) {
						final int countSelected = listView.getList().getSelectedIndices().length;
						final JMenuItem menuItemExport = new JMenuItem(
								String.format("Export log traces of %s selected alignment(s)", countSelected));
						menuItemExport.addActionListener(new ActionExportTraces(context, listView, traceResolver));
						menu.add(menuItemExport);
					}

					JMenuItem menuItemSaveAsPDF = new JMenuItem("Save as PDF/EMF/EPS");
					menuItemSaveAsPDF.addActionListener(new ActionSaveAs(listView, getSortOrder()));
					menu.add(menuItemSaveAsPDF);

				}
				menu.show(e.getComponent(), e.getX(), e.getY());
			}

		});
		listView.getList().setToolTipText(
				"Double click on a move to get more information.\n Use right mouse button show the original traces in a new window!");
		return listView;
	}

	private JComponent createDetailSortPanel() {

		@SuppressWarnings("rawtypes")
		final JComboBox sortBox = SlickerFactory.instance().createComboBox(XAlignmentOrdering.values());
		sortBox.setPreferredSize(null);
		sortBox.setMinimumSize(null);
		sortBox.setMaximumSize(new Dimension(200, 30));
		sortBox.setSelectedItem(XAlignmentOrdering.FITNESS_DESC);
		sortBox.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				setSortOrder((XAlignmentOrdering) sortBox.getSelectedItem());
			}
		});

		return sortBox;
	}

	private Component createStatisticsPanel() {
		statisticsModel = new DefaultTableModel();
		statisticsModel.addColumn("Name");
		statisticsModel.addColumn("Value");
		updateStatistics();
		return new ProMTableWithoutPanel(statisticsModel);
	}

	private void updateStatistics() {
		
		NumberFormat integerFormat = NumberFormat.getIntegerInstance();
		NumberFormat percentageFormat = NumberFormat.getPercentInstance();
		NumberFormat realFormat = NumberFormat.getNumberInstance();
		realFormat.setMaximumFractionDigits(2);

		ListModel<XAlignment> model = listView.getListModel();
		int tracesNum = model.getSize();
		statisticsModel.getDataVector().clear();
		statisticsModel.addRow(new String[] { "Count Traces", integerFormat.format(tracesNum)});
		
		// get fitness values from alignments list
		double[] fitness = new double[tracesNum];
		for (int i = 0; i < model.getSize(); i++) {
			XAlignment alignment = model.getElementAt(i);
			fitness[i] = alignment.getFitness();
		}
		statisticsModel.addRow(new String[] { "Average Fitness", percentageFormat.format(StatUtils.mean(fitness))});
		statisticsModel.addRow(new String[] { "Median Fitness", percentageFormat.format(StatUtils.percentile(fitness, 50))});
		
		// time stats
		SummaryStatistics alignmentTimeSummary = replayResult.getAlignmentTimeSummary();
		if (alignmentTimeSummary != null) {
			statisticsModel.addRow(new String[] { "", "" });
			statisticsModel.addRow(new String[] { "Average (actual) Time",
					realFormat.format(alignmentTimeSummary.getMean()) + PlanningBasedAlignment.DEFAULT_TIME_UNIT });
			statisticsModel.addRow(new String[] { "Maximum (actual) Time",
					realFormat.format(alignmentTimeSummary.getMax()) + PlanningBasedAlignment.DEFAULT_TIME_UNIT });
			statisticsModel.addRow(new String[] { "Minimum (actual) Time",
					realFormat.format(alignmentTimeSummary.getMin()) + PlanningBasedAlignment.DEFAULT_TIME_UNIT });
			statisticsModel.addRow(
					new String[] { "Standard deviation", realFormat.format(alignmentTimeSummary.getStandardDeviation())
							+ PlanningBasedAlignment.DEFAULT_TIME_UNIT });
		}
		
		// expanded states stats
		SummaryStatistics expandedStatesSummary = replayResult.getExpandedStatesSummary();
		if (expandedStatesSummary != null) {
			statisticsModel.addRow(new String[] { "", "" });
			statisticsModel.addRow(
					new String[] { "Average Expanded States", realFormat.format(expandedStatesSummary.getMean()) });
			statisticsModel.addRow(
					new String[] { "Average Expanded States", realFormat.format(expandedStatesSummary.getMax()) });
			statisticsModel.addRow(
					new String[] { "Minimum Expanded States", realFormat.format(expandedStatesSummary.getMin()) });
			statisticsModel.addRow( new String[] { 
							"Standard deviation", 
							realFormat.format(expandedStatesSummary.getStandardDeviation()) });
		}
		
		// generate states stats
		SummaryStatistics generatedStatesSummary = replayResult.getGeneratedStatesSummary();
		if (generatedStatesSummary != null) {
			statisticsModel.addRow(new String[] { "", "" });
			statisticsModel.addRow(
					new String[] { "Average Generated States", realFormat.format(generatedStatesSummary.getMean()) });
			statisticsModel.addRow(
					new String[] { "Maximum Generated States", realFormat.format(generatedStatesSummary.getMax()) });
			statisticsModel.addRow(
					new String[] { "Minimum Generated States", realFormat.format(generatedStatesSummary.getMin()) });
			statisticsModel.addRow(new String[] { "Standard deviation",
					realFormat.format(generatedStatesSummary.getStandardDeviation()) });
		}
		
		statisticsModel.fireTableDataChanged();
	}

	private JPanel createDetailFilterPanel() {

		final JPanel detailFilterPanel = new JPanel();
		detailFilterPanel.setLayout(new BoxLayout(detailFilterPanel, BoxLayout.Y_AXIS));
		detailFilterPanel.setOpaque(false);
		detailFilterPanel.setForeground(null);

		final JPanel searchPanel = new JPanel();
		searchPanel.setOpaque(false);
		searchPanel.setForeground(null);
		searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));
		JButton searchQueryBtn = SlickerFactory.instance().createButton("Search");
		final ProMTextField searchQueryField = new ProMTextField();
		searchQueryField.setHint("Filter by trace/event names or by fitness (fitness>0.5)");
		searchQueryField.getTextField().setToolTipText(
				"<html>" 
				+ "Supports a simple query language for filtering alignments by names of events and traces or by their fitness value (e.g., fitness>0.5)."
				+ "<br>Use '~' as 1st character to use a regular expressions, use '%' as 1st character to use a 'contains' query. "
				+ "</html>");
		searchQueryField.setMaximumSize(new Dimension(200, 30));

		SearchAction searchAction = new SearchAction(searchPanel, searchQueryField);
		searchQueryBtn.addActionListener(searchAction);
		searchQueryField.addActionListener(searchAction);

		JButton resetButton = createFilterButton(listView, "Reset", new ListModelFilter<XAlignment>() {

			public boolean accept(XAlignment a) {
				return true;
			}

		});
		resetButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				searchQueryField.setText("");
			}
		});

		searchPanel.add(searchQueryBtn);
		searchPanel.add(resetButton);
		searchPanel.add(searchQueryField);
		detailFilterPanel.add(searchPanel);

		return detailFilterPanel;

	}

	private JLabel createHeading(String label) {
		JLabel title = new JLabel(label);
		title.setAlignmentX(Component.CENTER_ALIGNMENT);
		title.setFont(title.getFont().deriveFont(Font.BOLD));
		title.setForeground(null);
		return title;
	}

	private <T> JButton createFilterButton(final ProMTraceList<T> traceList, String btnName,
			final FilteredListModelImpl.ListModelFilter<T> filter) {
		JButton button = SlickerFactory.instance().createButton(btnName);
		button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				traceList.filter(filter);
				
				// TODO disable stats update after query (for the time being)
				//updateStatistics();
			}

		});
		return button;
	}

	public AlignmentListView getListView() {
		return listView;
	}

	public Comparator<XAlignment> getSortOrder() {
		return currentOrder;
	}

	public void setSortOrder(Comparator<XAlignment> order) {
		currentOrder = order;
		listView.sort(currentOrder);
	}

	public JComponent getDetailComponent() {
		return this;
	}

	public ProMTraceList<XAlignment> getDetailList() {
		return listView;
	}

}
