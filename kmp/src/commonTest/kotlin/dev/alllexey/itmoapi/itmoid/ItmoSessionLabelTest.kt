package dev.alllexey.itmoapi.itmoid

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ItmoSessionLabelTest {
    @Test
    fun botLabelsMatchObservedVectors() {
        assertEquals("ITMO Widgets agent bot/1.0 (X11; Linux x86_64)", ItmoSessionLabel.botUserAgent("agent", null, "1.0", "linux"))
        assertEquals("ITMO Widgets MCP bot/0.3", ItmoSessionLabel.botUserAgent("MCP", "", "0.3", null))
        assertEquals("ITMO Widgets agent nas bot/1.2 (X11; Linux x86_64)", ItmoSessionLabel.botUserAgent("agent", "nas", "1.2", "linux"))
        assertEquals("ITMO Widgets MCP laptop bot/2.0.1 (Macintosh; Intel Mac OS X 10_15_7)", ItmoSessionLabel.botUserAgent("MCP", "laptop", "2.0.1", "macos"))
        assertEquals("ITMO Widgets agent pc bot/1 (Windows NT 10.0; Win64; x64)", ItmoSessionLabel.botUserAgent("agent", "pc", null, "Windows"))
    }

    @Test
    fun namesKeepOnlyTheAllowedCharactersAndCollapseSpaces() {
        assertEquals("ITMO Widgets agent my-nas_2 [home]! bot/1 (X11; Linux x86_64)", ItmoSessionLabel.botUserAgent("agent", "  my-nas_2   [home]!  ", "", "linux"))
        assertEquals("ITMO Widgets agent nashome bot/1", ItmoSessionLabel.botUserAgent("agent", "nas.home", "1", "freebsd"))
        assertEquals("ITMO Widgets MCP bot/1", ItmoSessionLabel.botUserAgent("MCP", "Ноутбук", "1", null))
        assertEquals("ITMO Widgets agent a:b c bot/1", ItmoSessionLabel.botUserAgent("agent", "a:\tb /c", "1", null))
    }

    @Test
    fun theNameBeforeTheVersionStaysWithin50Characters() {
        val label = ItmoSessionLabel.botUserAgent("agent", "x".repeat(10) + " " + "y".repeat(60), "3.1", "linux")
        val name = label.substringBefore('/')
        assertEquals(50, name.length)
        assertTrue(name.endsWith(" bot") && name.startsWith("ITMO Widgets agent xxxxxxxxxx yyy"), "Name cut before bot")
        assertEquals("ITMO Widgets agent xxxxxxxxxx bot/1", ItmoSessionLabel.botUserAgent("agent", "x".repeat(10) + "   ", "1", null))
    }

    @Test
    fun versionsOutsideTheAllowedShapeBecomeOne() {
        for (version in listOf("1.2.3.4", "v1", "1.", "1-beta", "", " ")) {
            assertEquals("ITMO Widgets agent bot/1", ItmoSessionLabel.botUserAgent("agent", null, version, null))
        }
        assertEquals("ITMO Widgets agent bot/10.20.30", ItmoSessionLabel.botUserAgent("agent", null, " 10.20.30 ", null))
    }

    @Test
    fun browserUserAgentIsDesktopChromeForThePlatformAndLinuxByDefault() {
        val linux = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36"
        assertEquals(linux, ItmoSessionLabel.browserUserAgent("linux"))
        assertEquals(linux, ItmoSessionLabel.browserUserAgent(null))
        assertEquals(linux, ItmoSessionLabel.browserUserAgent("android"))
        assertEquals("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36", ItmoSessionLabel.browserUserAgent("macos"))
        assertEquals("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36", ItmoSessionLabel.browserUserAgent("windows"))
    }
}
