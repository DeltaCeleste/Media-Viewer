package com.mediaviewer.ui.components;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JSplitPane;
import javax.swing.border.Border;

import com.mediaviewer.util.ThemeUtils;

public class ThemedSplitPanel extends ThemedComponent {
    private final JSplitPane split;
    private ThemeUtils.PanelType bg;
    private int type;
    private CollapseState state;

    public enum CollapseState {
        NORMAL,
        COLLAPSED_A,
        COLLAPSED_B;

        public String getName(){
            return this.name();
        }
    }

    public ThemedSplitPanel(int i, Container c1, Container c2){
        super();
        split = new JSplitPane(i, c1, c2);
        type = i;
        state = CollapseState.NORMAL;

        bg = ThemeUtils.PanelType.PANEL;

        applyTheme();
        setLayout(new BorderLayout());
        add(split, BorderLayout.CENTER);
    }

    public CollapseState getCollapseState() { 
        if( split.getDividerLocation() <= split.getMinimumDividerLocation() ){
            return CollapseState.COLLAPSED_A;
        } 
        else if( split.getDividerLocation() >= split.getMaximumDividerLocation() ){
            return CollapseState.COLLAPSED_B;
        } 
        else return CollapseState.NORMAL;
    }

    public void setCollapse(CollapseState newState){
        this.state = newState;
        switch (newState){
            case COLLAPSED_A:
                split.setDividerLocation(0);
                break;
            case COLLAPSED_B:
                if(type == JSplitPane.HORIZONTAL_SPLIT) {
                    split.setDividerLocation(this.getParent().getWidth());
                }
                else if (type == JSplitPane.VERTICAL_SPLIT) {
                    split.setDividerLocation(this.getParent().getHeight());
                } 
                break;
            case NORMAL:
                if(type == JSplitPane.HORIZONTAL_SPLIT) {
                    split.setDividerLocation(this.getParent().getWidth() - 300);
                }
                else if (type == JSplitPane.VERTICAL_SPLIT) {
                    split.setDividerLocation(this.getParent().getHeight() - 300);
                }      
                break;
        }
    }

    @Override
    protected void applyTheme(){
        split.setBackground(currentTheme.getBG(bg));
    }

    public void setDividerLocation(int i){
        split.setDividerLocation(i);
    }

    public void setDividerSize(int i){
        split.setDividerSize(i);
    }

    @Override
    public void setBorder(Border b){
        split.setBorder(b);
    }

    public void setBackground(ThemeUtils.PanelType type){
        bg = type;
        split.setBackground(currentTheme.getBG(type));
    }

    public void setResizeWeight(double d){
        split.setResizeWeight(d);
    }

    public void setOneTouchExpandable(boolean b){
        split.setOneTouchExpandable(b);
    }
}