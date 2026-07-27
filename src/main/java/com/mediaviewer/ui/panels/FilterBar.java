package com.mediaviewer.ui.panels;

import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.mediaviewer.model.FilterOptions;
import com.mediaviewer.util.Theme;

/**
 * Barra de filtros: búsqueda de texto, tipo, ordenación, subcarpetas.
 * Notifica al controlador principal cada vez que cambia algo.
 */
public class FilterBar extends JPanel {

    private final JTextField        searchField;
    private final JComboBox<String> typeCombo;
    private final JComboBox<String> sortCombo;
    private final JCheckBox         recursiveBox;
    private final JLabel            countLabel;
    private final Runnable          onChangedOrder;
    private final Runnable          onChangedRecursive;

    public FilterBar(Runnable onChangedOrder, Runnable onChangedRecursive) {
        this.onChangedOrder = onChangedOrder;
        this.onChangedRecursive = onChangedRecursive;
        setBackground(Theme.PANEL);
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

        // Icono búsqueda
        JLabel searchIco = new JLabel("🔍");
        searchIco.setFont(new Font(Theme.FONT_EMOJI, Font.PLAIN, 14));
        searchIco.setForeground(Theme.TEXT);
        add(searchIco);

        // Campo de texto
        searchField = new JTextField(20);
        searchField.setBackground(Theme.INPUT);
        searchField.setForeground(Theme.TEXT);
        searchField.setCaretColor(Theme.TEXT);
        searchField.setFont(Theme.FONT_SMALL);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        searchField.setToolTipText("Filtrar por nombre (texto parcial)");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { onChangedOrder.run(); }
            public void removeUpdate(DocumentEvent e)  { onChangedOrder.run(); }
            public void changedUpdate(DocumentEvent e) { onChangedOrder.run(); }
        });
        add(searchField);

        // Tipo
        add(dimLabel("Tipo:"));
        typeCombo = darkCombo("Todo", "Imágenes", "GIFs", "Videos");
        typeCombo.addActionListener(e -> onChangedOrder.run());
        add(typeCombo);

        // Ordenación
        add(dimLabel("Orden:"));
        sortCombo = darkCombo(
            "Nombre ↑", "Nombre ↓",
            "Fecha ↑",  "Fecha ↓",
            "Tamaño ↑", "Tamaño ↓");
        sortCombo.addActionListener(e -> onChangedOrder.run());
        add(sortCombo);

        // Subcarpetas
        recursiveBox = new JCheckBox("Subcarpetas");
        recursiveBox.setBackground(Theme.PANEL);
        recursiveBox.setForeground(Theme.TEXT);
        recursiveBox.setFont(Theme.FONT_SMALL);
        recursiveBox.addActionListener(e -> onChangedRecursive.run());
        add(recursiveBox);

        // Contador (a la derecha)
        countLabel = new JLabel("—");
        countLabel.setForeground(Theme.TEXT2);
        countLabel.setFont(Theme.FONT_SMALL);
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
    private static JLabel dimLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Theme.TEXT2);
        l.setFont(Theme.FONT_SMALL);
        return l;
    }

    /**
     * @brief Crea un menú desplegable
     */
    private static JComboBox<String> darkCombo(String... items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setBackground(Theme.INPUT);
        cb.setForeground(Theme.TEXT);
        cb.setFont(Theme.FONT_SMALL);
        return cb;
    }
}
