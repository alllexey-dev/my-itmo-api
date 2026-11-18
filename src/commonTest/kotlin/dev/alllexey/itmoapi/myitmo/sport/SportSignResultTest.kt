package dev.alllexey.itmoapi.myitmo.sport

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.testing.fixture
import dev.alllexey.itmoapi.testing.mockExchange
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.*

class SportSignResultTest {
    private suspend fun signIn(response: String, status: Int = 200, failure: IOException? = null): SportSignResult =
        mockExchange(
            "/api/sport/sign/schedule/lessons", response, method = HttpMethod.Post, body = "[500001]", status = status,
            inspect = { if (failure != null) throw failure },
        ) { SportApiImpl(it).signInLesson(500001) }

    @Test
    fun enrolledLessonIsBooked() = runTest {
        assertSame(SportSignResult.Booked, signIn(fixture("sport/lessons.json")))
    }

    @Test
    fun legacyCapacityRefusalWithContextIsFull() = runTest {
        assertSame(SportSignResult.Full, signIn(fixture("sport/sign-in-full.json")))
    }

    @Test
    fun capacitySignatureWithoutContextAndOtherReasonsAreRejected() = runTest {
        val bare = """{"error_code":137,"error_message":"нельзя записать студента: нет свободных мест на занятии","result":null}"""
        for (response in listOf(fixture("sport/sign-in-error.json"), bare)) {
            val rejected = assertIs<SportSignResult.Rejected>(signIn(response))
            assertEquals(137, rejected.refusal.errorCode)
            assertEquals(200, rejected.refusal.status)
        }
        val reasons = assertIs<SportSignResult.Rejected>(signIn(fixture("sport/sign-in-error.json"))).refusal.serverMessage
        assertEquals("[cannot enroll student: [daily limit], [overlapping enrollment], [weekly limit]]", reasons)
    }

    @Test
    fun noDecisionAboutTheLessonIsRetriedLater() = runTest {
        val missingResult = """{"error_code":0,"error_message":null,"result":null}"""
        val sessionCode = """{"error_code":401,"error_message":"synthetic","result":null}"""
        val cases = listOf(
            Triple(fixture("sport/sign-in-error.json"), 400, MyItmoException.Api::class),
            Triple(fixture("sport/sign-in-full.json"), 400, MyItmoException.Api::class),
            Triple(sessionCode, 200, MyItmoException.Api::class),
            Triple("<html>Bad Gateway</html>", 502, MyItmoException.Http::class),
            Triple("{}", 401, MyItmoException.Auth::class),
            Triple(missingResult, 200, MyItmoException.Decode::class),
        )
        for ((response, status, cause) in cases) {
            val retry = assertIs<SportSignResult.RetryLater>(signIn(response, status))
            assertTrue(cause.isInstance(retry.cause), "Unexpected cause for status $status")
        }
        val offline = assertIs<SportSignResult.RetryLater>(signIn("{}", failure = IOException("Synthetic engine failure")))
        assertIs<MyItmoException.Network>(offline.cause)
    }
}
