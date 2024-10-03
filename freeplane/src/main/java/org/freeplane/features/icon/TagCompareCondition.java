/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Dimitry Polivaev
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
package org.freeplane.features.icon;

import java.awt.FontMetrics;
import java.util.List;

import javax.swing.Icon;

import org.freeplane.core.io.xml.TreeXmlReader;
import org.freeplane.core.io.xml.TreeXmlWriter;
import org.freeplane.core.util.LineComparator;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.filter.condition.ASelectableCondition;
import org.freeplane.features.filter.condition.CompareConditionAdapter;
import org.freeplane.features.filter.condition.ConditionFactory.ConditionOperator;
import org.freeplane.n3.nanoxml.XMLElement;

/**
 * @author Dimitry Polivaev
 */
public class TagCompareCondition extends TagCondition {
    static final String NAME = "tag_compare_condition";
    static final String COMPARATION_RESULT = "COMPARATION_RESULT";
	static final String SUCCEED = "SUCCEED";
	static final String VALUE = "VALUE";

	static ASelectableCondition load(final XMLElement element) {
		return new TagCompareCondition(element.getAttribute(VALUE, null),
		        TreeXmlReader.xmlToBoolean(element.getAttribute(CompareConditionAdapter.MATCH_CASE, null)),
		        Integer.parseInt(element.getAttribute(COMPARATION_RESULT, null)),
		        TreeXmlReader.xmlToBoolean(element.getAttribute( SUCCEED, null)),
		        TreeXmlReader.xmlToBoolean(element.getAttribute(
                	    MATCH_APPROXIMATELY, null)),
		        Boolean.valueOf(element.getAttribute(IGNORE_DIACRITICS, null)),
		        Boolean.valueOf(element.getAttribute(SEARCH_ACROSS_ALL_CATEGORIES, null)));
	}

	private final int comparationResult;
	private final boolean succeed;
    private final String conditionContent;

	/**
	 */
	public TagCompareCondition(final String content, final boolean matchCase,
	                                 final int comparationResult, final boolean succeed, final boolean matchApproximately, boolean ignoreDiacritics,
	                                 boolean searchesAcrossAllCategories) {
		super(content, matchCase, matchApproximately, false, ignoreDiacritics, searchesAcrossAllCategories);
        this.conditionContent = content;
		this.comparationResult = comparationResult;
		this.succeed = succeed;

	}

	public boolean isEqualityCondition()
	{
		return comparationResult == 0;
	}

    @Override
    protected boolean checkText(String comparedContent) {
	    return succeed == (LineComparator.compareLinesParsingNumbers(comparedContent, conditionContent) == comparationResult);
    }

    @Override
	protected String createDescription() {
		String simpleCondition = CompareConditionAdapter.createComparisonDescription(comparationResult, succeed);
        return createDescription(TextUtils.getText(IconConditionController.FILTER_TAG), simpleCondition, conditionContent);
	}

    @Override
    protected List<Icon> createRenderedIcons(FontMetrics fontMetrics) {
        ConditionOperator operator = CompareConditionAdapter.createComparisonOperator(comparationResult, succeed);
        ConditionOperator displayedOperator = operator == ConditionOperator.FILTER_IS_EQUAL_TO ? ConditionOperator.EMPTY : operator;
        return createRenderedIcons("", displayedOperator, tagIcon(fontMetrics) , fontMetrics);
    }

	@Override
    public void fillXML(final XMLElement child) {
		super.fillXML(child);
        child.setAttribute(VALUE, conditionContent);
        child.setAttribute(COMPARATION_RESULT, Integer.toString(comparationResult));
		child.setAttribute(SUCCEED, TreeXmlWriter.BooleanToXml(succeed));
	}

	@Override
    protected String getName() {
	    return NAME;
    }

    @Override
    protected Object conditionValue() {
        return conditionContent;
    }
}
