package dev.alllexey.itmoapi.testing

/** Shared by JVM and Native; generated from the entire synthetic fixture tree at build time. */
internal fun fixture(path: String): String = generatedFixtures[path] ?: error("Unknown fixture path")

internal fun fixturePaths(): Set<String> = generatedFixtures.keys
