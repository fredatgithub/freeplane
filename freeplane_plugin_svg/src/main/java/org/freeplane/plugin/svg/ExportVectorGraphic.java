/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file author is Christian Foltin
 *  It is modified by Dimitry Polivaev in 2008.
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
package org.freeplane.plugin.svg;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.text.AttributedCharacterIterator;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGeneratorContext.GraphicContextDefaults;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.util.SVGConstants;
import org.freeplane.core.awt.GraphicsHints;
import org.freeplane.features.export.mindmapmode.IExportEngine;
import org.freeplane.view.swing.map.MapView;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * @author foltin
 */
abstract class ExportVectorGraphic implements IExportEngine {

	/**
	 */
	protected SVGGraphics2D createSVGGraphics2D(final MapView view) {
	    final DOMImplementation impl = GenericDOMImplementation.getDOMImplementation();
	    final String namespaceURI = SVGConstants.SVG_NAMESPACE_URI;
	    final Document domFactory = impl.createDocument(namespaceURI, "svg", null);
	    final SVGGeneratorContext ctx = createGeneratorContext(domFactory);
	    final GraphicContextDefaults defaults = new GraphicContextDefaults();
	    defaults.setFont(new Font("Arial", Font.PLAIN, 12));
	    ctx.setGraphicContextDefaults(defaults);
	    ctx.setExtensionHandler(new GradientExtensionHandler());
	    ctx.setPrecision(12);
	    final SVGGraphics2D g2d = new SVGGraphics2D(ctx, false) {

            @Override
            public void drawString(String s, float x, float y) {
                if(getRenderingHint(GraphicsHints.FORCE_TEXT_TO_SHAPE) == Boolean.TRUE) {
                    boolean oldTextAsShapes = textAsShapes;
                    textAsShapes = true;
                    super.drawString(s, x, y);
                    textAsShapes = oldTextAsShapes;
                }
                else
                    super.drawString(s, x, y);
            }

            @Override
            public void drawString(AttributedCharacterIterator ati, float x, float y) {
                if(getRenderingHint(GraphicsHints.FORCE_TEXT_TO_SHAPE) == Boolean.TRUE) {
                    boolean oldTextAsShapes = textAsShapes;
                    textAsShapes = true;
                    super.drawString(ati, x, y);
                    textAsShapes = oldTextAsShapes;
                }
                else
                    super.drawString(ati, x, y);
            }
	    };

	    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
	    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
	    view.preparePrinting();
	    final Rectangle innerBounds = view.getInnerBounds();
	    g2d.setSVGCanvasSize(new Dimension(innerBounds.width, innerBounds.height));
	    g2d.translate(-innerBounds.x, -innerBounds.y);
	    view.print(g2d);
	    view.endPrinting();
	    return g2d;
	}

	protected SVGGeneratorContext createGeneratorContext(final Document domFactory) {
		final SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(domFactory);
		return ctx;
	}
}
