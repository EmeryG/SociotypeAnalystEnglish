package org.socionicasys.analyst;

import org.socionicasys.analyst.model.AData;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.text.SimpleAttributeSet;

/**
 * Представление части документа для буфера обмена.
 */
public class ADocumentFragment implements Transferable, Serializable {
	private static final long serialVersionUID = -5267960680238237369L;

	private static DataFlavor nativeFlavor;

	private final String text;
	private final Map<FixedDocumentSection, SimpleAttributeSet> styleMap;
	private final Map<FixedDocumentSection, AData> aDataMap;

	/**
	 * Создает представление фрагмента документа без пометок.
	 * @param text текст фрагмента
	 */
	public ADocumentFragment(String text) {
		this.text = text;
		styleMap = new HashMap<FixedDocumentSection, SimpleAttributeSet>();
		aDataMap = new HashMap<FixedDocumentSection, AData>();
	}

	/**
	 * Создает представление фрагмента документа со стилями и пометками пользователя.
	 * @param text текст фрагмента
	 * @param styleMap стили фрагмента
	 * @param aDataMap пометки фрагмента
	 */
	public ADocumentFragment(String text, Map<FixedDocumentSection, SimpleAttributeSet> styleMap,
			Map<FixedDocumentSection, AData> aDataMap) {
		this(text);
		this.styleMap.putAll(styleMap);
		this.aDataMap.putAll(aDataMap);
	}

	/**
	 * @return текст фрагмента
	 */
	public String getText() {
		return text;
	}

	/**
	 * @return стили фрагмента
	 */
	public Map<FixedDocumentSection, SimpleAttributeSet> getStyleMap() {
		return Collections.unmodifiableMap(styleMap);
	}

	/**
	 * @return пометки фрагмента
	 */
	public Map<FixedDocumentSection, AData> getADataMap() {
		return Collections.unmodifiableMap(aDataMap);
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (flavor.equals(getNativeFlavor())) {
			return this;
		} else if (flavor.equals(DataFlavor.stringFlavor)) {
			return text;
		} else {
			throw new UnsupportedFlavorException(flavor);
		}
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[]{getNativeFlavor(), DataFlavor.stringFlavor};
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor.equals(getNativeFlavor()) || flavor.equals(DataFlavor.stringFlavor);
	}

	/**
	 * Инициализирует DataFlavor, предствляющий ADocumentFragment.
	 * @return DataFlavor для представлнения фрагмента в документе
	 */
	public static DataFlavor getNativeFlavor() {
		if (nativeFlavor == null) {
			nativeFlavor = new DataFlavor(ADocumentFragment.class, "ADocumentFragment");
		}
		return nativeFlavor;
	}
}
