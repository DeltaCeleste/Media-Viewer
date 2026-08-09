package com.mediaviewer.ui.panels;

import com.mediaviewer.model.FilterOptions;
import com.mediaviewer.util.Theme;
import com.mediaviewer.ui.components.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.function.Supplier;

/**
 * Barra de filtros: búsqueda de texto, tipo, ordenación, subcarpetas.
 * Notifica al controlador principal cada vez que cambia algo.
 */
public class FilterBar extends JPanel {
    private final ThemedPanel       mainPanel;
    private final ThemedTextField   searchField;
    private final ThemedComboBox    typeCombo;
    private final ThemedComboBox    sortCombo;
    private final JCheckBox    recursiveBox;
    private final ThemedLabel       countLabel;
    private final Runnable     onChanged;

    public FilterBar(Runnable onChanged) {
        this.onChanged = onChanged;
        mainPanel = new ThemedPanel(new FlowLayout(FlowLayout.LEFT, 8, 6), BorderFactory.createMatteBorder(0, 0, 1, 0));
        setLaoyut(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        // Icono búsqueda
        ThemedLabel searchIco = new ThemedLabel("🔍", TextType.PRIMARY, FontSize.MED, Font.PLAIN, FontType.EMOJI);
        mainPanel.add(searchIco);

        // Campo de texto
        Border b = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(),
            BorderFactory.createEmptyBorder(3, 6, 3, 6));
        searchField = new ThemedTextField(20,"Filtrar por nombre (texto parcial)", b);
        
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { onChanged.run(); }
            public void removeUpdate(DocumentEvent e)  { onChanged.run(); }
            public void changedUpdate(DocumentEvent e) { onChanged.run(); }
        });
        add(searchField);

        // Tipo
        add(dimLabel("Tipo:"));
        
        typeCombo = new ThemedComboBox(TextType.PRIMARY, FontSize.SMALL, Font.PLAIN, FontType.BASIC,
                                       "Todo", "Imágenes", "GIFs", "Videos");
        typeCombo.addActionListener(e -> onChanged.run());
        add(typeCombo);

        // Ordenación
        add(dimLabel("Orden:"));
        sortCombo = ThemedComboBox(TextType.PRIMARY, FontSize.SMALL, Font.PLAIN, FontType.SYMBOL, 
            "Nombre ↑", "Nombre ↓",
            "Fecha ↑",  "Fecha ↓",
            "Tamaño ↑", "Tamaño ↓");
        sortCombo.addActionListener(e -> onChanged.run());
        add(sortCombo);

        // Subcarpetas
        recursiveBox = new JCheckBox("Subcarpetas", TextType.PRIMARY, FontSize.SMALL, Font.PLAIN, FontType.BASIC);
        recursiveBox.addActionListener(e -> onChanged.run());
        add(recursiveBox);

        // Contador (a la derecha)
        countLabel = new ThemedLabel("—", TextType.SECONDARY, FontSize.SMALL, Font.PLAIN, FontType.BASIC);
        add(Box.createHorizontalStrut(20));
        add(countLabel);
    }

    /** 
     * @brief Devuelve el estado actual de los filtros. 
     */
    public FilterOptions get() {
        return new FilterOptions(
            searchField.getText().toLowerCase().trim(),
            (String) typeCombo.getSelectedItem(),
            (String) sortCombo.getSelectedItem(),
            recursiveBox.isSelected()
        );
    }

    /**
     * @brief Establece la etiqueta que indica cuántos archivos se muestran con los filtros actuales respecto de cuántos hay
     */
    public void setCount(int shown, int total) {
        countLabel.setText(shown + " / " + total + " archivos");
    }

    /**
     * @brief Crea una una etiqueta de texto secundario
     */
    private static ThemedLabel dimLabel(String text) {
        ThemedLabel l = new ThemedLabel(text, TextType.SECONDARY, FontSize.SMALL, Font.PLAIN, FontType.BASIC);
        return l;
    }
}
