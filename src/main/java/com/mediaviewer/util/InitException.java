package com.mediaviewer.util;


/**
 * Clase de excepción para controlar la inicialización asíncrona
 */
public class InitException extends Exception {
    /* Código de error
     * -1 - Fallo confirmado durante la inicialización
     * 0  - Inicialización empezada pero no acabada
     * 1  - Inicialización en proceso correctamente pero no finalizado
     */
    private final int errorCode;

    public InitException(String message) {
        super(message);
        errorCode = 0;
    }

    public InitException(String message, int errCode) {
        super(message);
        errorCode = errCode;
    }

    public int getErrorCode() { return errorCode; }
}