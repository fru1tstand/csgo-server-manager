package me.fru1t.csgo_server_manager.files;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public class TextSettingTest {
    private static final String TEST_KEY = "A key";
    private static final String TEST_VALUE = "a value";

    private TextSetting testTextSetting;

    @Test
    public void isValid() {
        testTextSetting = new TextSetting();

        // No values
        assertThat(testTextSetting.isValid().passed).isFalse();

        // Solely a key
        testTextSetting.key = TEST_KEY;
        assertThat(testTextSetting.isValid().passed).isFalse();

        // Both value and children
        testTextSetting.value = TEST_VALUE;
        testTextSetting.children = new HashMap<>();
        assertThat(testTextSetting.isValid().passed).isFalse();

        // Normall just value
        testTextSetting.children = null;
        assertThat(testTextSetting.isValid().passed).isTrue();

        // Normally just children
        testTextSetting.value = null;
        testTextSetting.children = new HashMap<>();
        assertThat(testTextSetting.isValid().passed).isTrue();
    }

    @Test
    public void toFileString() throws InvalidTextSettingException {
        // Error
        testTextSetting = new TextSetting();
        try {
            testTextSetting.toFileString();
            fail("#toFileString should have thrown an exception.");
        } catch (InvalidTextSettingException e) {
            // Expected behavior.
        }

        // Normal operation
        testTextSetting = TextSetting.builder()
            .key("A key")
            .startChildBlock()
            .addChild(TextSetting.builder()
                .key("2nd key")
                .value("a value")
                .build())
            .addChild(TextSetting.builder()
                .key("3rd key")
                .value("another value")
                .build())
            .build();

        assertThat(testTextSetting.toFileString()).isEqualTo("\"A key\" {\n"
            + TextSetting.INDENTATION_PATTERN + "\"3rd key\" \"another value\"\n"
            + TextSetting.INDENTATION_PATTERN + "\"2nd key\" \"a value\"\n"
            + "}\n");
    }

}
