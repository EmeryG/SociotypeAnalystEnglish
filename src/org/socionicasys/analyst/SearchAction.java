package org.socionicasys.analyst;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

@SuppressWarnings("serial")
public class SearchAction extends AbstractAction {
	private static final Logger logger = LoggerFactory.getLogger(SearchAction.class);

	private final JTextComponent textComponent;
	private final JPanel mainPanel;
	private final JTextArea searchQuote;
	private final JCheckBox caseCheckbox;
	private final JLabel status;
	private final ButtonGroup searchDirectionButtons;

	public SearchAction(JTextComponent textComponent) {
		super("Search");
		this.textComponent = textComponent;

		JRadioButton forwardDirection = new JRadioButton("Forward");
		forwardDirection.setActionCommand("f");

		JRadioButton backwardDirection = new JRadioButton("Backward");
		backwardDirection.setActionCommand("b");

		searchDirectionButtons = new ButtonGroup();
		searchDirectionButtons.add(forwardDirection);
		searchDirectionButtons.add(backwardDirection);
		forwardDirection.setSelected(true);

		JButton searchButton = new JButton("Search");

		searchQuote = new JTextArea(3, 30);
		searchQuote.setMaximumSize(new Dimension(400, 100));
		searchQuote.setMinimumSize(new Dimension(400, 100));
		searchQuote.setLineWrap(true);
		searchQuote.setWrapStyleWord(true);
		JPanel p1 = new JPanel();
		JPanel p2 = new JPanel();
		JPanel p3 = new JPanel();
		mainPanel = new JPanel();
		caseCheckbox = new JCheckBox("Case sensitive");
		caseCheckbox.setSelected(false);

		status = new JLabel("   "); //("Введите строку поиска и нажмите кнопку \"Искать\"");
		p2.setLayout(new BorderLayout());
		JScrollPane scrl = new JScrollPane(searchQuote);
		scrl.setBorder(new TitledBorder("Search text:"));
		p1.add(scrl, BorderLayout.CENTER);

		p2.setLayout(new BorderLayout());
		p2.add(forwardDirection, BorderLayout.NORTH);
		p2.add(backwardDirection, BorderLayout.SOUTH);

		p3.setLayout(new BorderLayout());
		p3.add(p2, BorderLayout.EAST);
		p3.add(caseCheckbox, BorderLayout.WEST);
		p3.add(searchButton, BorderLayout.CENTER);

		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.add(p1);
		mainPanel.add(p2);
		mainPanel.add(p3);
		mainPanel.add(status);

		searchButton.addActionListener(this);
	}

	public void showSearchDialog() {
		JOptionPane.showOptionDialog(null,
			mainPanel,
			"Search",
			JOptionPane.OK_OPTION,
			JOptionPane.PLAIN_MESSAGE,
			null,
			new Object[]{"Close"},
			null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if ("Search".equals(e.getActionCommand())) {
			status.setText("");
			showSearchDialog();
		} else {
			boolean caseSensitive = caseCheckbox.isSelected();
			boolean forward = true;
			if ("f".equals(searchDirectionButtons.getSelection().getActionCommand())) {
				forward = true;
			} else if ("b".equals(searchDirectionButtons.getSelection().getActionCommand())) {
				forward = false;
			}

			int searchOffset = textComponent.getCaret().getDot();

			String text = "";
			try {
				Document document = textComponent.getDocument();
				text = document.getText(0, document.getLength());
			} catch (BadLocationException ex) {
				logger.error("Illegal document position in actionPerformed()", ex);
			}

			if (forward) {
				searchOffset = Math.min(searchOffset + 1, text.length());
			} else {
				searchOffset = Math.max(0, searchOffset - 1);
			}

			String searchText = searchQuote.getText();

			if (!caseSensitive) {
				text = text.toLowerCase();
				searchText = searchText.toLowerCase();
			}

			int dot = searchOffset;
			int mark = searchOffset;
			int searchResult;
			if (forward) {
				searchResult = text.indexOf(searchText, searchOffset);
				if (searchResult > 0) {
					dot = Math.min(searchResult + searchText.length(), text.length());
					mark = searchResult;
				}
			} else {
				searchResult = text.substring(0, searchOffset).lastIndexOf(searchText);
				if (searchResult > 0) {
					dot = Math.min(searchResult + searchText.length(), text.length());
					mark = searchResult;
				}
			}

			if (searchResult >= 0) {
				status.setText("Position found: " + searchResult);
				try {
					Rectangle rect = textComponent.modelToView(searchResult);
					((JComponent) textComponent.getParent()).scrollRectToVisible(rect);
					textComponent.getCaret().setDot(dot);
					textComponent.getCaret().moveDot(mark);
					textComponent.requestFocus();
					status.setText("");
				} catch (BadLocationException ex) {
					logger.error("SearchPane: error setting model to view :: bad location", ex);
				}
			} else {
				status.setText("       ...text not found...");
			}
			searchQuote.selectAll();
			searchQuote.requestFocus();
		}
	}
}
