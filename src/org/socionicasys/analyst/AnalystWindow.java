package org.socionicasys.analyst;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.socionicasys.analyst.model.DocumentSelectionModel;
import org.socionicasys.analyst.panel.ControlsPane;
import org.socionicasys.analyst.service.VersionInfo;
import org.socionicasys.analyst.undo.ActiveUndoManager;
import org.socionicasys.analyst.undo.RedoAction;
import org.socionicasys.analyst.undo.UndoAction;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.StyledEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Dictionary;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("serial")
public class AnalystWindow extends JFrame {
	private static final String WINDOW_TITLE_FORMAT = "%s - %s";

	private static final Logger logger = LoggerFactory.getLogger(AnalystWindow.class);

	private final DocumentHolder documentHolder;
	private final TextPane textPane;
	private final StatusLabel status;
	private final ATree navigateTree;
	private final BTree analysisTree;
	private final MatchMissView histogramTree;
	private final JFileChooser fileChooser;

	private boolean programExit;
	private boolean makeNewDocument;

	private final ActiveUndoManager undoManager = new ActiveUndoManager();

	public AnalystWindow() {
		super(String.format(WINDOW_TITLE_FORMAT, VersionInfo.getApplicationName(), ADocument.DEFAULT_TITLE));

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				onWindowClosing();
			}
		});
		setMinimumSize(new Dimension(600, 400));
		setPreferredSize(new Dimension(1000, 700));
		//Create the text pane and configure it.
		documentHolder = new DocumentHolder(new ADocument());
		textPane = new TextPane(documentHolder);

		fileChooser = new JFileChooser();
		fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(String.format("Files .%s",
				LegacyHtmlFormat.EXTENSION), LegacyHtmlFormat.EXTENSION));

		DocumentSelectionModel selectionModel = new DocumentSelectionModel();
		DocumentSelectionConnector selectionConnector = new DocumentSelectionConnector(textPane, selectionModel);

		JScrollPane scrollPane = new JScrollPane(textPane);
		scrollPane.setPreferredSize(new Dimension(600, 500));
		scrollPane.setMinimumSize(new Dimension(400, 250));

		//Create the text area for the status log and configure it.
		JTextArea commentField = new JTextArea(5, 30);
		commentField.setEditable(false);
		commentField.setLineWrap(true);
		commentField.setWrapStyleWord(true);
		commentField.setMaximumSize(new Dimension(400, 30));
		new CommentConnector(selectionModel, commentField);

		JScrollPane scrollPaneForComment = new JScrollPane(commentField);
		scrollPaneForComment.setMinimumSize(new Dimension(400, 30));
		scrollPaneForComment.setMaximumSize(new Dimension(400, 30));
		scrollPaneForComment.setPreferredSize(new Dimension(400, 30));

		//Create a split pane for the change log and the text area.
		JSplitPane splitPaneV = new JSplitPane(
			JSplitPane.VERTICAL_SPLIT,
			scrollPane, scrollPaneForComment);
		splitPaneV.setOneTouchExpandable(false);

		//Create the status area.
		JPanel statusPane = new JPanel(new BorderLayout());
		status = new StatusLabel(selectionModel, documentHolder);
		JProgressBar progress = new JProgressBar(0, 100);
		progress.setSize(new Dimension(300, 30));

		progress.setVisible(false);
		statusPane.add(status, BorderLayout.WEST);
		statusPane.add(progress, BorderLayout.CENTER);

		// Create tabbed navigation pane
		navigateTree = new ATree(documentHolder);
		analysisTree = new BTree(documentHolder.getModel());
		documentHolder.addADocumentChangeListener(analysisTree);
		histogramTree = new MatchMissView(documentHolder);
		documentHolder.addADocumentChangeListener(histogramTree);
		JTabbedPane navigateTabs = createTabPane();

		JSplitPane splitPaneH = new JSplitPane(
			JSplitPane.HORIZONTAL_SPLIT,
			navigateTabs, splitPaneV);
		splitPaneH.setOneTouchExpandable(true);

		//Add the control panels.
		ControlsPane controlsPane = new ControlsPane(selectionModel);

		navigateTree.addTreeSelectionListener(selectionConnector);
		analysisTree.addTreeSelectionListener(selectionConnector);

		JScrollPane scrollPaneControls = new JScrollPane(controlsPane);
		scrollPaneControls.setMinimumSize(new Dimension(300, 500));
		scrollPaneControls.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		getContentPane().add(splitPaneH, BorderLayout.CENTER);
		getContentPane().add(statusPane, BorderLayout.SOUTH);

		controlsPane.setMargin(new Insets(1, 1, 1, 1));
		controlsPane.setBorderPainted(true);

		getContentPane().add(controlsPane, BorderLayout.EAST);

		JMenu fileMenu = createFileMenu();
		JMenu editMenu = createEditMenu();
		JMenu styleMenu = createStyleMenu();
		JMenu infoMenu = createInfoMenu();
		JMenuBar mb = new JMenuBar();

		mb.add(fileMenu);
		mb.add(editMenu);
		mb.add(styleMenu);
		mb.add(infoMenu);
		setJMenuBar(mb);

		//Start watching for undoable edits and caret changes.
		documentHolder.addUndoableEditListener(undoManager);

		pack();

		textPane.requestFocusInWindow();
	}

	public void openFile(String filename, boolean append) {
		openFile(new File(filename), append);
	}

	public void openFile(File file, boolean append) {
		final LegacyHtmlReader worker = new LegacyHtmlReader(file);
		worker.addPropertyChangeListener(new DocumentLoadListener(documentHolder, append, textPane.getCaretPosition()));
		worker.addPropertyChangeListener(new ProgressWindow(this, "    Loading file...   "));
		worker.execute();

		textPane.requestFocusInWindow();
		status.setText("");
		setTitle(String.format(WINDOW_TITLE_FORMAT, VersionInfo.getApplicationName(), file.getName()));
	}

	private JTabbedPane createTabPane() {
		JTabbedPane navigateTabs = new JTabbedPane();
		navigateTabs.addTab("Navigation", navigateTree.getContainer());
		navigateTabs.addTab("Analysis", analysisTree.getContainer());
		navigateTabs.addTab("Chart", histogramTree.getContainer());
		navigateTabs.setMinimumSize(new Dimension(200, 400));
		navigateTabs.setPreferredSize(new Dimension(300, 400));
		return navigateTabs;
	}

	private JMenu createFileMenu() {
		JMenu menu = new JMenu("File");

		JMenuItem newDocumnet = new JMenuItem("Create new document");
		newDocumnet.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				switch (saveConfirmation()) {
				case JOptionPane.YES_OPTION:
					makeNewDocument = true;
					break;

				case JOptionPane.NO_OPTION:
					initNewDocument();
					break;

				case JOptionPane.CANCEL_OPTION:
				default:
					// Save confirmation was cancelled, do nothing
					break;
				}
			}
		});

		JMenuItem exit = new JMenuItem("Exit");
		exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				programExit = true;
				switch (saveConfirmation()) {
				case JOptionPane.YES_OPTION:
					// Save before exit was chosed. Do nothing: save in handled by saveConfirmation()
					// and exit after save in handled by DocumentSaveListener.
					break;
				
				case JOptionPane.CANCEL_OPTION:
					programExit = false;
					break;

				case JOptionPane.NO_OPTION:
					dispose();
					break;

				default:
					break;
				}
			}
		});

		JMenuItem save = new JMenuItem(new SaveAction(false));
		JMenuItem saveAs = new JMenuItem(new SaveAction(true));

		JMenuItem load = new JMenuItem(new OpenAction(false));
		JMenuItem append = new JMenuItem(new OpenAction(true));

		KeyStroke saveKey = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK);
		save.setAccelerator(saveKey);

		KeyStroke saveAsKey = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);
		saveAs.setAccelerator(saveAsKey);

		KeyStroke loadKey = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK);
		load.setAccelerator(loadKey);

		menu.add(newDocumnet);
		menu.addSeparator();
		menu.add(load);
		menu.add(append);
		menu.addSeparator();
		menu.add(save);
		menu.add(saveAs);
		menu.addSeparator();
		menu.add(exit);

		return menu;
	}

	/**
	 * Create the edit menu.
	 * @return edit menu
	 */
	private JMenu createEditMenu() {
		JMenu menu = new JMenu("Edit");

		//Undo and redo are actions of our own creation.
		JMenuItem undoMenuItem = new JMenuItem(new UndoAction(undoManager));
		KeyStroke undoKey = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK);
		undoMenuItem.setAccelerator(undoKey);
		menu.add(undoMenuItem);

		JMenuItem redoMenuItem = new JMenuItem(new RedoAction(undoManager));
		KeyStroke redoKey = KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK);
		redoMenuItem.setAccelerator(redoKey);
		menu.add(redoMenuItem);

		menu.addSeparator();

		//Get the actions and stick them in the menu.
		Action cutAction = textPane.getAction(DefaultEditorKit.cutAction);
		cutAction.putValue(Action.NAME, "Cut");
		JMenuItem cutMenuItem = new JMenuItem(cutAction);
		cutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
		menu.add(cutMenuItem);

		Action copyAction = textPane.getAction(DefaultEditorKit.copyAction);
		copyAction.putValue(Action.NAME, "Copy");
		JMenuItem copyMenuItem = new JMenuItem(copyAction);
		copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
		menu.add(copyMenuItem);

		Action pasteAction = textPane.getAction(DefaultEditorKit.pasteAction);
		pasteAction.putValue(Action.NAME, "Insert");
		JMenuItem pasteMenuItem = new JMenuItem(pasteAction);
		pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
		menu.add(pasteMenuItem);

		menu.addSeparator();

		Action selectAllAction = textPane.getAction(DefaultEditorKit.selectAllAction);
		selectAllAction.putValue(Action.NAME, "Select all");
		JMenuItem selectAllMenuItem = new JMenuItem(selectAllAction);
		selectAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
		menu.add(selectAllMenuItem);

		menu.addSeparator();

		Action searchAction = new SearchAction(textPane);
		JMenuItem searchMenuItem = new JMenuItem(searchAction);
		searchMenuItem.setAction(searchAction);
		KeyStroke searchKey = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK);
		searchMenuItem.setAccelerator(searchKey);
		menu.add(searchMenuItem);

		return menu;
	}

	/**
	 * Create the style menu.
	 * @return style menu
	 */
	private static JMenu createStyleMenu() {
		JMenu menu = new JMenu("Style");

		Action boldAction = new StyledEditorKit.BoldAction();
		boldAction.putValue(Action.NAME, "Question");
		JMenuItem boldMenuItem = new JMenuItem();
		boldMenuItem.setAction(boldAction);
		KeyStroke boldKey = KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_MASK);
		boldMenuItem.setAccelerator(boldKey);
		menu.add(boldMenuItem);

		Action italicAction = new StyledEditorKit.ItalicAction();
		italicAction.putValue(Action.NAME, "Quotation");
		JMenuItem italicMenuItem = new JMenuItem();
		italicMenuItem.setAction(italicAction);
		KeyStroke italicKey = KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK);
		italicMenuItem.setAccelerator(italicKey);
		menu.add(italicMenuItem);

		return menu;
	}

	// infoMenu
	private JMenu createInfoMenu() {
		JMenu menu = new JMenu("Info");

		Action docProperties = new DocumentPropertiesAction();

		docProperties.putValue(Action.NAME, "Document properties");
		menu.add(docProperties);
		menu.add(new AboutAction(this));
		return menu;
	}

	private void onWindowClosing() {
		programExit = true;
		int option = saveConfirmation();
		if (option == JOptionPane.CANCEL_OPTION) {
			programExit = false;
		}
		else if (option == JOptionPane.NO_OPTION) {
			dispose();
		}
	}

	private int saveConfirmation() {
		int choice = JOptionPane.NO_OPTION;
		// if existing document is not empty
		ADocument document = documentHolder.getModel();
		if (document.getLength() > 0) {
			choice = JOptionPane.showOptionDialog(this,
				"Current document is not empty.\nSave current document?",
				"SSS Analyst",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.ERROR_MESSAGE,
				null,
				new Object[]{"Save", "Don't save", "Cancel"},
				null);
			if (choice == JOptionPane.YES_OPTION) {
				File file = document.getAssociatedFile();
				if (file == null) {
					boolean cancel = false;
					boolean overwrite = false;
					while (!(cancel || overwrite)) {
						fileChooser.setDialogTitle("Saving document");
						int returnVal = fileChooser.showSaveDialog(this);
						if (returnVal == JFileChooser.APPROVE_OPTION) {
							file = fileChooser.getSelectedFile();
							if (file.exists()) {
								Object[] options = {"Yes", "No"};
								int option = JOptionPane.showOptionDialog(this,
									"File with this name already exists!\nDo you want to overwrite it?", "Warning!",
									JOptionPane.YES_NO_OPTION,
									JOptionPane.QUESTION_MESSAGE,
									null,
									options, null);
								if (option == JOptionPane.YES_OPTION) {
									overwrite = true;
								}
							}	//if file doesn't exist
							else {
								cancel = true;
							}
						} else if (returnVal == JFileChooser.CANCEL_OPTION) {
							cancel = true;
						}
					}
				}

				if (file != null) {
					LegacyHtmlWriter iow = new LegacyHtmlWriter(this, document, file);
					iow.addPropertyChangeListener(new ProgressWindow(this, "    Saving file: "));
					iow.addPropertyChangeListener(new DocumentSaveListener());
					iow.execute();
				}
			}
		}
		return choice;
	}// end save confifmation

	public ATree getNavigeTree() {
		return navigateTree;
	}

	private void initNewDocument() {
		ADocument newDocument = new ADocument();
		documentHolder.setModel(newDocument);
		setTitle(String.format(WINDOW_TITLE_FORMAT, VersionInfo.getApplicationName(), newDocument.getProperty(Document.TitleProperty)));
		makeNewDocument = false;
	}

	@SuppressWarnings("SerializableNonStaticInnerClassWithoutSerialVersionUID")
	private final class SaveAction extends AbstractAction {
		private final boolean saveAs;

		private SaveAction(boolean saveAs) {
			super(saveAs ? "Save as..." : "Save");
			this.saveAs = saveAs;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			File saveFile = documentHolder.getModel().getAssociatedFile();
			if (saveAs || saveFile == null) {
				// Если у документа еще нет привязки к имени файла, или выбран пункт «Сохранить как…»,
				// нужно показать диалог сохранения файла
				fileChooser.setDialogTitle(saveAs ? "Save document under a new name" : "Save document");
				int saveResult = fileChooser.showDialog(AnalystWindow.this, saveAs ? "Save as..." : "Save");
				if (saveResult != JFileChooser.APPROVE_OPTION) {
					return;
				}

				String saveFileName = fileChooser.getSelectedFile().getAbsolutePath();
				String suffix = String.format(".%s", LegacyHtmlFormat.EXTENSION);
				if (!saveFileName.endsWith(suffix)) {
					saveFileName += suffix;
				}
				saveFile = new File(saveFileName);

				// Подтверждение замены файла
				if (saveFile.exists()) {
					Object[] options = {"Yes", "No"};
					int replaceResult = JOptionPane.showOptionDialog(AnalystWindow.this,
						"File with this name already exists.\nDo you want to overwrite it?", "Warning!",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE,
						null,
						options,
						null
					);
					if (replaceResult == JOptionPane.NO_OPTION) {
						return;
					}
				}
			}

			ADocument document = documentHolder.getModel();
			LegacyHtmlWriter backgroundWriter = new LegacyHtmlWriter(AnalystWindow.this, document, saveFile);
			backgroundWriter.addPropertyChangeListener(new ProgressWindow(AnalystWindow.this, "    Saving  file: "));
			backgroundWriter.addPropertyChangeListener(new DocumentSaveListener());
			backgroundWriter.execute();
			setTitle(String.format(WINDOW_TITLE_FORMAT, VersionInfo.getApplicationName(), saveFile.getName()));
		}
	}

	@SuppressWarnings("SerializableNonStaticInnerClassWithoutSerialVersionUID")
	private final class OpenAction extends AbstractAction {
		private final boolean append;

		private OpenAction(boolean append) {
			super(append ? "Insert from file..." : "Open...");
			this.append = append;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (!append) {
				status.setText("Open existing document...");
				if (saveConfirmation() == JOptionPane.CANCEL_OPTION) {
					return;
				}
			}
			fileChooser.setDialogTitle(append ? "Insert document" : "Open existing document");
			int openResult = fileChooser.showDialog(AnalystWindow.this, append ? "Insert" : "Open");
			if (openResult == JFileChooser.APPROVE_OPTION) {
				openFile(fileChooser.getSelectedFile(), append);
			}
		}
	}

	/**
	 * Отображает окно свойств документа.
	 */
	@SuppressWarnings("SerializableNonStaticInnerClassWithoutSerialVersionUID")
	private final class DocumentPropertiesAction extends AbstractAction {
		private DocumentPropertiesAction() {
			super("Document properties");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			JTextArea titleField = new JTextArea(1, 30);
			titleField.setLineWrap(true);
			JTextArea expertField = new JTextArea(4, 30);
			expertField.setLineWrap(true);
			JTextArea clientField = new JTextArea(1, 30);
			clientField.setLineWrap(true);
			JTextArea dateField = new JTextArea(1, 30);
			dateField.setLineWrap(true);
			JTextArea commentArea = new JTextArea(5, 30);
			commentArea.setLineWrap(true);

			JLabel lt = new JLabel("Title:");
			lt.setPreferredSize(new Dimension(100, 40));
			lt.setMaximumSize(new Dimension(100, 40));
			JLabel le = new JLabel("Expert(s):");
			le.setPreferredSize(new Dimension(100, 40));
			le.setMaximumSize(new Dimension(100, 40));
			JLabel lc = new JLabel("Interviewee:");
			lc.setPreferredSize(new Dimension(100, 40));
			lc.setMaximumSize(new Dimension(100, 40));
			JLabel ld = new JLabel("Date:");
			ld.setPreferredSize(new Dimension(100, 40));
			ld.setMaximumSize(new Dimension(100, 40));
			JLabel lcm = new JLabel("Comments:");
			lcm.setPreferredSize(new Dimension(100, 40));
			lcm.setMaximumSize(new Dimension(100, 40));

			Panel pt = new Panel();
			Panel pe = new Panel();
			Panel pc = new Panel();
			Panel pd = new Panel();
			Panel ppc = new Panel();
			Panel panel = new Panel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

			pt.add(lt);
			pt.setMinimumSize(new Dimension(500, 40));
			pt.add(new JScrollPane(titleField, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
			pe.add(le);
			pe.setMinimumSize(new Dimension(500, 50));
			pe.add(new JScrollPane(expertField, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
			pc.add(lc);
			pc.setMinimumSize(new Dimension(500, 40));
			pc.add(new JScrollPane(clientField, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
			pd.add(ld);
			pd.setMinimumSize(new Dimension(500, 40));
			pd.add(new JScrollPane(dateField, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
			ppc.add(lcm);
			ppc.setMinimumSize(new Dimension(500, 70));
			ppc.add(new JScrollPane(commentArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));

			ADocument document = documentHolder.getModel();
			String title = (String) document.getProperty(Document.TitleProperty);
			String expert = (String) document.getProperty(ADocument.EXPERT_PROPERTY);
			String client = (String) document.getProperty(ADocument.CLIENT_PROPERTY);
			String date = (String) document.getProperty(ADocument.DATE_PROPERTY);
			String comment = (String) document.getProperty(ADocument.COMMENT_PROPERTY);

			panel.add(pt);
			panel.add(pe);
			panel.add(pc);
			panel.add(pd);
			panel.add(ppc);

			if (title != null) {
				titleField.setText(title);
			}
			if (expert != null) {
				expertField.setText(expert);
			}
			if (client != null) {
				clientField.setText(client);
			}
			if (date != null) {
				dateField.setText(date);
			}
			if (comment != null) {
				commentArea.setText(comment);
			}

			if (JOptionPane.showOptionDialog(AnalystWindow.this,
				panel,
				"Document info",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.INFORMATION_MESSAGE,
				null,
				new Object[]{"Accept", "Cancel"},
				null
			) == JOptionPane.YES_OPTION) {

				String updatedTitle = titleField.getText();
				String updatedExpert = expertField.getText();
				String updatedClient = clientField.getText();
				String updatedDate = dateField.getText();
				String updatedComment = commentArea.getText();

				Dictionary<Object, Object> properties = document.getDocumentProperties();

				properties.put(ADocument.TitleProperty, updatedTitle);
				properties.put(ADocument.CLIENT_PROPERTY, updatedClient);
				properties.put(ADocument.EXPERT_PROPERTY, updatedExpert);
				properties.put(ADocument.DATE_PROPERTY, updatedDate);
				properties.put(ADocument.COMMENT_PROPERTY, updatedComment);
				document.fireADocumentChanged();
			}
		}
	}

	/**
	 * Класс, слушающий состояние сохранения документа. Выполняет действия, отложенные до конца сохранения:
	 * выход из приложения, инициализацию нового докуента.
	 */
	private final class DocumentSaveListener extends SwingWorkerDoneListener<LegacyHtmlWriter> {
		@Override
		protected void swingWorkerDone(LegacyHtmlWriter worker) {
			try {
				worker.get();
				if (programExit) {
					dispose();
				}
				if (makeNewDocument) {
					initNewDocument();
				}
			} catch (InterruptedException e) {
				logger.info("Document loading interrupted", e);
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				logger.error("Error saving document to file", cause);
				JOptionPane.showOptionDialog(null,
					"Error saing the file:\n\n" + cause.getMessage(),
					"Error saing the file",
					JOptionPane.DEFAULT_OPTION,
					JOptionPane.ERROR_MESSAGE,
					null,
					new Object[]{"Close"},
					null);
			}
		}
	}
}
