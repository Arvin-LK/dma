package com.dma.common.exception;
public class ConversionException extends DmaException {
    public ConversionException(String message) { super("CONV_001", message); }
    public ConversionException(String message, Throwable cause) { super("CONV_001", message, cause); }
}
