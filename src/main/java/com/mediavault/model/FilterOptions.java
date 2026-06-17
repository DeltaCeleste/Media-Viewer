package com.mediavault.model;

/** Snapshot inmutable de los filtros activos. */
public record FilterOptions(
    String  searchText,   // texto de búsqueda en minúsculas
    String  typeFilter,   // "Todo" | "Imágenes" | "GIFs" | "Videos"
    String  sortKey,      // "Nombre ↑" | "Nombre ↓" | "Fecha ↑" | ...
    boolean recursive
) {}
