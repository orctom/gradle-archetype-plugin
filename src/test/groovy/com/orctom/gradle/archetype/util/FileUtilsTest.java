package com.orctom.gradle.archetype.util;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class FileUtilsTest {
    private Map<String, String> map = new HashMap<>();

    public FileUtilsTest() {
        map.put("yes", "Y");
        map.put("no", "N");
        map.put("defo", "definitely");
    }

    @Test
    public void testIfBlocksMapEntryPresent() {
        String resolvedText = FileUtils.resolve("Just before>@IF(yes)@This is present@ENDIF@<Just afterwards", map);

        assertEquals("Just before>This is present<Just afterwards", resolvedText);

        resolvedText = FileUtils.resolve("Just before>@IF(no)@This is present@ENDIF@<Just afterwards", map);

        assertEquals("Just before><Just afterwards", resolvedText);
    }

    @Test
    public void testIfNotBlocksMapEntryPresent() {
        String resolvedText = FileUtils.resolve("Just before>@IF(!no)@This is present@ENDIF@<Just afterwards", map);

        assertEquals("Just before>This is present<Just afterwards", resolvedText);

        resolvedText = FileUtils.resolve("Just before>@IF(!yes)@This is present@ENDIF@<Just afterwards", map);

        assertEquals("Just before><Just afterwards", resolvedText);
    }

    @Test
    public void testIfBlocksMultilineMapEntry() {
        String resolvedText = FileUtils.resolve("Before @IF(defo.startsWith('def'))@inline text@ENDIF@\n" +
                                                     "@IF(yes)@\n" +
                                                     "    This is @defo@ present\n" +
                                                     "    This is on the next line\n" +
                                                     "@ENDIF@\n" +
                                                     "Afterwards\n", map);
        assertEquals("Before inline text\n" +
                "    This is definitely present\n" +
                "    This is on the next line\n" +
                "Afterwards\n", resolvedText);

        resolvedText = FileUtils.resolve("Before @IF(!no.matches('^[Nn]'))@inline text@ENDIF@nothing\\n" +
                                         "@IF(no)@\n" +
                                         "    This is not present\n" +
                                         "@ENDIF@\n" +
                                         "Afterwards\n", map);

        assertEquals("Before nothing\n" +
                     "Afterwards\n", resolvedText);
    }

    @Test
    public void testNestedIfBlocks() {
        String resolvedText = FileUtils.resolve("@IF(yes.matches('^[Yy]'))@This is@IF(yes.matches('^[Yy]'))@ @defo@ nested@ENDIF@ text@ENDIF@", map);

        assertEquals("This is definitely nested text", resolvedText);

    }

    @Test
    public void testIfBlocksMapEntryNotPresent() {
        String resolvedText = FileUtils.resolve("@IF(bob)@Who is bob@ENDIF@", map);

        assertEquals("", resolvedText);
    }
}
