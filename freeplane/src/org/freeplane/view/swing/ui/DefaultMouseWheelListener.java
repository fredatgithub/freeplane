package org.freeplane.view.swing.ui;

import java.awt.event.InputEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Iterator;
import java.util.Set;

import org.freeplane.core.controller.Controller;
import org.freeplane.core.modecontroller.ModeController;
import org.freeplane.core.resources.IFreeplanePropertyListener;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.resources.ResourceControllerProperties;
import org.freeplane.core.ui.IMouseWheelEventHandler;
import org.freeplane.view.swing.map.MapView;

/**
 * @author foltin
 */
public class DefaultMouseWheelListener implements MouseWheelListener {
	private static final int HORIZONTAL_SCROLL_MASK = InputEvent.SHIFT_MASK | InputEvent.BUTTON1_MASK
	        | InputEvent.BUTTON2_MASK | InputEvent.BUTTON3_MASK;
	private static int SCROLL_SKIPS = 8;
	private static final int ZOOM_MASK = InputEvent.CTRL_MASK;
	final private Controller controller;

	/**
	 *
	 */
	public DefaultMouseWheelListener(final Controller controller) {
		super();
		this.controller = controller;
		ResourceController.getResourceController().addPropertyChangeListener(new IFreeplanePropertyListener() {
			public void propertyChanged(final String propertyName, final String newValue, final String oldValue) {
				if (propertyName.equals(ResourceControllerProperties.RESOURCES_WHEEL_VELOCITY)) {
					DefaultMouseWheelListener.SCROLL_SKIPS = Integer.parseInt(newValue);
				}
			}
		});
		DefaultMouseWheelListener.SCROLL_SKIPS = ResourceController.getResourceController().getIntProperty(
		    ResourceControllerProperties.RESOURCES_WHEEL_VELOCITY, 8);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * freeplane.modes.ModeController.MouseWheelEventHandler#handleMouseWheelEvent
	 * (java.awt.event.MouseWheelEvent)
	 */
	public void mouseWheelMoved(final MouseWheelEvent e) {
		final MapView mapView = (MapView) e.getSource();
		final ModeController mController = mapView.getModeController();
		if (mController.isBlocked()) {
			return;
		}
		final Set registeredMouseWheelEventHandler = mController.getUserInputListenerFactory()
		    .getMouseWheelEventHandlers();
		for (final Iterator i = registeredMouseWheelEventHandler.iterator(); i.hasNext();) {
			final IMouseWheelEventHandler handler = (IMouseWheelEventHandler) i.next();
			final boolean result = handler.handleMouseWheelEvent(e);
			if (result) {
				return;
			}
		}
		if ((e.getModifiers() & DefaultMouseWheelListener.ZOOM_MASK) != 0) {
			float newZoomFactor = 1f + Math.abs((float) e.getWheelRotation()) / 10f;
			if (e.getWheelRotation() < 0) {
				newZoomFactor = 1 / newZoomFactor;
			}
			final float oldZoom = ((MapView) e.getComponent()).getZoom();
			float newZoom = oldZoom / newZoomFactor;
			newZoom = (float) Math.rint(newZoom * 1000f) / 1000f;
			newZoom = Math.max(1f / 32f, newZoom);
			newZoom = Math.min(32f, newZoom);
			if (newZoom != oldZoom) {
				controller.getViewController().setZoom(newZoom);
			}
		}
		else if ((e.getModifiers() & DefaultMouseWheelListener.HORIZONTAL_SCROLL_MASK) != 0) {
			((MapView) e.getComponent()).scrollBy(DefaultMouseWheelListener.SCROLL_SKIPS * e.getWheelRotation(), 0);
		}
		else {
			((MapView) e.getComponent()).scrollBy(0, DefaultMouseWheelListener.SCROLL_SKIPS * e.getWheelRotation());
		}
	}
}
