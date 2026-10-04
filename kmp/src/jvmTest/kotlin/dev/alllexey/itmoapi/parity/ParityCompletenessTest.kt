package dev.alllexey.itmoapi.parity

import java.io.File
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Actual converter registrations, not fixture-name strings or request-only fixture reads. */
internal object ParityRegistrations {
    private val paths = ThreadLocal<MutableMap<String, String>?>()

    fun record(path: String, category: String = "domain-model wire equality") {
        paths.get()?.put(path, category)
    }

    fun collect(block: () -> Unit): Map<String, String> {
        check(paths.get() == null) { "Nested parity collection" }
        val registered = linkedMapOf<String, String>()
        paths.set(registered)
        try {
            block()
            return registered.toMap()
        } finally {
            paths.remove()
        }
    }
}

/** Source-backed gates. Generated output stays under the ignored KMP build directory. */
class ParityCompletenessTest {
    private val kmp = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
        .map { if (it.name == "kmp") it else File(it, "kmp") }
        .first { File(it, "src/commonMain").isDirectory }
    private val parity = File(kmp, "src/jvmTest/kotlin/dev/alllexey/itmoapi/parity")
    private val legacyModels = File(kmp.parentFile, "src/main/java").walkTopDown()
        .filter { it.isFile && it.extension == "java" && "/model/" in it.invariantSeparatorsPath }
        .sortedBy { it.invariantSeparatorsPath }.toList()
    private val modernSources = File(kmp, "src/commonMain").walkTopDown()
        .filter { it.isFile && it.extension == "kt" }.toList()
    private val excludedFlows = setOf("Flow", "FlowChain", "FlowChainsWrapper")

    @Test
    fun everyFixtureHasAnExecutedConverterRegistration() {
        val registered = ParityRegistrations.collect {
            parity.listFiles()!!.filter {
                it.name.endsWith("ParityTest.kt") && it.name != "ParityCompletenessTest.kt"
            }.sortedBy { it.name }.forEach { source ->
                val type = Class.forName("dev.alllexey.itmoapi.parity.${source.nameWithoutExtension}")
                val instance = type.getDeclaredConstructor().newInstance()
                type.declaredMethods.filter { method ->
                    method.annotations.any { it.annotationClass.java.name == "org.junit.Test" }
                }.sortedBy { it.name }.forEach { method ->
                    try {
                        method.invoke(instance)
                    } catch (failure: InvocationTargetException) {
                        throw AssertionError("Parity registration failed: ${type.simpleName}.${method.name}", failure.cause)
                    }
                }
            }
        }
        assertTrue(registered.isNotEmpty(), "No parity tests executed")
        val fixtures = File(kmp, "fixtures").walkTopDown().filter { it.isFile }
            .map { it.relativeTo(File(kmp, "fixtures")).invariantSeparatorsPath }.toSet()
        val report = "Registered files by purpose:\n" + registered.toSortedMap().entries.joinToString("\n") { (path, purpose) -> "$path: $purpose" } +
            "\n\nUnregistered fixture files:\n" + (fixtures - registered.keys).sorted().joinToString("\n")
        File(kmp, "build/parity").mkdirs()
        File(kmp, "build/parity/fixture-coverage.txt").writeText(report + "\n")
        assertEquals(emptySet(), fixtures - registered.keys, report)
        assertEquals(emptySet(), registered.keys - fixtures, "Registered fixture does not exist")
    }

    @Test
    fun allLegacyModelsAndMembersAreAccountedFor() {
        assertEquals(84, legacyModels.size, "Legacy model inventory changed; review the snapshot")
        val dependency = api.myitmo.MyItmo::class.java.protectionDomain.codeSource.location.path
        assertTrue(dependency.endsWith("my-itmo-api-1.8.2.jar"), "Parity must use the pinned Central 1.8.2 artifact")
        val problems = mutableListOf<String>()
        val rows = mutableListOf("# 1.x to 2.x member map", "", "Generated from the checked-out source snapshot.", "")
        legacyModels.forEach { legacy ->
            val name = legacy.nameWithoutExtension
            val packageName = Regex("package\\s+([\\w.]+);").find(legacy.readText())!!.groupValues[1]
            Class.forName("$packageName.$name")
            rows += "## ${legacy.relativeTo(kmp.parentFile).invariantSeparatorsPath}"
            rows += ""
            if (name in excludedFlows) {
                rows += "Not ported: deprecated Flow API excluded by the L20 exit policy."
                rows += ""
                return@forEach
            }
            val renamed = if (name == "TokenResponse") "TokenSet" else name
            val candidates = modernSources.filter {
                Regex("\\b(?:class|interface)\\s+$renamed\\b").containsMatchIn(it.readText())
            }
            if (candidates.size != 1) {
                problems += "$name: expected one mapped model, found ${candidates.size}"
                rows += "UNRESOLVED model mapping"
                return@forEach
            }
            val modern = candidates.single()
            rows += "${if (renamed == name) "Ported" else "Renamed"}: ${modern.relativeTo(kmp).invariantSeparatorsPath}"
            rows += ""
            rows += "| Legacy member | Wire key | Modern member |"
            rows += "|---|---|---|"
            val kotlin = declarationsOnly(modelSource(modern, renamed))
            val fields = Regex("(?s)(?<annotations>(?:\\s*@[\\w.]+(?:\\([^\\n]*\\))?\\s*)*)private\\s+(?!static)([\\w<>?,. ]+)\\s+(\\w+)\\s*;")
            fields.findAll(declarationsOnly(legacy.readText())).forEach { field ->
                val member = field.groupValues[3]
                val wire = Regex("@SerializedName\\(\"([^\"]+)\"\\)")
                    .find(field.groups["annotations"]!!.value)?.groupValues?.get(1) ?: member
                val annotated = Regex("@SerialName\\(\"${Regex.escape(wire)}\"\\)\\s*public\\s+val\\s+(\\w+)")
                    .find(kotlin)?.groupValues?.get(1)
                val destination = annotated ?: if (Regex("\\bval\\s+$member\\b").containsMatchIn(kotlin)) member else null
                val converted = if (name == "TokenResponse") when (member) {
                    "expiresIn" -> "accessExpiresAt (seconds converted to Instant using injected Clock)"
                    "refreshExpiresIn" -> "refreshExpiresAt (seconds converted to Instant using injected Clock)"
                    "sessionState" -> "Not retained: ML-04a five-field Storage contract; ItmoIdClient.TokenWire documents ignored session_state"
                    else -> null
                } else null
                if (destination == null && converted == null) problems += "$name.$member ($wire): no mapped member"
                rows += "| $member | $wire | ${destination ?: converted ?: "UNRESOLVED"} |"
            }
            val unknowns = Regex("(?m)^\\s*//\\s*private\\s+[^;]+\\s+(\\w+);").findAll(legacy.readText())
                .map { it.groupValues[1] }.toList()
            unknowns.forEach { unknown ->
                val comments = modern.readLines().filter { it.trimStart().startsWith("//") }.joinToString("\n")
                if (!Regex("\\bval\\s+$unknown\\b").containsMatchIn(comments)) {
                    problems += "$name.$unknown: unknown-type observation was not retained as a commented declaration"
                }
            }
            if (unknowns.isNotEmpty()) rows += "Observed unknown-type declarations (not public members): ${unknowns.joinToString()}. Retained as comments, not invented wire types."
            Regex("\\bextends\\s+(\\w+)").find(legacy.readText())?.let {
                rows += "Inherited members: see the ${it.groupValues[1]} mapping; 2.x may flatten their declarations."
            }
            rows += ""
        }
        File(kmp, "build/parity").mkdirs()
        File(kmp, "build/parity/member-map.md").writeText(rows.joinToString("\n") + "\n")
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test
    fun eachMappedModelRetainsAtLeastItsLegacyDocumentationCount() {
        val deficits = mutableListOf<String>()
        legacyModels.filterNot { it.nameWithoutExtension in excludedFlows }.forEach { legacy ->
            val name = if (legacy.nameWithoutExtension == "TokenResponse") "TokenSet" else legacy.nameWithoutExtension
            val candidates = modernSources.filter { Regex("\\b(?:class|interface)\\s+$name\\b").containsMatchIn(it.readText()) }
            if (candidates.size != 1) {
                deficits += "${legacy.name}: unresolved model mapping"
            } else {
                val javaCount = Regex("/\\*\\*").findAll(legacy.readText()).count()
                val kotlinCount = Regex("/\\*\\*").findAll(modelSource(candidates.single(), name)).count()
                if (kotlinCount < javaCount) deficits += "${legacy.nameWithoutExtension} -> $name: $javaCount Javadoc, $kotlinCount KDoc"
            }
        }
        assertTrue(deficits.isEmpty(), deficits.joinToString("\n"))
    }

    private fun declarationsOnly(text: String): String = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)
        .replace(text, "").lineSequence().filterNot { it.trimStart().startsWith("//") }.joinToString("\n")

    private fun modelSource(file: File, name: String): String {
        val text = file.readText()
        // All models have their own file except the five envelopes. Do not credit sibling KDoc.
        if (file.name != "Envelopes.kt") return text
        val declaration = Regex("public data class $name\\b").find(text) ?: error("Missing model $name")
        val start = text.lastIndexOf("/**", declaration.range.first)
        val next = text.indexOf("\n/**", declaration.range.last)
        return text.substring(start, if (next == -1) text.length else next)
    }
}
