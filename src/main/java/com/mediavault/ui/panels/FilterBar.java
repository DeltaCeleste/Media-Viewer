package com.mediavault.ui.panels;

import com.mediavault.model.FilterOptions;
import com.mediavault.util.Theme;

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

    private final JTextField   searchField;
    private final JComboBox<String> typeCombo;
    private final JComboBox<String> sortCombo;
    private final JCheckBox    recursiveBox;
    private final JLabel       countLabel;
    private final Runnable     onChanged;

    public FilterBar(Runnable onChanged) {
        this.onChanged = onChanged;
        setBackground(Theme.PANEL);
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

        // Icono búsqueda
        JLabel searchIco = new JLabel("🔍");
        searchIco.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        add(searchIco);

        // Campo de texto
        searchField = new JTextField(20);
        searchField.setBackground(Theme.INPUT);
        searchField.setForeground(Theme.TEXT);
        searchField.setCaretColor(Theme.TEXT);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        searchField.setToolTipText("Filtrar por nombre (texto parcial)");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { onChanged.run(); }
            public void removeUpdate(DocumentEvent e)  { onChanged.run(); }
            public void changedUpdate(DocumentEvent e) { onChanged.run(); }
        });
        add(searchField);

        // Tipo
        add(dimLabel("Tipo:"));
        typeCombo = darkCombo("Todo", "Imágenes", "GIFs", "Videos");
        typeCombo.addActionListener(e -> onChanged.run());
        add(typeCombo);

        // Ordenación
        add(dimLabel("Orden:"));
        sortCombo = darkCombo(
            "Nombre ↑", "Nombre ↓",
            "Fecha ↑",  "Fecha ↓",
            "Tamaño ↑", "Tamaño ↓");
        sortCombo.addActionListener(e -> onChanged.run());
        add(sortCombo);

        // Subcarpetas
        recursiveBox = new JCheckBox("Subcarpetas");
        recursiveBox.setBackground(Theme.PANEL);
        recursiveBox.setForeground(Theme.TEXT);
        recursiveBox.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        recursiveBox.addActionListener(e -> onChanged.run());
        add(recursiveBox);

        // Contador (a la derecha)
        countLabel = new JLabel("—");
        countLabel.setForeground(Theme.DIM);
        countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        add(Box.createHorizontalStrut(20));
        add(countLabel);
    }

    /** Devuelve el estado actual de los filtros. */
    public FilterOptions get() {
        return new FilterOptions(
            searchField.getText().toLowerCase().trim(),
            (String) typeCombo.getSelectedItem(),
            (String) sortCombo.getSelectedItem(),
            recursiveBox.isSelected()
        );
    }

    public void setCount(int shown, int total) {
        countLabel.setText(shown + " / " + total + " archivos");
    }

    private static JLabel dimLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Theme.DIM);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        return l;
    }

    private static JComboBox<String> darkCombo(String... items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setBackground(Theme.INPUT);
        cb.setForeground(Theme.TEXT);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        cb.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        return cb;
    }
}
