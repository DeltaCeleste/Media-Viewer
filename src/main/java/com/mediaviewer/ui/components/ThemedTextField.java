package com.mediaviewer.ui.components;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.Document;

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
        add(field, BorderLayout.CENTER);
    }
    
    @Override
    protected void applyTheme() {
        field.setBackground(currentTheme.getInput());
        field.setForeground(currentTheme.getText1());
        field.setCaretColor(currentTheme.getText1());

        if(border != null){
            changeBorderColorByType(this.border);
        }
    }

    public Document getDocument(){
        return this.field.getDocument();
    }

    public String getText(){
        return field.getText();
    }

    private Border changeBorderColorByType(Border border) {
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
            Border innerBorder = changeBorderColorByType(tb.getBorder());
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
            Border newOutside = changeBorderColorByType(cb.getOutsideBorder());
            Border newInside = changeBorderColorByType(cb.getInsideBorder());
            return new CompoundBorder(newOutside, newInside);
            
        } else return null;
    }
}