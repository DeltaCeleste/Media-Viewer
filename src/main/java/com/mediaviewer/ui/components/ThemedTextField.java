package com.mediaviewer.ui.components;

import com.mediaviewer.util.Theme;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.border.*;
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
            changeBorderColorByType();
        }
    }

    public Document getDocument(){
        this.field.getDocument();
    }

    public String getText(){
        return field.getText();
    }

    private static Border changeBorderColorByType() {
        if (border == null) return null;
        
        // Detectar tipo y crear nuevo borde con el color cambiado
        if (border instanceof LineBorder) {
            LineBorder lb = (LineBorder) border;
            return new LineBorder(currentTheme.getBorder(), lb.getThickness(), lb.getRoundedCorners());
            
        } else if (border instanceof MatteBorder) {
            MatteBorder mb = (MatteBorder) border;
            return new MatteBorder(mb.getBorderInsets(), currentTheme.getBorder());
            
        } else if (border instanceof EtchedBorder) {
            EtchedBorder eb = (EtchedBorder) border;
            return new EtchedBorder(eb.getEtchType(), currentTheme.getBorder(), currentTheme.getBorder().darker());
            
        } else if (border instanceof BevelBorder) {
            BevelBorder bb = (BevelBorder) border;
            // BevelBorder tiene colores internos complejos
            // Podemos recrearlo con los colores del nuevo color
            return new BevelBorder(bb.getBevelType(), currentTheme.getBorder(), currentTheme.getBorder().darker());
            
        } else if (border instanceof TitledBorder) {
            TitledBorder tb = (TitledBorder) border;
            // Cambiar color del título y del borde interno
            Border innerBorder = changeBorderColorByType(tb.getBorder(), currentTheme.getBorder());
            return BorderFactory.createTitledBorder(
                innerBorder,
                tb.getTitle(),
                tb.getTitleJustification(),
                tb.getTitlePosition(),
                tb.getTitleFont(),
                currentTheme.getBorder() // Cambiar color del título
            );
            
        } else if (border instanceof CompoundBorder) {
            CompoundBorder cb = (CompoundBorder) border;
            // Cambiar color recursivamente en ambos bordes
            Border newOutside = changeBorderColorByType(cb.getOutsideBorder(), currentTheme.getBorder());
            Border newInside = changeBorderColorByType(cb.getInsideBorder(), currentTheme.getBorder());
            return new CompoundBorder(newOutside, newInside);
            
        } else {
            // Si no sabemos el tipo, lo envolvemos con un borde de color
            return new ColorOverlayBorder(border, currentTheme.getBorder());
        }
    }
}