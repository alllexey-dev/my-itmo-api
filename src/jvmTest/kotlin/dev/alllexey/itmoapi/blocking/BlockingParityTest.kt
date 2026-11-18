package dev.alllexey.itmoapi.blocking

import dev.alllexey.itmoapi.MyItmo
import dev.alllexey.itmoapi.bars.BarsClient
import dev.alllexey.itmoapi.myitmo.dormitory.DormitoryApi
import dev.alllexey.itmoapi.myitmo.election.ElectionApi
import dev.alllexey.itmoapi.myitmo.finances.FinancesApi
import dev.alllexey.itmoapi.myitmo.personalities.PersonalitiesApi
import dev.alllexey.itmoapi.myitmo.qr.QrApi
import dev.alllexey.itmoapi.myitmo.recordbook.RecordBookApi
import dev.alllexey.itmoapi.myitmo.requests.RequestsApi
import dev.alllexey.itmoapi.myitmo.schedule.ScheduleApi
import dev.alllexey.itmoapi.myitmo.sport.SportApi
import dev.alllexey.itmoapi.myitmo.studyplan.StudyPlanApi
import dev.alllexey.itmoapi.myitmo.system.SystemApi
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.coroutines.Continuation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The blocking classes are written by hand: this keeps them in step with the suspend API. */
class BlockingParityTest {
    private val pairs: List<Pair<Class<*>, Class<*>>> = listOf(
        ScheduleApi::class.java to BlockingSchedule::class.java,
        RecordBookApi::class.java to BlockingRecordBook::class.java,
        PersonalitiesApi::class.java to BlockingPersonalities::class.java,
        StudyPlanApi::class.java to BlockingStudyPlan::class.java,
        QrApi::class.java to BlockingQr::class.java,
        SportApi::class.java to BlockingSport::class.java,
        ElectionApi::class.java to BlockingElection::class.java,
        FinancesApi::class.java to BlockingFinances::class.java,
        DormitoryApi::class.java to BlockingDormitory::class.java,
        RequestsApi::class.java to BlockingRequests::class.java,
        SystemApi::class.java to BlockingSystem::class.java,
        BarsClient::class.java to BlockingBars::class.java,
    )

    private fun visible(method: Method): Boolean =
        Modifier.isPublic(method.modifiers) && !Modifier.isStatic(method.modifiers) && !method.isSynthetic && '$' !in method.name

    /** The blocking twin takes `java.time.LocalDate` and a Java `Function` where the suspend API takes Kotlin types. */
    private fun twin(type: Class<*>): Class<*> = when (type) {
        kotlinx.datetime.LocalDate::class.java -> java.time.LocalDate::class.java
        Function2::class.java -> java.util.function.Function::class.java
        else -> type
    }

    @Test
    fun everySuspendMethodHasABlockingTwinWithTheSameParameters() {
        var checked = 0
        for ((api, blocking) in pairs) {
            val suspending = api.declaredMethods
                .filter { visible(it) && it.parameterTypes.lastOrNull() == Continuation::class.java }
                .associate { it.name to it.parameterTypes.dropLast(1).map(::twin) }
            // @JvmOverloads adds shorter overloads; the longest one is the full signature.
            val blockingMethods = blocking.declaredMethods.filter(::visible).groupBy { it.name }
                .mapValues { (_, overloads) -> overloads.maxBy { it.parameterCount }.parameterTypes.toList() }
            assertEquals(suspending.keys.sorted(), blockingMethods.keys.sorted(), "Methods of ${blocking.simpleName}")
            for ((name, parameters) in suspending) {
                assertEquals(parameters, blockingMethods[name], "Parameters of ${blocking.simpleName}.$name")
                checked++
            }
        }
        assertTrue(checked > 60, "Reflection found too few methods: $checked")
    }

    @Test
    fun everyAreaOfMyItmoHasABlockingView() {
        fun areas(type: Class<*>, suffix: String) = type.declaredMethods
            .filter { visible(it) && it.parameterCount == 0 && it.name.startsWith("get") && it.returnType.simpleName.let { name -> name.endsWith(suffix) || name.endsWith("Bars") || name == "BarsClient" } }
            .filter { !it.isAnnotationPresent(java.lang.Deprecated::class.java) }
            .map { it.name }.sorted()
        assertEquals(areas(MyItmo::class.java, "Api"), areas(BlockingMyItmo::class.java, "").filter { it != "getItmo" })
    }
}
