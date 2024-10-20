package org.freeplane.view.swing.features.time.mindmapmode.nodelist;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.lang.StringUtils;
import org.freeplane.core.ui.components.JComboBoxFactory;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.filter.condition.CJKNormalizer;

class NodeListWithReplacement extends NodeList{
	private class HolderAccessor{
		HolderAccessor() {
	        super();
        }

		private void changeString(final TextHolder textHolder, final String newText) {
			textHolder.setText(newText);
		}

		public int getLength() {
			return mFlatNodeTableFilterModel.getRowCount();
		}

		private TextHolder[] getNodeHoldersAt(final int row) {
			return new TextHolder[]{
			        columnVisibilityChanger.isColumnVisible(nodeTextColumn)  ? (TextHolder) sorter.getValueAt(row, nodeTextColumn) : null,
			        columnVisibilityChanger.isColumnVisible(nodeDetailsColumn)  ? (TextHolder) sorter.getValueAt(row, nodeDetailsColumn) : null,
			        columnVisibilityChanger.isColumnVisible(nodeNotesColumn)  ? (TextHolder) sorter.getValueAt(row, nodeNotesColumn) : null,
			};
		}
	}

	private static final String REMINDER_TEXT_REPLACE = "reminder.Replace";
	final private JComboBox mFilterTextReplaceField;
	private final JCheckBox useRegexInReplace;

	NodeListWithReplacement(String windowTitle, boolean searchInAllMaps, String windowPreferenceStorageProperty) {
		super(windowTitle, searchInAllMaps, windowPreferenceStorageProperty);
		mFilterTextSearchField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent pEvent) {
				if (pEvent.getKeyCode() == KeyEvent.VK_DOWN) {
					mFilterTextReplaceField.requestFocusInWindow();
				}
			}
		});
		mFilterTextReplaceField = JComboBoxFactory.create();
		mFilterTextReplaceField.setEditable(true);
		mFilterTextReplaceField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent pEvent) {
				if (pEvent.getKeyCode() == KeyEvent.VK_DOWN) {
					tableView.requestFocusInWindow();
				}
				else if (pEvent.getKeyCode() == KeyEvent.VK_UP) {
					mFilterTextSearchField.requestFocusInWindow();
				}
			}
		});
		useRegexInReplace = new JCheckBox(TextUtils.getText("regular_expressions"));
	}
	private static String replace(final Pattern p, String input, final String replacement) {
		final String result = HtmlUtils.getReplaceResult(p, input, replacement);
		return result;
	}
	private void replace(final HolderAccessor holderAccessor, boolean selectedOnly) {
		final String searchString = (String) mFilterTextSearchField.getSelectedItem();
		if(searchString == null)
			return;
		final String replaceString = (String) mFilterTextReplaceField.getSelectedItem();
		Pattern p;
		try {
			p = Pattern.compile(useRegexInFind.isSelected() ? searchString : Pattern.quote(searchString),
					matchCase.isSelected() ? 0 : Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
		}
		catch (final PatternSyntaxException e) {
			UITools.errorMessage(TextUtils.format("wrong_regexp", searchString, e.getMessage()));
			return;
		}
		final String replacement = replaceString == null ? "" : replaceString;
		final int length = holderAccessor.getLength();
		for (int i = 0; i < length; i++) {
			if( !selectedOnly || tableView.isRowSelected(i)){
				TextHolder[] textHolders = holderAccessor.getNodeHoldersAt(i);
				for(final TextHolder textHolder:textHolders){
				    if(textHolder == null)
				        continue;
					String text = textHolder.getText();
					final String replaceResult;
					final String literalReplacement = useRegexInReplace.isSelected() ? replacement : Matcher.quoteReplacement(replacement);
					try {
						if (HtmlUtils.isHtml(text)) {
							replaceResult = replace(p, text,literalReplacement);
						}
						else {
						    text = CJKNormalizer.removeSpacesBetweenCJKCharacters(text);
							replaceResult = p.matcher(text).replaceAll(literalReplacement);
						}
					}
					catch (Exception e) {
						final String message = e.getMessage();
						UITools.errorMessage(TextUtils.format("wrong_regexp", replacement, message != null ? message : e.getClass().getSimpleName()));
						return;
					}
					if (!StringUtils.equals(text, replaceResult)) {
						holderAccessor.changeString(textHolder, replaceResult);
					}
				}
			}
		}
		mFlatNodeTableFilterModel.resetFilter();
		mFilterTextSearchField.insertItemAt(mFilterTextSearchField.getSelectedItem(), 0);
		mFilterTextReplaceField.insertItemAt(mFilterTextReplaceField.getSelectedItem(), 0);
		mFilterTextSearchField.setSelectedItem("");
	}

	@Override
	protected void createSpecificUI(final Container contentPane, final GridBagConstraints layoutConstraints) {
		layoutConstraints.gridy++;
		layoutConstraints.weightx = 0.0;
		layoutConstraints.gridwidth = 1;
		contentPane.add(new JLabel(TextUtils.getText(REMINDER_TEXT_REPLACE)), layoutConstraints);
		layoutConstraints.gridx = 4;
		contentPane.add(useRegexInReplace, layoutConstraints);
		layoutConstraints.gridx = 0;
		layoutConstraints.weightx = 1.0;
		layoutConstraints.gridwidth = GridBagConstraints.REMAINDER;
		layoutConstraints.gridy++;
		contentPane.add(mFilterTextReplaceField, layoutConstraints);
	}
	@Override
	protected void createSpecificButtons(final Container container) {
		final AbstractAction replaceAllAction = new AbstractAction(TextUtils
		    .getText("reminder.Replace_All")) {
			/**
			     *
			     */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent arg0) {
				replace(new HolderAccessor(), false);
			}
		};
		final JButton replaceAllButton = new JButton(replaceAllAction);
		final AbstractAction replaceSelectedAction = new AbstractAction(TextUtils
		    .getText("reminder.Replace_Selected")) {
			/**
			     *
			     */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent arg0) {
				replace(new HolderAccessor(), true);
			}
		};
		final JButton replaceSelectedButton = new JButton(replaceSelectedAction);
		replaceSelectedAction.setEnabled(false);
		container.add(replaceAllButton);
		container.add(replaceSelectedButton);
		final ListSelectionModel rowSM1 = tableView.getSelectionModel();
		rowSM1.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				final ListSelectionModel lsm = (ListSelectionModel) e.getSource();
				final boolean enable = !(lsm.isSelectionEmpty());
				replaceSelectedAction.setEnabled(enable);
			}
		});
	}
}
