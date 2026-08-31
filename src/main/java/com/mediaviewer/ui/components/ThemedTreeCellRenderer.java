package com.mediaviewer.ui.components;

import javax.swing.tree.DefaultTreeCellRenderer;

import com.mediaviewer.util.ThemeUtils;

public class ThemedTreeCellRenderer extends ThemedComponent {
    private final DefaultTreeCellRenderer render;
    private ThemeUtils.PanelType unselectedBackground;
    private ThemeUtils.PanelType selectedBackground;
    private ThemeUtils.TextType  unselectedText;
    private ThemeUtils.TextType  selectedText;

    public ThemedTreeCellRenderer(){
        super();
        render = new DefaultTreeCellRenderer();

        unselectedBackground = ThemeUtils.PanelType.PANEL;
        selectedBackground = ThemeUtils.PanelType.PANEL;
        unselectedText = ThemeUtils.TextType.PRIMARY;
        selectedText = ThemeUtils.TextType.PRIMARY;
    }

    @Override
    protected void applyTheme(){
        render.setBackgroundNonSelectionColor(currentTheme.getBG(unselectedBackground));
        render.setBackgroundSelectionColor(currentTheme.getBG(selectedBackground));
        render.setTextNonSelectionColor(currentTheme.getText(unselectedText));
        render.setTextSelectionColor(currentTheme.getText(selectedText));
        render.setBorderSelectionColor(currentTheme.getBorder());
    }

    public DefaultTreeCellRenderer getRender() { return render; }

    public void setBackgroundNonSelectionColor(ThemeUtils.PanelType type){
        this.unselectedBackground = type;
        render.setBackgroundNonSelectionColor(currentTheme.getBG(type));
    }

    public void setBackgroundSelectionColor(ThemeUtils.PanelType type){
        this.selectedBackground = type;
        render.setBackgroundSelectionColor(currentTheme.getBG(type));
    }

    public void setTextNonSelectionColor(ThemeUtils.TextType type){
        this.unselectedText = type;
        render.setTextNonSelectionColor(currentTheme.getText(type));
    }

    public void setTextSelectionColor(ThemeUtils.TextType type){
        this.selectedText = type;
        render.setTextSelectionColor(currentTheme.getText(type));
    }


}