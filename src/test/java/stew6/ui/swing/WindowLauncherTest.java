package stew6.ui.swing;

import static org.junit.Assert.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import org.hamcrest.*;
import org.junit.*;
import org.junit.rules.*;
import stew6.*;
import stew6.ui.swing.WindowLauncher.*;

public class WindowLauncherTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testConfigurationLoad() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        final File dir = tmpFolder.newFolder(testName);
        Configuration config = new Configuration();
        final File f1 = new File(dir, "stew5-config1.xml");
        final File f2 = new File(dir, "stew5-config2.xml");
        config.saveTo(f1);
        config.setAlwaysOnTop(true);
        config.setShowInfoTree(true);
        config.setShowStatusBar(true);
        config.setShowTableColumnNumber(true);
        config.setDividerLocation(33);
        config.setDividerLocation0(66);
        config.setAutoAdjustMode("adjust");
        config.setPostProcessMode("post-process");
        config.setLocation(new Point(123, 456));
        config.setSize(new Dimension(1182, 968));
        config.saveTo(f2);
        Configuration loaded1 = Configuration.loadFrom(f1);
        assertEquals(new Point(200, 200), loaded1.getLocation());
        assertEquals(-1, loaded1.getDividerLocation());
        assertFalse(loaded1.isAlwaysOnTop());
        assertFalse(loaded1.isShowInfoTree());
        assertFalse(loaded1.isShowStatusBar());
        assertFalse(loaded1.isShowTableColumnNumber());
        assertEquals(-1, loaded1.getDividerLocation());
        assertEquals(-1, loaded1.getDividerLocation0());
        assertEquals("autoAdjustMode", loaded1.getAutoAdjustMode());
        assertEquals("postProcessMode", loaded1.getPostProcessMode());
        assertEquals(new Point(200, 200), loaded1.getLocation());
        assertEquals(new Dimension(640, 480), loaded1.getSize());
        Configuration loaded2 = Configuration.loadFrom(f2);
        assertTrue(loaded2.isAlwaysOnTop());
        assertTrue(loaded2.isShowInfoTree());
        assertTrue(loaded2.isShowStatusBar());
        assertTrue(loaded2.isShowTableColumnNumber());
        assertEquals(33, loaded2.getDividerLocation());
        assertEquals(66, loaded2.getDividerLocation0());
        assertEquals("adjust", loaded2.getAutoAdjustMode());
        assertEquals("post-process", loaded2.getPostProcessMode());
        assertEquals(new Point(123, 456), loaded2.getLocation());
        assertEquals(new Dimension(1182, 968), loaded2.getSize());
    }

    @Test
    public void testConfigurationSave() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        final File dir = tmpFolder.newFolder(testName);
        Configuration config = new Configuration();
        final File f = new File(dir, "stew5-config.xml");
        config.saveTo(f);
        String result = String.join("%n", Files.readAllLines(f.toPath(), StandardCharsets.UTF_8));
        assertThat(result, Matchers.containsString("java.util.HashMap"));
        assertThat(result, Matchers.containsString("showInfoTree"));
        assertThat(result, Matchers.containsString("location"));
        assertThat(result, Matchers.containsString("dividerLocation"));
        assertThat(result, Matchers.containsString("autoAdjustMode"));
        assertThat(result, Matchers.containsString("showStatusBar"));
        assertThat(result, Matchers.containsString("showTableColumnNumber"));
        assertThat(result, Matchers.containsString("postProcessMode"));
        assertThat(result, Matchers.containsString("dividerLocation0"));
        assertThat(result, Matchers.containsString("alwaysOnTop"));
    }

}
