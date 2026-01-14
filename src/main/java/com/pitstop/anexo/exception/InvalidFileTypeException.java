package com.pitstop.anexo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção lançada quando o tipo de arquivo não é permitido.
 */
@ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
public class InvalidFileTypeException extends RuntimeException {

    private final String mimeType;

    public InvalidFileTypeException(String mimeType) {
        super("Tipo de arquivo não permitido: " + mimeType + ". Tipos aceitos: JPEG, PNG, WebP, PDF");
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }
}
