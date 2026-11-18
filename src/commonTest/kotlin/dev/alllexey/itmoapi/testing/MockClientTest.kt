package dev.alllexey.itmoapi.testing

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MockClientTest {
    @Test
    fun harnessRejectsWrongMethod() = runTest {
        val mock = mockClient { expect(HttpMethod.Post, "/expected") }
        try {
            assertFailsWith<AssertionError> { mock.client.get("https://example.invalid/expected") }
        } finally {
            mock.close()
        }
    }

    @Test
    fun harnessRejectsWrongPath() = runTest {
        val mock = mockClient { expect(HttpMethod.Get, "/expected") }
        try {
            assertFailsWith<AssertionError> { mock.client.get("https://example.invalid/wrong") }
        } finally {
            mock.close()
        }
    }

    @Test
    fun harnessRejectsMissingRepeatedQueryValue() = runTest {
        val mock = mockClient { expect(HttpMethod.Get, "/expected") { query("id", "1", "2") } }
        try {
            assertFailsWith<AssertionError> { mock.client.get("https://example.invalid/expected") { parameter("id", "1") } }
        } finally {
            mock.close()
        }
    }

    @Test
    fun harnessRejectsWrongHeaderWithoutRenderingIt() = runTest {
        val mock = mockClient { expect(HttpMethod.Get, "/expected") { header("Accept-Language", "ru") } }
        try {
            assertFailsWith<AssertionError> { mock.client.get("https://example.invalid/expected") { header("Accept-Language", "en") } }
        } finally {
            mock.close()
        }
    }

    @Test
    fun harnessRejectsWrongBodyWithoutRenderingIt() = runTest {
        val mock = mockClient { expect(HttpMethod.Post, "/expected") { body("expected") } }
        try {
            assertFailsWith<AssertionError> { mock.client.post("https://example.invalid/expected") { setBody("wrong") } }
        } finally {
            mock.close()
        }
    }

    @Test
    fun harnessRejectsUnconsumedExchange() {
        val mock = mockClient { expect(HttpMethod.Get, "/expected") }
        try {
            assertFailsWith<AssertionError> { mock.assertComplete() }
        } finally {
            mock.close()
        }
    }
}
