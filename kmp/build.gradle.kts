import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.api.publish.PublishingExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish) apply false
}

// Use the base DSL so signing is controlled only by the in-memory key below.
apply(plugin = "com.vanniktech.maven.publish.base")

group = providers.gradleProperty("GROUP").get()
version = providers.gradleProperty("VERSION_NAME").get()

kotlin {
    explicitApi()
    coreLibrariesVersion = "2.2.0"
    jvmToolchain(17)
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
        optIn.addAll("kotlin.time.ExperimentalTime", "kotlin.uuid.ExperimentalUuidApi")
    }
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosArm64()
    iosSimulatorArm64()
    targets.withType<KotlinNativeTarget>().configureEach {
        // Remove checkout paths from klibs for identical Mac/Linux publications.
        compilerOptions.freeCompilerArgs.add("-Xklib-relative-path-base=${rootDir.absolutePath}")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        val appleMain = create("appleMain") {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        iosArm64Main.get().dependsOn(appleMain)
        iosSimulatorArm64Main.get().dependsOn(appleMain)
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(libs.mockwebserver)
            implementation(libs.myitmoapi.legacy)
        }
    }
}

tasks.withType<KotlinNativeSimulatorTest>().configureEach {
    device = "iPhone 17"
}

// Common constants avoid platform resource APIs; every new fixture is discovered automatically.
val fixtureDirectory = layout.projectDirectory.dir("fixtures")
val generatedFixtures = layout.buildDirectory.dir("generated/fixtures/commonTest")
val generateFixtures = tasks.register("generateFixtures") {
    inputs.files(fileTree(fixtureDirectory) { include("**/*.json", "**/*.html") })
    outputs.dir(generatedFixtures)
    doLast {
        fun quoted(value: String): String = buildString {
            append('"')
            value.forEach { character ->
                when (character) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '$' -> append("\\$")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (character.code < 32) {
                        append("\\u%04x".format(character.code))
                    } else {
                        append(character)
                    }
                }
            }
            append('"')
        }
        val root = fixtureDirectory.asFile
        val entries = inputs.files.files.sortedBy { it.relativeTo(root).invariantSeparatorsPath }.joinToString(",\n") {
            "        ${quoted(it.relativeTo(root).invariantSeparatorsPath)} to ${quoted(it.readText())}"
        }
        val output = generatedFixtures.get().file("dev/alllexey/itmoapi/testing/GeneratedFixtures.kt").asFile
        output.parentFile.mkdirs()
        output.writeText(
            "package dev.alllexey.itmoapi.testing\n\n" +
                "internal val generatedFixtures: Map<String, String> = mapOf(\n$entries\n)\n"
        )
    }
}
kotlin.sourceSets.commonTest {
    kotlin.srcDir(generatedFixtures)
}
tasks.matching { it.name.startsWith("compileTestKotlin") }.configureEach {
    dependsOn(generateFixtures)
}

extensions.configure<MavenPublishBaseExtension> {
    configure(KotlinMultiplatform(JavadocJar.Empty(), SourcesJar.Sources()))
    coordinates(
        groupId = providers.gradleProperty("GROUP").get(),
        artifactId = providers.gradleProperty("POM_ARTIFACT_ID").get(),
        version = providers.gradleProperty("VERSION_NAME").get(),
    )
    publishToMavenCentral(automaticRelease = false)
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    pom {
        name.set(providers.gradleProperty("POM_NAME"))
        description.set(providers.gradleProperty("POM_DESCRIPTION"))
        url.set(providers.gradleProperty("POM_URL"))
        licenses {
            license {
                name.set(providers.gradleProperty("POM_LICENSE_NAME"))
                url.set(providers.gradleProperty("POM_LICENSE_URL"))
            }
        }
        developers {
            developer {
                name.set(providers.gradleProperty("POM_DEVELOPER_NAME"))
                organization.set(providers.gradleProperty("POM_DEVELOPER_ORGANIZATION"))
                organizationUrl.set(providers.gradleProperty("POM_DEVELOPER_ORGANIZATION_URL"))
            }
        }
        scm {
            connection.set(providers.gradleProperty("POM_SCM_CONNECTION"))
            developerConnection.set(providers.gradleProperty("POM_SCM_DEV_CONNECTION"))
            url.set(providers.gradleProperty("POM_SCM_URL"))
        }
    }
}

extensions.configure<PublishingExtension> {
    repositories.maven {
        name = "DryRun"
        url = uri(layout.buildDirectory.dir("dry-run-repo"))
    }
}
