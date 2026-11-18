package dev.alllexey.itmoapi.itmoid

/** User-Agents for an ITMO.ID sign-in. The UA of the password sign-in fixes the session's row on
 * id.itmo.ru `/activity` ("`os`, Browser: `browser`", parsed by ua-parser) for the session's whole life;
 * refreshes and silent sign-ins with another UA do not change it.
 * Platforms are `linux`, `macos` and `windows` (any case); anything else is no platform.
 */
public object ItmoSessionLabel {
    /** Latest stable Chrome major version; bumped with releases, since an old version is a tell of its own. */
    public const val CHROME_MAJOR_VERSION: Int = 155

    /** An ordinary desktop Chrome UA for [platform] (no platform: Linux), so the row reads like
     * "Linux, Browser: Chrome 155".
     */
    public fun browserUserAgent(platform: String?): String =
        "Mozilla/5.0 (${os(platform) ?: LINUX}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$CHROME_MAJOR_VERSION.0.0.0 Safari/537.36"

    /** The honest label `ITMO Widgets <client> <name> bot/<version> (<os>)`, which ua-parser reads through its
     * generic rule for names ending in `bot`: agent `nas` 1.2 on Linux shows "Linux, Browser: ITMO Widgets agent
     * nas bot/1.2". [client] and [name] keep only `[A-Za-z0-9 _!:\[\]-]` with spaces collapsed (Cyrillic and dots
     * break the rule), empty ones are omitted, and the part before `/` is cut to 50 characters. [version] must be
     * `\d+(\.\d+){0,2}`, else `1`. No platform means no parentheses, and ITMO then shows the client name `my.itmo`.
     */
    public fun botUserAgent(client: String, name: String?, version: String?, platform: String?): String {
        val words = listOfNotNull(client, name).map(::clean).filter { it.isNotEmpty() }
        val head = (listOf("ITMO Widgets") + words).joinToString(" ").take(MAX_NAME - " bot".length).trimEnd()
        val number = version?.trim()?.takeIf { VERSION.matches(it) } ?: "1"
        return "$head bot/$number" + (os(platform)?.let { " ($it)" } ?: "")
    }

    private fun clean(value: String): String =
        value.filter { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it in " _!:[]-" }.split(' ').filter { it.isNotEmpty() }.joinToString(" ")

    private fun os(platform: String?): String? = when (platform?.lowercase()) {
        "linux" -> LINUX
        "macos" -> "Macintosh; Intel Mac OS X 10_15_7"
        "windows" -> "Windows NT 10.0; Win64; x64"
        else -> null
    }

    private const val LINUX = "X11; Linux x86_64"
    private const val MAX_NAME = 50
    private val VERSION = Regex("""\d+(\.\d+){0,2}""")
}
