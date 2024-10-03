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

import org.freeplane.core.util.Hyperlink;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.filter.StringMatchingStrategy;
import org.freeplane.features.filter.StringMatchingStrategy.Type;
import org.freeplane.features.filter.condition.ConditionFactory;
import org.freeplane.features.filter.condition.ConditionFactory.ConditionOperator;
import org.freeplane.n3.nanoxml.XMLElement;

/**
 * @author Dimitry Polivaev
 * Mar 7, 2009
 */
public class HyperLinkEqualsCondition extends HyperLinkCondition {
	public static final String NAME = "hyper_link_equals";

	private final StringMatchingStrategy stringMatchingStrategy;

	public HyperLinkEqualsCondition(final String hyperlink, final boolean matchCase, final boolean matchApproximately, boolean ignoreDiacritics) {
		super(hyperlink, matchCase, matchApproximately, false, ignoreDiacritics);
		this.stringMatchingStrategy = matchApproximately ? StringMatchingStrategy.DEFAULT_APPROXIMATE_STRING_MATCHING_STRATEGY :
			StringMatchingStrategy.EXACT_STRING_MATCHING_STRATEGY;
	}

	@Override
	protected boolean checkLink(final Hyperlink nodeLink) {
		return stringMatchingStrategy.matches(normalizedValue(), normalize(nodeLink), Type.ALL);
	}

	@Override
	protected String createDescription() {
		final String condition = TextUtils.getText(LinkConditionController.FILTER_LINK);
		final String simpleCondition = TextUtils.getText(ConditionFactory.FILTER_IS_EQUAL_TO);
		return createDescription(condition, simpleCondition, getHyperlink());
	}

    @Override
    protected List<Icon> createRenderedIcons(FontMetrics fontMetrics) {
        final String condition = TextUtils.getText(LinkConditionController.FILTER_LINK);
        return createRenderedIcons(condition, ConditionOperator.FILTER_IS_EQUAL_TO, getHyperlink(), fontMetrics);
    }

	@Override
	protected String getName() {
		return NAME;
	}

	@Override
	public void fillXML(final XMLElement child) {
		super.fillXML(child);
	}

}
