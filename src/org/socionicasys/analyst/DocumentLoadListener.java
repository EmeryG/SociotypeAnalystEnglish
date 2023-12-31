package org.socionicasys.analyst;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.concurrent.ExecutionException;

/**
 * Класс, слушающий состояние загрузки документа. Помещает документ в главное окно по окончанию загрузки.
 */
public final class DocumentLoadListener extends SwingWorkerDoneListener<LegacyHtmlReader> {
	/**
	 * Добавлять ли документ после загрузки к уже существующему.
	 */
	private final boolean append;

	/**
	 * Смещение, по которому нужно добавить текст в существующий документ в случае {@code append = true}.
	 */
	private final int appendOffset;

	/**
	 * Контейнер с документом, в который нужно поместить новый после окончания загрузки.
	 */
	private final DocumentHolder documentHolder;
	
	private static final Logger logger = LoggerFactory.getLogger(DocumentLoadListener.class);

	/**
	 * Creates an object that can observe the process of loading a document with a background thread
	 * {@link LegacyHtmlReader} and will add the loaded document to the specified container when finished.
	 *
	 * @param documentHolder container for the loaded document
	 * @param append Whether to add a new document to the end of an existing document instead of replacing it entirely
	 * @param appendOffset The offset at which to add text to an existing document
	 */

	public DocumentLoadListener(DocumentHolder documentHolder, boolean append, int appendOffset) {
		this.documentHolder = documentHolder;
		this.append = append;
		this.appendOffset = appendOffset;
	}

	@Override
	protected void swingWorkerDone(LegacyHtmlReader worker) {
		try {
			ADocument document = worker.get();
			if (append) {
				documentHolder.getModel().appendDocument(document, appendOffset);
			} else {
				documentHolder.setModel(document);
			}
		} catch (InterruptedException e) {
			logger.info("Document loading interrupted", e);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			logger.error("Error while loading document", cause);
			JOptionPane.showOptionDialog(null,
					String.format("Error while opening file:\n%s", cause.getMessage()),
					"Error while opening file",
					JOptionPane.DEFAULT_OPTION,
					JOptionPane.ERROR_MESSAGE,
					null,
					new Object[]{"Close"},
					null);
		}
	}
}
