package com.mediaviewer.ui.components;

import com.mediaviewer.util.Theme;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class ThemedTextField extends ThemedComponent {
    private JTextField field;
    private Border border = null;

    
    public ThemedTextField(int columns, String text, Border border) {
        super();
        this.border = border;
        field = new JTextField(columns);

        field.setToolTipText(text);

        field.setFont(currentTheme.getFontSmall());
        field.setBorder(border);

        applyTheme();
        setLayout(new BorderLayout());
        add(label, BorderLayout.CENTER);
    }
    
    @Override
    protected void applyTheme() {
        field.setBackground(currentTheme.getInput());
        field.setForeground(currentTheme.getText1());
        field.setCaretColor(currentTheme.getText1());

        if(border != null){
            if (original instanceof CompoundBorder) {
                CompoundBorder cb = (CompoundBorder) border;
                // Cambiar color recursivamente en ambos bordes
                Border newOutside = changeBorderColorByType(cb.getOutsideBorder(), currentTheme.getBorder());
                Border newInside = changeBorderColorByType(cb.getInsideBorder(), currentTheme.getBorder());
                border = new CompoundBorder(newOutside, newInside);
            }
        }
    }
}