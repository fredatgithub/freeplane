/*
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
package org.freeplane.features.icon.mindmapmode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.StringWriter;
import java.util.EventObject;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DropMode;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.resources.components.JColorButton;
import org.freeplane.core.ui.ColorTracker;
import org.freeplane.core.ui.LabelAndMnemonicSetter;
import org.freeplane.core.ui.components.JRestrictedSizeScrollPane;
import org.freeplane.core.ui.components.TagIcon;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.icon.IconRegistry;
import org.freeplane.features.icon.Tag;

class TagCategoryEditor {

    static class TagCellRenderer extends DefaultTreeCellRenderer {
        private Object rootNode; // Reference to the root node object

        public TagCellRenderer(DefaultMutableTreeNode rootNode) {
            this.rootNode = rootNode;
            setHorizontalAlignment(CENTER);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                boolean expanded, boolean leaf, int row, boolean hasFocus) {

            super.getTreeCellRendererComponent(tree, null, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode) {
                Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                if (userObject instanceof Tag) {
                    Tag tag = (Tag) userObject;

                    if (leaf && value != rootNode) {
                        setText(null);
                        setIcon(new TagIcon(tag, getFont())); // Example of
                                                              // setting
                                                              // a custom icon
                    } else {
                        setText(tag.getContent());
                    }
                } else if (userObject != null) {
                    setText(userObject.toString());
                }
            }

            return this;
        }
    }

    static class TagCellEditor extends AbstractCellEditor implements TreeCellEditor {

        private JTextField textField;

        private DefaultMutableTreeNode currentNode;

        private IconRegistry registry;

        public TagCellEditor(IconRegistry registry) {
            this.registry = registry;
            textField = new JTextField();
            textField.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    stopCellEditing();
                }
            });

            textField.addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent e) {
                    stopCellEditing();
                }
            });
        }

        @Override
        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected,
                boolean expanded, boolean leaf, int row) {
            currentNode = (DefaultMutableTreeNode) value;
            Tag tag = (Tag) currentNode.getUserObject();
            textField.setText(tag.getContent());
            return textField;
        }

        @Override
        public boolean isCellEditable(EventObject event) {
            if (event instanceof MouseEvent) {
                return ((MouseEvent) event).getClickCount() >= 2;
            }
            return true;
        }

        @Override
        public Object getCellEditorValue() {
            return registry.createTag(textField.getText());
        }

        @Override
        public boolean stopCellEditing() {
            fireEditingStopped();
            return true;
        }

        @Override
        public void cancelCellEditing() {
            fireEditingCanceled();
        }
    }

    @SuppressWarnings("serial")
    static class TreeTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            JTree tree = (JTree) c;
            TreePath path = tree.getSelectionPath();
            if (path != null) {
                return new StringSelection(path.toString());
            }
            return null;
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            if (!support.isDrop()) {
                return false;
            }
            support.setShowDropLocation(true);
            return support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
            int childIndex = dl.getChildIndex();
            TreePath dest = dl.getPath();
            JTree tree = (JTree) support.getComponent();
            DefaultTreeModel tagCategories = (DefaultTreeModel) tree.getModel();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();
            JTree treeSource = (JTree) support.getComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeSource.getSelectionPath()
                    .getLastPathComponent();
            try {
                DefaultMutableTreeNode newNode = (DefaultMutableTreeNode) node.clone();
                if (childIndex == -1) {
                    childIndex = parent.getChildCount();
                }
                tagCategories.insertNodeInto(newNode, parent, childIndex);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    private static final String WIDTH_PROPERTY = "tagDialog.width";

    private static final String HEIGHT_PROPERTY = "tagDialog.height";

    private final String title;

    private final JDialog dialog;

    private final JColorButton colorButton;

    private final JTree tree;

    private final TagCategories tagCategories;

    private boolean contentWasModified;

    TagCategoryEditor(RootPaneContainer frame, TagCategories tagCategories) {
        title = TextUtils.getText("edit_tag_categories");
        contentWasModified = false;
        this.dialog = frame instanceof Frame ? new JDialog((Frame) frame, title, /*
                                                                                  * modal=
                                                                                  */true)
                : new JDialog((JDialog) frame, title, /* modal= */true);
        final JButton okButton = new JButton();
        final JButton cancelButton = new JButton();
        colorButton = new JColorButton();
        colorButton.setColor(Tag.EMPTY_TAG.getIconColor());
        final JCheckBox enterConfirms = new JCheckBox("", ResourceController.getResourceController()
                .getBooleanProperty("el__enter_confirms_by_default"));
        LabelAndMnemonicSetter.setLabelAndMnemonic(okButton, TextUtils.getRawText("ok"));
        LabelAndMnemonicSetter.setLabelAndMnemonic(cancelButton, TextUtils.getRawText("cancel"));
        LabelAndMnemonicSetter.setLabelAndMnemonic(enterConfirms, TextUtils.getRawText(
                "enter_confirms"));
        colorButton.setEnabled(false);
        okButton.addActionListener(e -> {
            dialog.setVisible(false);
            submit();
        });
        cancelButton.addActionListener(e -> dialog.setVisible(false));

        colorButton.addActionListener(e -> modifyTagColor());
        final JPanel buttonPane = new JPanel();
        buttonPane.add(enterConfirms);
        buttonPane.add(okButton);
        buttonPane.add(cancelButton);
        buttonPane.add(colorButton);
        buttonPane.setMaximumSize(new Dimension(1000, 20));
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        final Container contentPane = dialog.getContentPane();

        this.tagCategories = tagCategories;
        tree = new JTree(tagCategories.nodes) {

            @Override
            public boolean isPathEditable(TreePath path) {
                Object lastPathComponent = path.getLastPathComponent();
                if (!(lastPathComponent instanceof DefaultMutableTreeNode))
                    return false;
                Object userObject = ((DefaultMutableTreeNode) lastPathComponent).getUserObject();
                return userObject instanceof Tag;
            }
        };
        tree.setEditable(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setInvokesStopCellEditing(true);
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setTransferHandler(new TreeTransferHandler());
        tree.setCellRenderer(new TagCellRenderer(tagCategories.getRootNode()));
        tree.setCellEditor(new TagCellEditor(tagCategories.getRegistry()));

        JScrollPane scrollPane = new JScrollPane(tree);
        configureKeyBindings();

        JRestrictedSizeScrollPane editorScrollPane = createScrollPane();
        editorScrollPane.setViewportView(tree);
        editorScrollPane.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                tree.revalidate();
                tree.repaint();
            }

        });
        enterConfirms.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                tree.requestFocus();
                ResourceController.getResourceController().setProperty(
                        "el__enter_confirms_by_default", Boolean.toString(enterConfirms
                                .isSelected()));
            }
        });
        tree.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(final KeyEvent e) {
                switch (e.getKeyCode()) {
                case KeyEvent.VK_ESCAPE:
                    e.consume();
                    dialog.setVisible(false);
                    break;
                case KeyEvent.VK_ENTER:
                    e.consume();
                    if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0
                            || enterConfirms.isSelected() == ((e.getModifiers() & InputEvent.ALT_DOWN_MASK) != 0)) {
                        addNode((e.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK)) != 0);
                        break;
                    }
                    dialog.setVisible(false);
                    submit();
                    break;
                }
            }

            @Override
            public void keyReleased(final KeyEvent e) {
            }

            @Override
            public void keyTyped(final KeyEvent e) {
            }
        });

        tree.getSelectionModel().addTreeSelectionListener(this::updateColorButton);
        tagCategories.addTreeModelListener(new TreeModelListener() {

            @Override
            public void treeStructureChanged(TreeModelEvent e) {
                contentWasModified = true;
                updateColorButton();
            }

            @Override
            public void treeNodesRemoved(TreeModelEvent e) {
                contentWasModified = true;
                updateColorButton();
            }

            @Override
            public void treeNodesInserted(TreeModelEvent e) {
                contentWasModified = true;
                tree.addSelectionPath(e.getTreePath());
            }

            @Override
            public void treeNodesChanged(TreeModelEvent e) {
                contentWasModified = true;
                updateColorButton();
            }
        });

        contentPane.add(editorScrollPane, BorderLayout.CENTER);
        final boolean areButtonsAtTheTop = ResourceController.getResourceController()
                .getBooleanProperty("el__buttons_above");
        contentPane.add(buttonPane, areButtonsAtTheTop ? BorderLayout.NORTH : BorderLayout.SOUTH);
        configureDialog(dialog);
        restoreDialogSize(dialog);
        dialog.pack();
        dialog.addComponentListener(new ComponentListener() {
            @Override
            public void componentShown(final ComponentEvent e) {
            }

            @Override
            public void componentResized(final ComponentEvent e) {
                saveDialogSize(dialog);
            }

            @Override
            public void componentMoved(final ComponentEvent e) {
            }

            @Override
            public void componentHidden(final ComponentEvent e) {
                dialog.dispose();
            }
        });

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                if (dialog.isVisible()) {
                    confirmedSubmit();
                }
            }
        });
    }

    private void configureKeyBindings() {
        InputMap im = tree.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = tree.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "startEditing");
        Action editNodeAction = am.get("startEditing");

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "removeNode");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removeNode");

        @SuppressWarnings("serial")
        AbstractAction addChildNodeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNode(true);
            }
        };

        @SuppressWarnings("serial")
        AbstractAction addSiblingNodeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNode(false);
            }
        };

        @SuppressWarnings("serial")
        AbstractAction removeNodeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeNode();
            }
        };
        am.put("removeNode", removeNodeAction);

        @SuppressWarnings("serial")
        AbstractAction copyNodeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyNode();
            }
        };
        am.put(TransferHandler.getCopyAction().getValue(Action.NAME), copyNodeAction);

        @SuppressWarnings("serial")
        AbstractAction cutNodeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyNode();
                removeNode();
            }
        };
        am.put(TransferHandler.getCutAction().getValue(Action.NAME), cutNodeAction);

        @SuppressWarnings("serial")
        AbstractAction pasteNodeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pasteNode();
            }
        };
        am.put(TransferHandler.getPasteAction().getValue(Action.NAME), pasteNodeAction);
        JMenuBar menubar = new JMenuBar();
        JMenu editMenu = TranslatedElementFactory.createMenu("edit");

        JMenuItem addChildMenuItem = TranslatedElementFactory.createMenuItem("menu_addChild");
        addChildMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Event.SHIFT_MASK | Event.ALT_MASK));
        addChildMenuItem.addActionListener(addChildNodeAction);
        editMenu.add(addChildMenuItem);

        JMenuItem addSiblingMenuItem = TranslatedElementFactory.createMenuItem("menu_addSibling");
        addSiblingMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Event.SHIFT_MASK));
        addSiblingMenuItem.addActionListener(addSiblingNodeAction);
        editMenu.add(addSiblingMenuItem);

        JMenuItem removeMenuItem = TranslatedElementFactory.createMenuItem("menu_remove");
        removeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
        removeMenuItem.addActionListener(removeNodeAction);
        editMenu.add(removeMenuItem);

        JMenuItem editMenuItem = TranslatedElementFactory.createMenuItem("menu_edit");
        editMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
        editMenuItem.addActionListener(editNodeAction);
        editMenu.add(editMenuItem);

        JMenuItem copyMenuItem = TranslatedElementFactory.createMenuItem("menu_copy");
        copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        copyMenuItem.addActionListener(copyNodeAction);
        editMenu.add(copyMenuItem);

        JMenuItem cutMenuItem = TranslatedElementFactory.createMenuItem("CutAction.text");
        cutMenuItem.addActionListener(cutNodeAction);
        cutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        editMenu.add(cutMenuItem);

        JMenuItem pasteMenuItem = TranslatedElementFactory.createMenuItem("PasteAction.text");
        pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        pasteMenuItem.addActionListener(pasteNodeAction);
        editMenu.add(pasteMenuItem);

        menubar.add(editMenu);
        dialog.setJMenuBar(menubar);
    }

    private void addNode(boolean asChild) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree
                .getLastSelectedPathComponent();
        TreeNode[] nodes = (asChild || selectedNode == null || selectedNode.isRoot()) ? tagCategories.addChildNode(selectedNode) : tagCategories.addSiblingNode(selectedNode);
        if(nodes.length == 0)
            return;
        TreePath path = new TreePath(nodes);
        tree.scrollPathToVisible(path);
        tree.setSelectionPath(path);
        tree.startEditingAtPath(path);
    }

    private void removeNode() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree
                .getLastSelectedPathComponent();
        if (selectedNode != null && selectedNode.getParent() != null) {
            tagCategories.removeNodeFromParent(selectedNode);
        }
    }

    private void copyNode() {
        TreePath currentPath = tree.getSelectionPath();
        if (currentPath != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) currentPath
                    .getLastPathComponent();
            try {
                StringWriter writer = new StringWriter();
                TagCategories.writeTagCategories(node, "", true, writer);
                String serializedData = writer.toString();
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection stringSelection = new StringSelection(serializedData);
                clipboard.setContents(stringSelection, null);
            } catch (IOException e) {
                /**/}
        }
    }

    private void pasteNode() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            Transferable contents = clipboard.getContents(null);
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String data = (String) contents.getTransferData(DataFlavor.stringFlavor);
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree
                        .getLastSelectedPathComponent();
                tagCategories.add(selectedNode, data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void modifyTagColor() {
        Tag tag = getSelectedTag();
        if (tag != null && !tag.isEmpty()) {
            Color defaultColor = new Color(tag.getDefaultColor().getRGB(), true);
            Color initialColor = tag.getIconColor();
            final Color result = ColorTracker.showCommonJColorChooserDialog(tree, tag.getContent(),
                    initialColor, defaultColor);
            if (result != null && !initialColor.equals(result) || result == defaultColor) {
                Optional<Color> newColor = result == defaultColor ? Optional.empty()
                        : Optional.of(result);
                tag.setColor(newColor);
                tagCategories.tagChanged(tag);
                updateColorButton();
            }
        }
    }

    private Tag getSelectedTag() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree
                .getLastSelectedPathComponent();
        if (selectedNode == null || !selectedNode.isLeaf())
            return null;

        Object userObject = selectedNode.getUserObject();
        if (!(userObject instanceof Tag))
            return null;

        return (Tag) userObject;
    }

    void show() {
        UITools.setDialogLocationRelativeTo(dialog, dialog.getParent());
        dialog.setVisible(true);
    }

    protected void submit() {
        tagCategories.save();
    }

    private JRestrictedSizeScrollPane createScrollPane() {
        final JRestrictedSizeScrollPane scrollPane = new JRestrictedSizeScrollPane();
        UITools.setScrollbarIncrement(scrollPane);
        scrollPane.setMinimumSize(new Dimension(0, 60));
        return scrollPane;
    }

    private void configureDialog(JDialog dialog) {
        dialog.setModal(false);
    }

    private void saveDialogSize(final JDialog dialog) {
        ResourceController resourceController = ResourceController.getResourceController();
        resourceController.setProperty(WIDTH_PROPERTY, dialog.getWidth());
        resourceController.setProperty(HEIGHT_PROPERTY, dialog.getHeight());
    }

    private void restoreDialogSize(final JDialog dialog) {
        Dimension preferredSize = dialog.getPreferredSize();
        ResourceController resourceController = ResourceController.getResourceController();
        preferredSize.width = Math.max(preferredSize.width, resourceController.getIntProperty(
                WIDTH_PROPERTY, 0));
        preferredSize.height = Math.max(preferredSize.height, resourceController.getIntProperty(
                HEIGHT_PROPERTY, 0));
        dialog.setPreferredSize(preferredSize);
    }

    private void confirmedSubmit() {
        if (dialog.isVisible()) {
            if (contentWasModified) {
                final int action = JOptionPane.showConfirmDialog(dialog, TextUtils.getText(
                        "long_node_changed_submit"), "", JOptionPane.YES_NO_CANCEL_OPTION);

                if (action == JOptionPane.YES_OPTION) {
                    submit();
                } else if (action == JOptionPane.CANCEL_OPTION) {
                    return;
                }
            }
            dialog.setVisible(false);
        }
    }

    private void updateColorButton(TreeSelectionEvent e) {
        updateColorButton();
    }

    private void updateColorButton() {
        Tag tag = getSelectedTag();
        if (tag == null || tag.isEmpty()) {
            colorButton.setEnabled(false);
            colorButton.setColor(Tag.EMPTY_TAG.getIconColor());
            return;
        }
        colorButton.setEnabled(true);
        colorButton.setColor(tag.getIconColor());
    }
}
