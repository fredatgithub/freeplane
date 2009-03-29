/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.features.mindmapmode.note;

import java.awt.event.ActionEvent;
import java.util.Iterator;

import javax.swing.JOptionPane;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.freeplane.core.modecontroller.ModeController;
import org.freeplane.core.model.NodeModel;
import org.freeplane.core.resources.ResourceControllerProperties;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.components.OptionalDontShowMeAgainDialog;
import org.freeplane.features.common.note.NoteModel;

class RemoveNoteAction extends AFreeplaneAction implements PopupMenuListener {
	/**
	 *
	 */
	final private MNoteController noteController;

	public RemoveNoteAction(final MNoteController noteController, final ModeController modeController) {
		super(modeController.getController(), "accessories/plugins/RemoveNote.properties_name");
		this.noteController = noteController;
	}

	public void actionPerformed(final ActionEvent e) {
		final int showResult = new OptionalDontShowMeAgainDialog(getController(), "really_remove_notes",
		    "confirmation",
		    ResourceControllerProperties.RESOURCES_REMOVE_NOTES_WITHOUT_QUESTION,
		    OptionalDontShowMeAgainDialog.ONLY_OK_SELECTION_IS_STORED).show().getResult();
		if (showResult != JOptionPane.OK_OPTION) {
			return;
		}
		for (final Iterator iterator = (getModeController()).getMapController().getSelectedNodes().iterator(); iterator
		    .hasNext();) {
			final NodeModel node = (NodeModel) iterator.next();
			if (NoteModel.getNoteText(node) != null) {
				removeNote(node);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * javax.swing.event.PopupMenuListener#popupMenuCanceled(javax.swing.event
	 * .PopupMenuEvent)
	 */
	public void popupMenuCanceled(final PopupMenuEvent e) {
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * javax.swing.event.PopupMenuListener#popupMenuWillBecomeInvisible(javax
	 * .swing.event.PopupMenuEvent)
	 */
	public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * javax.swing.event.PopupMenuListener#popupMenuWillBecomeVisible(javax.
	 * swing.event.PopupMenuEvent)
	 */
	public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
		setEnabled(isEnabled());
	}

	private void removeNote(final NodeModel node) {
		if ((getModeController()).getMapController().getSelectedNode() == node) {
			noteController.getHtmlEditorPanel().setCurrentDocumentContent("");
		}
		noteController.setNoteText(node, null);
	}

	@Override
	public void setEnabled() {
		boolean foundNote = false;
		final ModeController modeController = getModeController();
		if (modeController == null) {
			setEnabled(false);
			return;
		}
		for (final Iterator iterator = modeController.getMapController().getSelectedNodes().iterator(); iterator
		    .hasNext();) {
			final NodeModel node = (NodeModel) iterator.next();
			if (NoteModel.getNoteText(node) != null) {
				foundNote = true;
				break;
			}
		}
		setEnabled(foundNote);
	}
}
