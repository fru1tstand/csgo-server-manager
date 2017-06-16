package me.fru1t.csgo_server_manager.files;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.annotation.Nullable;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class TextSettingBuilderTest {
    private static final String TEST_KEY = "key";
    private static final String TEST_VALUE = "value";

    private TextSetting testChild;

    @Before
    public void setUp() throws InvalidTextSettingException {
        testChild = TextSetting.builder().key("test-key").value("test-value").build();
    }

    @Test
    public void build() throws Exception {
        TextSetting.TextSettingBuilder test = null;

        // No key
        test = makeBuilder(null, TEST_VALUE, null);
        assertBuildError(test);

        // No value
        test = makeBuilder(TEST_KEY, null, null);
        assertBuildError(test);

        // Both value and child error
        test = makeBuilder(TEST_KEY, TEST_VALUE, testChild);
        assertBuildError(test);

        // Valid key-value
        test = makeBuilder(TEST_KEY, TEST_VALUE, null);
        test.build();

        // Valid key-child
        test = makeBuilder(TEST_KEY, null, testChild);
        test.build();
    }

    private static TextSetting.TextSettingBuilder makeBuilder(@Nullable String key,
        @Nullable String value, @Nullable TextSetting child) {
        TextSetting.TextSettingBuilder result = TextSetting.builder();
        if (key != null) {
            result.key(key);
        }
        if (value != null) {
            result.value(value);
        }
        if (child != null) {
            result.startChildBlock();
            result.addChild(child);
        }
        return result;
    }

    private static void assertBuildError(TextSetting.TextSettingBuilder subject) {
        try {
            subject.build();
            fail("Build should have failed");
        } catch (InvalidTextSettingException e) {
            // Expected behavior
        }
    }
}
