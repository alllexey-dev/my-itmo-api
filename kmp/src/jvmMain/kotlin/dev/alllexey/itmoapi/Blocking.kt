package dev.alllexey.itmoapi

import dev.alllexey.itmoapi.core.MyItmoException
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

/** Calls any suspend function of this library from Java and waits for the result:
 * `DataResponse<List<Schedule>> week = Blocking.call(c -> itmo.getSchedule().getPersonalSchedule(from, to, c));`
 * Pass the lambda's continuation as the last argument and assign the result to a typed variable
 * (Java infers the type from it). Blocks the calling thread: not for an Android main thread.
 */
public object Blocking {
    /** A Java lambda that forwards its continuation to one suspend call. */
    public fun interface Call<T> {
        @Throws(Exception::class)
        public fun invoke(continuation: Continuation<T>): Any?
    }

    /** Runs [call] to completion on the current thread and returns its result or rethrows its exception. */
    @JvmStatic
    @Throws(MyItmoException::class)
    public fun <T> call(call: Call<T>): T = runBlocking {
        suspendCoroutineUninterceptedOrReturn { continuation -> call.invoke(continuation) }
    }
}
