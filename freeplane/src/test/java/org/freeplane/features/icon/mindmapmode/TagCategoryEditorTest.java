/*
 * Created on 6 Sept 2024
 *
 * author dimitry
 */
package org.freeplane.features.icon.mindmapmode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.freeplane.features.icon.TagAssertions.assertThatReferencedTags;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.icon.IconRegistry;
import org.freeplane.features.icon.Tag;
import org.freeplane.features.icon.TagAssertions;
import org.freeplane.features.icon.TagCategories;
import org.freeplane.features.icon.TagCategoriesTest;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class TagCategoryEditorTest {
    public interface TestSteps<T extends TestSteps<T>> {
        @SuppressWarnings("unchecked")
        default T me() {return (T) this;}
        default T given() {return me();}
        default T when() {return me();}
        default T then() {return me();}
        default T and() {return me();}
        default T that() {return me();}
     }

	class TagTestSteps implements TestSteps<TagTestSteps>, AutoCloseable {
        private final List<AutoCloseable> mocks;


        @Mock MIconController iconController;
        @Mock ResourceController resourceController;
        @Mock MapModel mapModel;
        @Mock IconRegistry iconRegistry;
        @Mock JDialog dialog;

        private NodeModel mapRootNode;

        private TagCategoryEditor uut;

        private TagCategories updatedTagCategories;

        TagTestSteps(){
            mocks = new ArrayList<>();
            mocks.add(MockitoAnnotations.openMocks(this));
            mapRootNode = new NodeModel(mapModel);
            Mockito.when(mapModel.getRootNode()).thenReturn(mapRootNode);
            Mockito.when(mapModel.getIconRegistry()).thenReturn(iconRegistry);
            MockedStatic<ResourceController> resourceControllerMock = mockStatic(ResourceController.class);
            mocks.add(resourceControllerMock);
            resourceControllerMock.when(ResourceController::getResourceController).thenReturn(resourceController);
            Mockito.when(resourceController.getIntProperty(any(), anyInt()))
                .then(x -> x.getArguments()[1]);
            MockedStatic<TextUtils> textUtilsMock = mockStatic(TextUtils.class);
            mocks.add(textUtilsMock);
            textUtilsMock.when(() -> TextUtils.getText( any(String.class)))
                .then(x -> x.getArguments()[0]);
            TagCategoryEditor.FORCE_HEADLESS_GRAPHICS_FOR_TEST = true;
        }

        @Override
        public void close() {
            try {
                for(AutoCloseable mock:mocks)
                    mock.close();
                TagCategoryEditor.FORCE_HEADLESS_GRAPHICS_FOR_TEST = false;
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
        }

        TagTestSteps tagCategoryEditor(TagCategories tagCategories) {
            Mockito.when(iconRegistry.getTagCategories()).thenReturn(tagCategories);
            Mockito.when(iconController.getTagFont(any())).thenReturn(new Font(Font.DIALOG, 0, 10));
            Mockito.when(dialog.getContentPane()).thenReturn(new JPanel());
            this.uut = new TagCategoryEditor(dialog, iconController, mapModel);
            this.updatedTagCategories = uut.getTagCategories();
            return me();
        }

        TagTestSteps selectNode(int ... indices) {
            TreeNode selectedNode = updatedTagCategories.getRootNode();
            for(int i : indices) {
                selectedNode = selectedNode.getChildAt(i);
            }
            JTree tree = uut.getTree();
            DefaultTreeModel nodes = updatedTagCategories.getNodes();
            TreeNode[] pathToRoot = nodes.getPathToRoot(selectedNode);
            tree.setSelectionPath(new TreePath(pathToRoot));
            return me();
        }

        TagTestSteps renameSelectedNode(String content) {
            DefaultTreeModel nodes = updatedTagCategories.getNodes();
            JTree tree = uut.getTree();
            TreePath selectionPath = tree.getSelectionPath();
            nodes.valueForPathChanged(selectionPath,
                    new Tag(content, updatedTagCategories.tagWithoutCategories((DefaultMutableTreeNode) selectionPath.getLastPathComponent()).getColor()));
            return me();
        }

        TagTestSteps submit() {
            uut.submit();
            verify(iconController).setTagCategories(mapModel, updatedTagCategories);
            updatedTagCategories.updateTagReferences();
            return me();
        }

        ObjectAssert<TagCategories> assertThatUpdatedTagCategories() {
            return Assertions.assertThat(updatedTagCategories);
        }

        TagTestSteps cut() {
            uut.cutNodes();
            return me();
        }

        TagTestSteps paste() {
            uut.pasteNodes();
            return me();
        }

        TagTestSteps setColor(Color color) {
            uut.setTagColor(color);
            return me();
        }


        public TagTestSteps addChild(String content, Color color) {
            return addNode(content, color, true);
        }

        private TagTestSteps addNode(String content, Color color, boolean asChild) {
            uut.addNode(asChild);
            updatedTagCategories.getNodes()
                .valueForPathChanged(uut.getTree().getSelectionPath(),
                    new Tag(content, color));
            return me();
        }

        public TagTestSteps addSibling(String content, Color color) {
            return addNode(content, color, false);
        }
    }

    @Test
    public void renameCategory() {
        try (TagTestSteps steps = new TagTestSteps()){
            TagCategories tagCategories = TagCategoriesTest.tagCategories("AA#11223344\n"
                    + " BB#22334455\n"
                    + "  CC#33445566\n"
                    + "DD#44556677\n");
            tagCategories.registerTag("UU");
            tagCategories.registerTag("VV");
            steps.given().tagCategoryEditor(tagCategories)
            .when().selectNode(0, 0)
            .renameSelectedNode("tag22")
            .and().submit()
            .then().assertThatUpdatedTagCategories()
            .satisfies(tc -> {
                TagAssertions.assertThatSerialized(tc).isEqualTo("AA#11223344\n"
                        + " tag22#22334455\n"
                        + "  CC#33445566\n"
                        + "DD#44556677\n");
                assertThat(tc.getTagsAsListModel().stream().map(Tag::getContent)).
                containsExactly("AA", "AA::tag22", "AA::tag22::CC", "DD",
                        "UU", "VV");
            });
        }
    }
    @Test
    public void renameCategoryAndSubCategory() {
        try (TagTestSteps steps = new TagTestSteps()){
            TagCategories tagCategories = TagCategoriesTest.tagCategories("AA#11223344\n"
                    + " BB#22334455\n"
                    + "  CC#33445566\n"
                    + "DD#44556677\n");
            steps.given().tagCategoryEditor(tagCategories)
            .when()
            .selectNode(0)
            .renameSelectedNode("EE")
            .selectNode(0, 0)
            .renameSelectedNode("FF")
            .and().submit()
            .then().assertThatUpdatedTagCategories()
            .satisfies(tc -> {
                TagAssertions.assertThatSerialized(tc).isEqualTo("EE#11223344\n"
                        + " FF#22334455\n"
                        + "  CC#33445566\n"
                        + "DD#44556677\n");
                assertThat(tc.getTagsAsListModel().stream().map(Tag::getContent)).
                containsExactly("DD", "EE", "EE::FF", "EE::FF::CC");
            });
        }
    }

    @Test
    public void renameUncategorizedTag() {
        try (TagTestSteps steps = new TagTestSteps()){
            TagCategories tagCategories = TagCategoriesTest.tagCategories("AA#11223344\n"
                    + " BB#22334455\n"
                    + "  CC#33445566\n"
                    + "DD#44556677\n");
            tagCategories.registerTag("UU");
            tagCategories.registerTag("VV");
            steps.given().tagCategoryEditor(tagCategories)
            .when().selectNode(2, 0)
            .renameSelectedNode("uncategorized11")
            .and().submit()
            .then().assertThatUpdatedTagCategories()
            .satisfies(tc -> {
                TagAssertions.assertThatSerialized(tc).isEqualTo("AA#11223344\n"
                        + " BB#22334455\n"
                        + "  CC#33445566\n"
                        + "DD#44556677\n");
                assertThat(tc.getTagsAsListModel().stream().map(Tag::getContent)).
                containsExactly("AA", "AA::BB", "AA::BB::CC", "DD",
                        "uncategorized11", "VV");
            });
        }
    }


    @Test
    public void renameAndMergeUncategorizedTag() {
        try (TagTestSteps steps = new TagTestSteps()){
            TagCategories tagCategories = TagCategoriesTest.tagCategories("");
            tagCategories.setTagColor("tag1", Color.BLACK);
            tagCategories.setTagColor("tag2", Color.WHITE);
            steps.given().tagCategoryEditor(tagCategories)
            .when().selectNode(0, 0)
            .renameSelectedNode("tag2")
            .and().submit()
            .then().assertThatUpdatedTagCategories()
            .satisfies(tc -> {
                TagAssertions.assertThatSerialized(tc).isEmpty();
                assertThat(tc.getTagsAsListModel()).map(Tag::getContent)
                .containsExactlyInAnyOrder("tag2");
            });
        }
    }

    @Test
    public void renameAndMergeCategorizedLeafTag() {
        try (TagTestSteps steps = new TagTestSteps()){
            TagCategories tagCategories = TagCategoriesTest.tagCategories("");
            tagCategories.setTagColor("cat::tag1", Color.BLACK);
            tagCategories.setTagColor("cat::tag2", Color.WHITE);
            steps.given().tagCategoryEditor(tagCategories)
            .when().selectNode(0, 0)
            .renameSelectedNode("tag2")
            .and().submit()
            .then().assertThatUpdatedTagCategories()
            .satisfies(tc -> {
                TagAssertions.assertThatSerialized(tc).isEqualTo("cat#2072ffff\n"
                        + " tag2#ffffffff\n");
                assertThatReferencedTags(tc).map(Tag::getContent)
                .containsExactlyInAnyOrder("cat", "cat::tag2");
            });
        }
    }


    @Test
    public void renameAndMergeCategory() {
        try (TagTestSteps steps = new TagTestSteps()){
            TagCategories tagCategories = TagCategoriesTest.tagCategories("");
            tagCategories.setTagColor("aaa::tag", Color.BLACK);
            tagCategories.setTagColor("bbb::tag", Color.WHITE);
            tagCategories.setTagColor("aaa", Color.BLUE);
            tagCategories.setTagColor("bbb", Color.GREEN);
            steps.given().tagCategoryEditor(tagCategories)
            .when().selectNode(0)
            .renameSelectedNode("bbb")
            .and().submit()
            .then().assertThatUpdatedTagCategories()
            .satisfies(tc -> {
                TagAssertions.assertThatSerialized(tc).isEqualTo("bbb#00ff00ff\n"
                        + " tag#ffffffff\n");
                assertThatReferencedTags(tc).map(Tag::getContent)
                .containsExactlyInAnyOrder("bbb", "bbb::tag");
            });
        }
    }

    @Test
    public void moveTagsToUncategorized() {
        try (TagTestSteps steps = new TagTestSteps()){
            TagCategories tagCategories = TagCategoriesTest.tagCategories("AA#11223344\n"
                    + " BB#22334455\n"
                    + "  CC#33445566\n"
                    + "DD#44556677\n");
            tagCategories.registerTag("UU");
            tagCategories.registerTag("VV");
            steps.given().tagCategoryEditor(tagCategories)
            .when().selectNode(0, 0)
            .cut()
            .selectNode(2)
            .paste()
            .and().submit()
            .then().assertThatUpdatedTagCategories()
            .satisfies(tc -> {
                TagAssertions.assertThatSerialized(tc).isEqualTo("AA#11223344\n"
                        + "DD#44556677\n");
                assertThat(tc.getTagsAsListModel().stream().map(Tag::getContent)).
                containsExactly("AA", "BB", "CC", "DD",
                        "UU", "VV");
            });
        }
    }

    @Test
    public void setCategoryColor() {
        try (TagTestSteps steps = new TagTestSteps()){
            TagCategories tagCategories = TagCategoriesTest.tagCategories("AA#11223344\n"
                    + " BB#22334455\n"
                    + "  CC#33445566\n"
                    + "DD#44556677\n");
            tagCategories.registerTag("UU");
            tagCategories.registerTag("VV");
            steps.given().tagCategoryEditor(tagCategories)
            .when().selectNode(0, 0)
            .setColor(Color.BLACK)
            .and().submit()
            .then().assertThatUpdatedTagCategories()
            .satisfies(tc -> {
                TagAssertions.assertThatSerialized(tc).isEqualTo("AA#11223344\n"
                        + " BB#000000ff\n"
                        + "  CC#33445566\n"
                        + "DD#44556677\n");
                assertThat(tc.getTagsAsListModel().stream().map(Tag::getContent)).
                containsExactly("AA", "AA::BB", "AA::BB::CC", "DD",
                        "UU", "VV");
                assertThat(tc.getTag(new Tag("AA::BB")).get().getColor())
                .isEqualTo(Color.BLACK);
            });
        }
    }

    @Test
    public void createsNoUncategorizedTags_creatingUncategorizedNodeChild() {
        try (TagTestSteps steps = new TagTestSteps()){
            TagCategories tagCategories = TagCategoriesTest.tagCategories("");
            steps.given().tagCategoryEditor(tagCategories)
            .when().selectNode(0)
            .addChild("tag", Color.WHITE)
            .and().submit()
            .then().assertThatUpdatedTagCategories()
            .satisfies(tc -> {
                TagAssertions.assertThatSerialized(tc).isEqualTo("tag#ffffffff\n");
                TagCategories copy = tc.copy();
                copy.getTagsAsListModel()
                .forEach(tag -> copy.registerTagReferenceIfUnknown(tag));
            });
        }
    }

    @Test
    public void createsNoUncategorizedTags_creatingUncategorizedNodeSibling() {
        try (TagTestSteps steps = new TagTestSteps()){
            TagCategories tagCategories = TagCategoriesTest.tagCategories("");
            steps.given().tagCategoryEditor(tagCategories)
            .when().selectNode(0)
            .addSibling("tag", Color.WHITE)
            .and().submit()
            .then().assertThatUpdatedTagCategories()
            .satisfies(tc -> {
                TagAssertions.assertThatSerialized(tc).isEqualTo("tag#ffffffff\n");
                TagCategories copy = tc.copy();
                copy.getTagsAsListModel()
                .forEach(tag -> copy.registerTagReferenceIfUnknown(tag));
            });
        }
    }


    @Test
    public void createsNoUncategorizedTags_creatingUncategorizedNodeChildsSibling() {
        try (TagTestSteps steps = new TagTestSteps()){
            TagCategories tagCategories = TagCategoriesTest.tagCategories("");
            tagCategories.registerTag("UU");
            steps.given().tagCategoryEditor(tagCategories)
            .when().selectNode(0, 0)
            .addSibling("tag", Color.WHITE)
            .and().submit()
            .then().assertThatUpdatedTagCategories()
            .satisfies(tc -> {
                TagAssertions.assertThatSerialized(tc).isEqualTo("tag#ffffffff\n");
                TagCategories copy = tc.copy();
                copy.getTagsAsListModel()
                .forEach(tag -> copy.registerTagReferenceIfUnknown(tag));
            });
        }
    }


    @Test
    public void createsNoUncategorizedTags_creatingUncategorizedNodeChildsChild() {
        try (TagTestSteps steps = new TagTestSteps()){
            TagCategories tagCategories = TagCategoriesTest.tagCategories("");
            tagCategories.registerTag("UU");
            steps.given().tagCategoryEditor(tagCategories)
            .when().selectNode(0, 0)
            .addChild("tag", Color.WHITE)
            .and().submit()
            .then().assertThatUpdatedTagCategories()
            .satisfies(tc -> {
                TagAssertions.assertThatSerialized(tc).isEqualTo("tag#ffffffff\n");
                TagCategories copy = tc.copy();
                copy.getTagsAsListModel()
                .forEach(tag -> copy.registerTagReferenceIfUnknown(tag));
            });
        }
    }
}
