package me.fru1t.csgo_server_manager.files;

/**
 * Thrown when the file processor detects a file to be of invalid format.
 */
public class InvalidFileFormatException extends Exception {
    public InvalidFileFormatException(String s) {
        super(s);
    }

    public InvalidFileFormatException(String stringFormat, Object... args) {
        this(String.format(stringFormat, args));
    }
}
