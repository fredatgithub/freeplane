/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2013 Dimitry
 *
 *  This file author is Dimitry
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
package org.freeplane.view.swing.features.time.mindmapmode.nodelist;

import org.freeplane.core.util.HtmlUtils;
import org.freeplane.features.filter.condition.CJKNormalizer;
import org.freeplane.features.map.NodeModel;

/**
 * @author Dimitry Polivaev
 * 03.10.2013
 */
/** removes html in notes before comparison. */
class TextHolder implements Comparable<TextHolder> {
	private TextAccessor textAccessor;
	private String originalNotesText = null;
	private String untaggedNotesText = null;


	TextHolder(final TextAccessor textAccessor) {
		this.textAccessor = textAccessor;
	}

	TextHolder(){
	    this(null);
	}

	public void setTextAccessor(TextAccessor textAccessor) {
        this.textAccessor = textAccessor;
    }

    public int compareTo(final TextHolder compareToObject) {
		return toString().compareTo(compareToObject.toString());
	}

	public String getTextAsSingleLine() {
		final String notesText = getText();
		if (notesText == null) {
			return "";
		}
		if (untaggedNotesText == null || (originalNotesText != null && !originalNotesText.equals(notesText))) {
			originalNotesText = notesText;
			String plainText = HtmlUtils.htmlToPlain(notesText).replaceAll("\\s+\\n", " ");
            untaggedNotesText = CJKNormalizer.removeSpacesBetweenCJKCharacters(plainText);
		}
		return untaggedNotesText;
	}

	@Override
	public String toString() {
		return getTextAsSingleLine();
	}

	public void setText(String newText) {
	    textAccessor.setText(newText);

    }

	public NodeModel getNode() {
	    return textAccessor.getNode();
    }

	public String getText() {
	    return textAccessor.getText();
    }
}