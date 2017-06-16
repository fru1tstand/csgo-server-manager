package me.fru1t.csgo_server_manager.files;

import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Utility methods for encoding and decoding files to/from TextSetting objects.
 */
public class TextSettingUtil {
    private enum TokenType {
        BEGIN_CHILD, END_CHILD, STRING, COMMENT, ERROR, END_OF_FILE
    }

    @RequiredArgsConstructor
    private static class Token {
        @Nonnull TokenType type;
        @Nonnull String content;
    }

    private static class TextSettingProcessor {
        private char[] currentLine;
        private int nextUnusedIndex;
        private Scanner contentScanner;
        private int currentLineNumber;

        TextSettingProcessor(Scanner contentScanner) {
            this.contentScanner = contentScanner;
            nextUnusedIndex = 0;
            currentLine = new char[]{};
            currentLineNumber = 0;
        }

        Token nextToken() {
            // Find start of next token
            while (++nextUnusedIndex < currentLine.length) {
                switch (currentLine[nextUnusedIndex]) {
                    // Begin child
                    case '{':
                        return new Token(TokenType.BEGIN_CHILD, "{");

                    // End child
                    case '}':
                        return new Token(TokenType.END_CHILD, "}");

                    // String
                    case '"':
                        // Don't include the starting quote mark
                        int sIndex = nextUnusedIndex + 1;

                        // Find the ending quote mark that's not escaped
                        while (++nextUnusedIndex < currentLine.length) {
                            if (currentLine[nextUnusedIndex] == '"'
                                && currentLine[nextUnusedIndex - 1] != '\\') {
                                return new Token(TokenType.STRING, new String(currentLine, sIndex,
                                    nextUnusedIndex - sIndex));
                            }
                        }
                        return detailedTokenError("There was no ending to the string.");

                    // Comment
                    case '/':
                        // Are we at the end of the current line? If so, this character is invalid
                        if (++nextUnusedIndex >= currentLine.length) {
                            return detailedTokenError("Thought it was a comment, but "
                                + "there was only a single slash before the end of the line.");
                        }

                        // Is the next character another slash? If not, this character is invalid
                        if (currentLine[nextUnusedIndex] != '/') {
                            return detailedTokenError("Thought it was a comment, but "
                                + "there was only a single slash.");
                        }

                        // Otherwise, the rest of this line is a comment, but don't include the "//"
                        int cIndex = nextUnusedIndex + 1;
                        nextUnusedIndex = currentLine.length;
                        return new Token(TokenType.COMMENT, new String(currentLine, cIndex,
                            currentLine.length - cIndex));

                    // Space/tab
                    case ' ':
                    case '\t':
                        // Ignore whitespace.
                        break;

                    // Everything else
                    default:
                        // Error. In a more lenient mode, this could be a break statement to ignore
                        // unexpected/invalid characters.
                        return detailedTokenError("It's simply an invalid character.");
                }
            }

            // If we didn't find anything at all, it means we reached the end of the current line
            // and need to queue the next from the scanner
            if (!contentScanner.hasNext()) {
                return new Token(TokenType.END_OF_FILE, "");
            }
            ++currentLineNumber;
            currentLine = contentScanner.nextLine().toCharArray();
            nextUnusedIndex = -1;
            return nextToken();
        }

        Token detailedTokenError(String context) {
            if (nextUnusedIndex >= 0 && nextUnusedIndex < currentLine.length) {
                return new Token(TokenType.ERROR,
                    "\tInvalid character: '" + currentLine[nextUnusedIndex] + "'"
                        + "\n\tOffset: " + nextUnusedIndex
                        + "\n\tFull line: " + new String(currentLine)
                        + "\n\tContext: " + context);
            } else {
                return new Token(TokenType.ERROR, "\tFull line: " + new String(currentLine)
                        + "\n\tContext: " + context);
            }
        }
    }

    // Uninstantiable
    private TextSettingUtil() { }

    /**
     * Attempts to process a given file into a TextSetting object.
     * @param fileScanner The file to process.
     * @return The processed file in TextSetting form.
     */
    public static TextSetting processFile(Scanner fileScanner) throws InvalidFileFormatException {
        TextSettingProcessor processor = new TextSettingProcessor(fileScanner);
        return processFile(null, processor);
    }

    // Each call to processFileHelper should handle a single block of key-value pairs from the
    // processor.
    @Nullable
    private static TextSetting processFile(@Nullable TextSetting.TextSettingBuilder root,
        TextSettingProcessor processor)
        throws InvalidFileFormatException {
        @Nullable TextSetting.TextSettingBuilder current = null;

        try {
            while (true) {
                Token token = processor.nextToken();
                switch (token.type) {
                    case STRING:
                        if (current == null) {
                            // It's a key
                            current = TextSetting.builder().key(token.content);
                        } else {
                            // It's a value
                            if (root == null) {
                                // Set as root
                                root = current;
                            } else {
                                // Otherwise add it as a child
                                root.addChild(current.value(token.content).build());
                            }
                            current = null;
                        }
                        break;

                    case COMMENT:
                        // Ignore comments
                        break;

                    case END_CHILD:
                        if (current != null) {
                            // We're in the middle of a key-value pair, it's invalid to end the block
                            throw new InvalidFileFormatException("Expected a value or begin child "
                                + "marker, but found a end marker for a child block instead. Line "
                                + processor.currentLineNumber);
                        }

                        // Otherwise, it's a valid end, so we're done with this recursive loop
                        return null;

                    case BEGIN_CHILD:
                        if (current == null) {
                            // There's no key to this child block. It's invalid to start one.
                            throw new InvalidFileFormatException(
                                "Expected a key, but found the start to a child block instead. "
                                    + "Line " + processor.currentLineNumber);
                        }

                        // Otherwise recurse down the child
                        current.startChildBlock();
                        if (processFile(current, processor) != null) {
                            throw new InvalidFileFormatException("Unexpected end of file. There's "
                                + "an imbalance of braces somewhere in your file. We can't "
                                + "pinpoint where it is, because to us, it looks like the end of "
                                + "the file.");
                        }
                        if (root == null) {
                            root = current;
                        } else {
                            TextSetting built = current.build();
                            root.addChild(built);
                        }
                        current = null;
                        break;

                    case END_OF_FILE:
                        if (current != null) {
                            throw new InvalidFileFormatException("Unexpected end of file. There's "
                                + "an imbalance of key-value pairs somewhere in your file.");
                        }
                        if (root != null) {
                            return root.build();
                        }
                        return null;

                    case ERROR:
                    default:
                        throw new InvalidFileFormatException(
                            "There was an error processing the file: \n\n%s"
                                + "\n\tLine: " + processor.currentLineNumber
                                + "\n", token.content);
                }
            }
        } catch (InvalidTextSettingException e) {
            throw new InvalidFileFormatException(e.getMessage() + "\nLine: "
                + processor.currentLineNumber);
        }
    }
}
