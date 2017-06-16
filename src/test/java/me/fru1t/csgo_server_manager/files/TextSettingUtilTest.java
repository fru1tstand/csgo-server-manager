package me.fru1t.csgo_server_manager.files;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Scanner;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public class TextSettingUtilTest {
    private static final String TEST_FILE = "\"test\" { \"testkey\" \"testvalue\" "
        + "\"testkeywithchildren\" { \"testkey2\" \"testvalue2\" \"testkey3\" \"testvalue3\" }}";

    @Test
    public void processFile() throws InvalidFileFormatException {
        // Non-balanced braces
        assertProcessError("\"root\" { \"key\" \"value\"");

        // Key no value or body
        assertProcessError("\"root\" { \"key\" }");

        // body with no key
        assertProcessError("{ \"key\" \"value\" }");

        // Invalid character
        assertProcessError("unexpectedkey \"value\"");

        // Invalid comment 1
        assertProcessError("\"root\" / comment");

        // Invalid comment 2
        assertProcessError("\"root\" /");

        // Invalid comment hiding a value (Unexpected EOF)
        assertProcessError("\"root\" // \"value hidden by comment\"");

        // Invalid no end of string
        assertProcessError("\"root");

        // Valid
        Scanner testFileScanner = new Scanner(TEST_FILE);
        TextSetting test = TextSettingUtil.processFile(testFileScanner);
        assertThat(test.key).isEqualTo("test");
    }

    private static void assertProcessError(String s) {
        Scanner testFileScanner = new Scanner(s);
        try {
            TextSettingUtil.processFile(testFileScanner);
            fail("Process should have errored");
        } catch (InvalidFileFormatException e) {
            // Expected behavior
        }
    }
}
