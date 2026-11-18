package dev.alllexey.itmoapi.blocking

import dev.alllexey.itmoapi.core.CountWrapper
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.myitmo.personalities.*
import kotlinx.coroutines.runBlocking

/** Blocking view of [PersonalitiesApi] for Java: each method waits for the result on the calling thread.
 * Do not call from an Android main thread.
 * Mirrors the interface method by method; `BlockingParityTest` fails when the two drift apart.
 */
public class BlockingPersonalities internal constructor(private val api: PersonalitiesApi) {
    /** Blocking [PersonalitiesApi.getPersonality]. */
    @Throws(MyItmoException::class)
    public fun getPersonality(isu: Long): Personality = runBlocking { api.getPersonality(isu) }

    /** Blocking [PersonalitiesApi.searchPersonalities]. */
    @Throws(MyItmoException::class)
    public fun searchPersonalities(limit: Int, offset: Int, query: String): CountWrapper<List<PersonalityMin>> =
        runBlocking { api.searchPersonalities(limit, offset, query) }
}
