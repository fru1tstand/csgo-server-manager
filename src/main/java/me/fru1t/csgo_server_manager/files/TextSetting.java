package me.fru1t.csgo_server_manager.files;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import me.fru1t.commons.lang.Result;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>A layer of settings in the valve settings format. Examples of this format may be found within
 * the gamemodes.txt file. Essentially, a whitespace-agnostic file structure which has a mapping
 * of keys to values, where values may be strings or a nested block. The following is an example
 * of the possible combinations:</p>
 * <pre>
 *     // This is comment
 *     "key1" { // Braces start new children
 *         "key2" "key 2's value"
 *         "key3" "key 3's value"
 *         "key4" {
 *             ...
 *         }
 *     }
 * </pre>
 * <p>Note how a key can be assigned either a value or a block, but not both. In this TextSetting
 * implementation, this dynamic is encapsulated with this single class that contains a mandatory
 * <i>key</i> field, and optional <i>value</i> and <i>children</i> fields. Each key corresponds to
 * a single TextSetting object, where either the value is set, or in the case of a block, the
 * children are set.</p>
 */
public class TextSetting {
    public static class TextSettingBuilder {
        @Nullable
        private String key;
        @Nullable
        private String value;
        @Nullable
        private Map<String, TextSetting> children;

        TextSettingBuilder() {
            key = null;
            value = null;
            children = null;
        }

        @Nullable
        public String key() {
            return key;
        }

        public TextSettingBuilder key(String key) {
            this.key = key;
            return this;
        }

        @Nullable
        public String value() {
            return value;
        }

        public TextSettingBuilder value(@Nullable String value) {
            this.value = value;
            return this;
        }

        @Nullable
        public Map<String, TextSetting> children() {
            return children == null ? null : ImmutableMap.copyOf(children);
        }

        /**
         * Clears the children from this builder, or does nothing if {@link #startChildBlock()}
         * hasn't been called.
         */
        public TextSettingBuilder clearChildren() {
            if (children != null) {
                children.clear();
            }
            return this;
        }

        public TextSettingBuilder deleteChildren() {
            children = null;
            return this;
        }

        /**
         * Adds a child to this builder, if {@link #startChildBlock()} has been called. Otherwise,
         * ignores any given child.
         */
        public TextSettingBuilder addChild(TextSetting child) {
            if (children != null) {
                children.put(child.key, child);
            }
            return this;
        }

        public TextSettingBuilder startChildBlock() {
            children = new HashMap<>();
            return this;
        }

        public TextSetting build() throws InvalidTextSettingException {
            if (key == null) {
                throw new InvalidTextSettingException("TextSettings must have a key: "
                    + toString());
            }
            if (value == null && children == null) {
                throw new InvalidTextSettingException("Either the value or children must be set: "
                    + toString());
            }
            if (value != null && children != null) {
                throw new InvalidTextSettingException("Either the value or children must be set, "
                    + "but not both: " + toString());
            }

            TextSetting result = new TextSetting();
            result.key = key;
            result.children = children == null ? null : ImmutableMap.copyOf(children);
            result.value = value;
            return result;
        }

        @Override
        public String toString() {
            return "TextSettingBuilder{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", children=" + (children == null ? "" : children.toString()) +
                '}';
        }
    }

    protected static final String INDENTATION_PATTERN = "  ";
    private static final String KEY_VALUE_DELIMITER = " ";

    public static TextSettingBuilder builder() {
        return new TextSettingBuilder();
    }

    @Getter
    @Nonnull
    protected String key;

    @Getter
    @Nullable
    protected String value;

    @Getter
    @Nullable
    protected Map<String, TextSetting> children;

    public TextSetting() {
        key = "";
        value = null;
        children = null;
    }

    /**
     * @return Whether or not this TextSetting is valid. That is, if a key exists, and either
     * the value OR children are set, but not both, and not neither.
     */
    public Result isValid() {
        if (key.isEmpty()) {
            return Result.fail("TextSetting must have a key.");
        }
        if (value == null && children == null) {
            return Result.fail("TextSetting must have either a value OR contain children. "
                + "(Key: " + key + ")");
        }
        if (value != null && children != null) {
            return Result.fail(
                "TextSetting must have either a value OR contain children, but not both. "
                    + "(Key: " + key + ")");
        }
        return Result.pass();
    }

    /**
     * @return An attempt at {@link #toFileString()}. This will fail silently and return an
     * empty string if it does.
     * @see #toFileString()
     */
    @Override
    public String toString() {
        try {
            return toFileString();
        } catch (InvalidTextSettingException e) {
            // Do nothing.
        }
        return "";
    }

    /**
     * @return A file write-able string of this TextSetting container. Recursively navigates
     * through children.
     * @throws InvalidTextSettingException Thrown if any TextSetting within this hierarchy is
     * invalid.
     */
    public String toFileString() throws InvalidTextSettingException {
        StringBuilder result = new StringBuilder();
        toFileString(0, result);
        return result.toString();
    }

    private void toFileString(int tabs, StringBuilder result) throws InvalidTextSettingException {
        // Sanity check
        Result r = isValid();
        if (!r.passed) {
            throw new InvalidTextSettingException("An error occurred while converting this "
                + "TextSetting to a file string. Please check where it's being created and that it "
                + "adheres to the following error: " + r.context);
        }

        String indentation = StringUtils.repeat(INDENTATION_PATTERN, tabs);

        // Always show key
        result.append(indentation).append("\"").append(key).append("\"");

        // Check if value
        if (value != null) {
            result.append(KEY_VALUE_DELIMITER).append("\"").append(value).append("\"\n");
            return;
        }

        // Otherwise, it's a child-bearing settings block
        result.append(" {\n");

        if (children != null) {
            for (TextSetting child : children.values()) {
                child.toFileString(tabs + 1, result);
            }
        }

        result.append(indentation).append("}\n");
    }
}
