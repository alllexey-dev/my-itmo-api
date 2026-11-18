package dev.alllexey.itmoapi.blocking

import dev.alllexey.itmoapi.MyItmo
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.itmoid.InMemoryTokenStorage
import dev.alllexey.itmoapi.itmoid.TokenStorage
import dev.alllexey.itmoapi.myitmo.MyItmoConfiguration
import kotlinx.coroutines.runBlocking

/** [MyItmo] for Java and other callers without coroutines: the same areas and methods, but every call
 * waits for the result on the calling thread and returns it or throws [MyItmoException].
 *
 * ```java
 * try (BlockingMyItmo itmo = BlockingMyItmo.login("id", "password")) {
 *     List<Schedule> week = itmo.getSchedule().getPersonalSchedule(from, to);
 * }
 * ```
 *
 * Do not call from an Android main thread: use the suspend API of [MyItmo] there. [itmo] is the wrapped
 * object for anything this view does not cover.
 */
public class BlockingMyItmo private constructor(public val itmo: MyItmo) : AutoCloseable {
    public val schedule: BlockingSchedule = BlockingSchedule(itmo.schedule)
    public val recordBook: BlockingRecordBook = BlockingRecordBook(itmo.recordBook)
    public val personalities: BlockingPersonalities = BlockingPersonalities(itmo.personalities)
    public val studyPlan: BlockingStudyPlan = BlockingStudyPlan(itmo.studyPlan)
    public val qr: BlockingQr = BlockingQr(itmo.qr)
    public val sport: BlockingSport = BlockingSport(itmo.sport)
    public val election: BlockingElection = BlockingElection(itmo.election)
    public val finances: BlockingFinances = BlockingFinances(itmo.finances)
    public val dormitory: BlockingDormitory = BlockingDormitory(itmo.dormitory)
    public val requests: BlockingRequests = BlockingRequests(itmo.requests)
    public val system: BlockingSystem = BlockingSystem(itmo.system)

    /** Blocking view of [MyItmo.bars]; the BARS client is created on first use. */
    public val bars: BlockingBars by lazy { BlockingBars(itmo.bars) }

    /** Blocking [MyItmo.hasSession]. */
    @Throws(MyItmoException::class)
    public fun hasSession(): Boolean = runBlocking { itmo.hasSession() }

    /** Blocking [MyItmo.logout]. */
    @Throws(MyItmoException::class)
    public fun logout(): Unit = runBlocking { itmo.logout() }

    /** Releases HTTP clients and the engine, as [MyItmo.close] does. */
    override fun close(): Unit = itmo.close()

    override fun toString(): String = "BlockingMyItmo(redacted)"

    public companion object {
        /** Blocking [MyItmo.login]. */
        @JvmStatic
        @JvmOverloads
        @Throws(MyItmoException::class)
        public fun login(
            username: String,
            password: String,
            storage: TokenStorage = InMemoryTokenStorage(),
            configuration: MyItmoConfiguration = MyItmoConfiguration.DEFAULT,
        ): BlockingMyItmo = BlockingMyItmo(runBlocking { MyItmo.login(username, password, storage, configuration) })

        /** Blocking [MyItmo.loginOrResume]. */
        @JvmStatic
        @JvmOverloads
        @Throws(MyItmoException::class)
        public fun loginOrResume(
            username: String,
            password: String,
            storage: TokenStorage,
            configuration: MyItmoConfiguration = MyItmoConfiguration.DEFAULT,
        ): BlockingMyItmo = BlockingMyItmo(runBlocking { MyItmo.loginOrResume(username, password, storage, configuration) })

        /** Blocking [MyItmo.fromRefreshToken]. */
        @JvmStatic
        @JvmOverloads
        @Throws(MyItmoException::class)
        public fun fromRefreshToken(
            refreshToken: String,
            storage: TokenStorage = InMemoryTokenStorage(),
            configuration: MyItmoConfiguration = MyItmoConfiguration.DEFAULT,
        ): BlockingMyItmo = BlockingMyItmo(runBlocking { MyItmo.fromRefreshToken(refreshToken, storage, configuration) })

        /** [MyItmo.fromStorage]: uses the stored session without a network call. */
        @JvmStatic
        @JvmOverloads
        public fun fromStorage(
            storage: TokenStorage,
            configuration: MyItmoConfiguration = MyItmoConfiguration.DEFAULT,
        ): BlockingMyItmo = BlockingMyItmo(MyItmo.fromStorage(storage, configuration))

        /** A blocking view of an existing [MyItmo]; closing either closes both. */
        @JvmStatic
        public fun of(itmo: MyItmo): BlockingMyItmo = BlockingMyItmo(itmo)
    }
}
