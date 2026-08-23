package com.mediaviewer.ui.panels;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.mediaviewer.model.FilterOptions;
import com.mediaviewer.ui.components.ThemedCheckBox;
import com.mediaviewer.ui.components.ThemedComboBox;
import com.mediaviewer.ui.components.ThemedLabel;
import com.mediaviewer.ui.components.ThemedPanel;
import com.mediaviewer.ui.components.ThemedTextField;
import com.mediaviewer.util.ThemeUtils;

/**
 * Barra de filtros: búsqueda de texto, tipo, ordenación, subcarpetas.
 * Notifica al controlador principal cada vez que cambia algo.
 */
public class FilterBar extends ThemedPanel {
    //private final ThemedPanel       mainPanel;
    private final ThemedTextField   searchField;
    private final ThemedComboBox    typeCombo;
    private final ThemedComboBox    sortCombo;
    private final ThemedCheckBox    recursiveBox;
    private final ThemedLabel       countLabel;
    private final Runnable          onChanged;
    private final Runnable          onChangedRecursive;

    public FilterBar(Runnable onChanged, Runnable onChangedRecursive) {
        super(new FlowLayout(FlowLayout.LEFT, 8, 6), ThemeUtils.PanelType.PANEL, BorderFactory.createMatteBorder(0, 0, 1, 0, Color.WHITE));
        this.onChanged = onChanged;
        this.onChangedRecursive = onChangedRecursive;

        // Icono búsqueda
        ThemedLabel searchIco = new ThemedLabel("🔍", ThemeUtils.TextType.PRIMARY, ThemeUtils.FontSize.MED, Font.PLAIN, ThemeUtils.FontType.EMOJI);
        addToPanel(searchIco);

        // Campo de texto
        Border b = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE),
            BorderFactory.createEmptyBorder(3, 6, 3, 6));
        searchField = new ThemedTextField(20,"Filtrar por nombre (texto parcial)", b);
        
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { onChanged.run(); }
            @Override public void removeUpdate(DocumentEvent e)  { onChanged.run(); }
            @Override public void changedUpdate(DocumentEvent e) { onChanged.run(); }
        });
        addToPanel(searchField);

        // Tipo
        addToPanel(dimLabel("Tipo:"));
        
        typeCombo = new ThemedComboBox(ThemeUtils.TextType.PRIMARY, ThemeUtils.FontSize.SMALL, Font.PLAIN, ThemeUtils.FontType.BASIC,
                                       "Todo", "Imágenes", "GIFs", "Videos");
        typeCombo.addActionListener(e -> onChanged.run());
        addToPanel(typeCombo);

        // Ordenación
        addToPanel(dimLabel("Orden:"));
        sortCombo = new ThemedComboBox(ThemeUtils.TextType.PRIMARY, ThemeUtils.FontSize.SMALL, Font.PLAIN, ThemeUtils.FontType.SYMBOL, 
            "Nombre ↑", "Nombre ↓",
            "Fecha ↑",  "Fecha ↓",
            "Tamaño ↑", "Tamaño ↓");
        sortCombo.addActionListener(e -> onChanged.run());
        addToPanel(sortCombo);

        // Subcarpetas
        recursiveBox = new ThemedCheckBox("Subcarpetas", ThemeUtils.TextType.PRIMARY, ThemeUtils.FontSize.SMALL, Font.PLAIN, ThemeUtils.FontType.BASIC);
        recursiveBox.addActionListener(e -> onChangedRecursive.run());
        addToPanel(recursiveBox);

        // Contador (a la derecha)
        countLabel = new ThemedLabel("—", ThemeUtils.TextType.SECONDARY, ThemeUtils.FontSize.SMALL, Font.PLAIN, ThemeUtils.FontType.BASIC);
        //addToPanel(Box.createHorizontalStrut(20));
        //addToPanel(countLabel);
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
        ThemedLabel l = new ThemedLabel(text, ThemeUtils.TextType.SECONDARY, ThemeUtils.FontSize.SMALL, Font.PLAIN, ThemeUtils.FontType.BASIC);
        return l;
    }
}
