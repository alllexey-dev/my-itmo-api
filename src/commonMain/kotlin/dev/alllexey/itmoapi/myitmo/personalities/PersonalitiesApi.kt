package dev.alllexey.itmoapi.myitmo.personalities

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.CountWrapper
import dev.alllexey.itmoapi.core.MyItmoException
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.builtins.ListSerializer

/** Public MyITMO profiles; language and authentication come from the owning client's configuration. */
public interface PersonalitiesApi {
    /** GET /api/personalities/persons/{isu}; successful result.isu matches the numeric ISU.
     * An observed unknown person returns HTTP 400, error_code 100 and result null, mapped to
     * MyItmoException.Api with status 400 and errorCode 100, not a successful null profile.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getPersonality(isu: Long): Personality

    /** GET /api/personalities/persons?limit&offset&q; limit is page size, offset is item offset.
     * q searches names, ISUs and server-supported attributes; count is the total result count.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun searchPersonalities(
        limit: Int,
        offset: Int,
        query: String,
    ): CountWrapper<List<PersonalityMin>>
}

internal class PersonalitiesApiImpl(private val transport: ItmoTransport) : PersonalitiesApi {
    override suspend fun getPersonality(isu: Long): Personality = transport.result(
        Personality.serializer(), HttpMethod.Get, "api/personalities/persons/$isu",
    )

    override suspend fun searchPersonalities(
        limit: Int,
        offset: Int,
        query: String,
    ): CountWrapper<List<PersonalityMin>> = transport.result(
        CountWrapper.serializer(ListSerializer(PersonalityMin.serializer())),
        HttpMethod.Get,
        "api/personalities/persons",
    ) {
        parameter("limit", limit)
        parameter("offset", offset)
        parameter("q", query)
    }
}
