package com.mediaviewer.ui.components;

import java.awt.BorderLayout;

import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.mediaviewer.util.ThemeUtils;

public class ThemedTree extends ThemedComponent {
    private final JTree tree;
    private ThemeUtils.PanelType bg;
    private ThemeUtils.TextType text;
    
    public ThemedTree(DefaultTreeModel model) {
        super();
        tree = new JTree(model);

        this.bg = ThemeUtils.PanelType.PANEL;
        this.text = ThemeUtils.TextType.PRIMARY;

        applyTheme();
        setLayout(new BorderLayout());
        add(tree, BorderLayout.CENTER);
    }

    public JTree getTree(){ return tree; }

    @Override
    protected void applyTheme(){
        tree.setBackground(currentTheme.getBG(bg));
        tree.setForeground(currentTheme.getText(text));
    }

    public void setBackground(ThemeUtils.PanelType type){
        this.bg = type;
        tree.setBackground(currentTheme.getBG(type));
    }

    public void setForeground(ThemeUtils.TextType type){
        this.text = type;
        tree.setForeground(currentTheme.getText(type));
    }

    public void setFont(ThemeUtils.FontType font, int style, ThemeUtils.FontSize size){
        tree.setFont(currentTheme.getFont(size, style, font));
    }

    public void setRowHeight(int i){
        tree.setRowHeight(i);
    }

    @Override
    public void setBorder(Border b){
        tree.setBorder(b);
    }

    public void setRootVisible(boolean b){
        tree.setRootVisible(b);
    }

    public void setShowsRootHandles(boolean b){
        tree.setShowsRootHandles(b);
    }

    public void setCellRenderer(DefaultTreeCellRenderer render){
        tree.setCellRenderer(render);
    }

    public TreePath getSelectionPath(){
        return tree.getSelectionPath();
    }

    public int getRowCount(){
        return tree.getRowCount();
    }
}