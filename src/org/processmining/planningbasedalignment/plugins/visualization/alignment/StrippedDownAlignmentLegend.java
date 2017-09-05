package org.processmining.planningbasedalignment.plugins.visualization.alignment;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.processmining.plugins.DataConformance.visualization.DataAwareStepTypes;

import com.fluxicon.slickerbox.factory.SlickerFactory;

import info.clearthought.layout.TableLayout;

/**
 * Borrowed from DataAwareReplayer.
 *
 */
public class StrippedDownAlignmentLegend extends JPanel {
	
	private static final long serialVersionUID = -5015916679610280259L;

	public StrippedDownAlignmentLegend() {
		super();
		setOpaque(false);		
		setBorder(BorderFactory.createEmptyBorder());
		setToolTipText("Click to hide");
		TableLayout layout = new TableLayout(new double[][] { { 0.10, TableLayout.FILL }, {} });
		setLayout(layout);
		setForeground(null);
		setMaximumSize(new Dimension(430, 0));
		
		SlickerFactory factory = SlickerFactory.instance();

		layout.insertRow(0, 0.2);
		int row = 1;

		layout.insertRow(row, TableLayout.PREFERRED);
		JLabel legend = factory.createLabel("LEGEND");
		legend.setFont(legend.getFont().deriveFont(Font.BOLD));
		add(legend, "0,1,1,1,c, c");
		row++;

		layout.insertRow(row, 0.2);

		layout.insertRow(row, TableLayout.PREFERRED);
		JPanel lmGoodPanel = new JPanel();		
		lmGoodPanel.setBackground(DataAwareStepTypes.LMGOOD.getColor());
		lmGoodPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		add(lmGoodPanel, "0," + row + ",r, c");
		JLabel syncLbl = factory.createLabel(" - Perfect Alignment Step (Move log and model)");
		syncLbl.setForeground(null);
		add(syncLbl, "1," + row++ + ",l, c");

		layout.insertRow(row, TableLayout.PREFERRED);
		JPanel mRealPanel = new JPanel();
		mRealPanel.setBackground(DataAwareStepTypes.MREAL.getColor());
		mRealPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		add(mRealPanel, "0," + row + ",r, c");
		JLabel moveRealLbl = factory.createLabel(" - Missing Event (Move model only)");
		moveRealLbl.setForeground(null);
		add(moveRealLbl, "1," + row++ + ",l, c");

		layout.insertRow(row, TableLayout.PREFERRED);
		JPanel mInviPanel = new JPanel();
		mInviPanel.setBackground(DataAwareStepTypes.MINVI.getColor());
		mInviPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		add(mInviPanel, "0," + row + ",r, c");
		JLabel moveInviLbl = factory.createLabel(" - Unobservable Event (Move model only)");
		moveInviLbl.setForeground(null);
		add(moveInviLbl, "1," + row++ + ",l, c");

		layout.insertRow(row, TableLayout.PREFERRED);
		JPanel lPanel = new JPanel();
		lPanel.setBackground(DataAwareStepTypes.L.getColor());
		lPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		add(lPanel, "0," + row + ",r, c");
		JLabel moveLogLbl = factory.createLabel(" - Wrong Event (Move log only)");
		moveLogLbl.setForeground(null);
		add(moveLogLbl, "1," + row++ + ",l, c");

		layout.insertRow(row, 0.2);
		add(javax.swing.Box.createVerticalStrut(5), "0," + row + ",r, c");
		
		addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent e) {
				setVisible(false);
			}
			
		});
	}

}
