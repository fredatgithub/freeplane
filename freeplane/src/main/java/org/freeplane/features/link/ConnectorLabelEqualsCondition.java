/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2009 Dimitry Polivaev
 *
 *  This file author is Dimitry Polivaev
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
package org.freeplane.features.link;

import java.awt.FontMetrics;
import java.util.List;

import javax.swing.Icon;

import org.freeplane.core.util.TextUtils;
import org.freeplane.features.filter.StringMatchingStrategy.Type;
import org.freeplane.features.filter.condition.ConditionFactory;
import org.freeplane.features.filter.condition.ConditionFactory.ConditionOperator;

/**
 * @author Dimitry Polivaev
 * Mar 7, 2009
 */
public class ConnectorLabelEqualsCondition extends ConnectorLabelCondition {
	public static final String NAME = "connector_label_equals";

	public ConnectorLabelEqualsCondition(final String text, final boolean matchCase,
			final boolean matchApproximately, boolean ignoreDiacritics) {
		super(text, matchCase, matchApproximately, false, ignoreDiacritics);
	}

	@Override
	public boolean check(final ConnectorModel connector) {
	    LinkController linkController = LinkController.getController();
		final String middleLabel = linkController.getMiddleLabel(connector);
		if (equals(middleLabel)) {
			return true;
		}
		final String sourceLabel = linkController.getSourceLabel(connector);
		if (equals(sourceLabel)) {
			return true;
		}
		final String targetLabel = linkController.getTargetLabel(connector);
		if (equals(targetLabel)) {
			return true;
		}
		return false;
	}

	private boolean equals(final String middleLabel) {
		if (middleLabel == null) {
			return false;
		}
		return getStringMatchingStrategy().matches(normalizedValue(), normalize(middleLabel), Type.ALL);
	}

	@Override
	protected String createDescription() {
		final String condition = TextUtils.getText(LinkConditionController.CONNECTOR_LABEL);
		final String simpleCondition = TextUtils.getText(ConditionFactory.FILTER_IS_EQUAL_TO);
		return createDescription(condition, simpleCondition, getText());
	}

    @Override
    protected List<Icon> createRenderedIcons(FontMetrics fontMetrics) {
        final String condition = TextUtils.getText(LinkConditionController.CONNECTOR_LABEL);
        return createRenderedIcons(condition, ConditionOperator.FILTER_IS_EQUAL_TO, getText(), fontMetrics);
    }

	@Override
	protected String getName() {
		return NAME;
	}
}
