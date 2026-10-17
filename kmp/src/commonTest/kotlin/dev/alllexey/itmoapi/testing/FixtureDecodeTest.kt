package dev.alllexey.itmoapi.testing

import dev.alllexey.itmoapi.bars.model.Discipline
import dev.alllexey.itmoapi.bars.model.GroupOrFlow
import dev.alllexey.itmoapi.bars.model.Setting
import dev.alllexey.itmoapi.bars.model.StudentJournal
import dev.alllexey.itmoapi.bars.model.User
import dev.alllexey.itmoapi.core.CountWrapper
import dev.alllexey.itmoapi.core.DataResponse
import dev.alllexey.itmoapi.core.IdValuePair
import dev.alllexey.itmoapi.core.ItmoApiJson
import dev.alllexey.itmoapi.core.ResultResponse
import dev.alllexey.itmoapi.core.SimpleResponse
import dev.alllexey.itmoapi.myitmo.election.AvailableDiscipline
import dev.alllexey.itmoapi.myitmo.election.ChangeResult
import dev.alllexey.itmoapi.myitmo.election.DisciplineSelectionValidation
import dev.alllexey.itmoapi.myitmo.election.ElectionAvailability
import dev.alllexey.itmoapi.myitmo.election.ElectionFlowChain
import dev.alllexey.itmoapi.myitmo.election.FlowLimit
import dev.alllexey.itmoapi.myitmo.finances.ScholarshipTotal
import dev.alllexey.itmoapi.myitmo.personalities.Personality
import dev.alllexey.itmoapi.myitmo.personalities.PersonalityMin
import dev.alllexey.itmoapi.myitmo.qr.QrData
import dev.alllexey.itmoapi.myitmo.recordbook.ControlEntry
import dev.alllexey.itmoapi.myitmo.recordbook.RecordBookEntry
import dev.alllexey.itmoapi.myitmo.recordbook.Specialization
import dev.alllexey.itmoapi.myitmo.requests.RequestSummary
import dev.alllexey.itmoapi.myitmo.schedule.ExtendedTimeSlot
import dev.alllexey.itmoapi.myitmo.schedule.Schedule
import dev.alllexey.itmoapi.myitmo.sport.ChosenSportSection
import dev.alllexey.itmoapi.myitmo.sport.SportAttempts
import dev.alllexey.itmoapi.myitmo.sport.SportDebt
import dev.alllexey.itmoapi.myitmo.sport.SportExternat
import dev.alllexey.itmoapi.myitmo.sport.SportFilters
import dev.alllexey.itmoapi.myitmo.sport.SportHealthLevelResponse
import dev.alllexey.itmoapi.myitmo.sport.SportProject
import dev.alllexey.itmoapi.myitmo.sport.SportSchedule
import dev.alllexey.itmoapi.myitmo.sport.SportScore
import dev.alllexey.itmoapi.myitmo.sport.SportSelection
import dev.alllexey.itmoapi.myitmo.sport.SportSemester
import dev.alllexey.itmoapi.myitmo.sport.SportSemesterOption
import dev.alllexey.itmoapi.myitmo.sport.SportSignLimit
import dev.alllexey.itmoapi.myitmo.sport.TimeSlot
import dev.alllexey.itmoapi.myitmo.studyplan.StudyPlan
import dev.alllexey.itmoapi.myitmo.studyplan.StudyPlanPrograms
import dev.alllexey.itmoapi.myitmo.system.DashboardItem
import dev.alllexey.itmoapi.myitmo.system.MenuResponse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Every endpoint fixture decodes into its 2.x model; codec, error and ITMO.ID fixtures have dedicated tests. */
class FixtureDecodeTest {
    private val untyped = ResultResponse.serializer(JsonElement.serializer().nullable)
    private val models: Map<String, KSerializer<*>> = buildMap {
        put("bars/user.json", User.serializer())
        put("bars/config.json", ListSerializer(Setting.serializer()))
        put("bars/setting.json", Setting.serializer())
        for (name in listOf("disciplines", "empty-disciplines")) put("bars/$name.json", ListSerializer(Discipline.serializer()))
        for (name in listOf("groups", "empty-groups")) put("bars/$name.json", ListSerializer(GroupOrFlow.serializer()))
        for (name in listOf("journal", "empty-journal")) put("bars/$name.json", StudentJournal.serializer())

        put("election/availability.json", ResultResponse.serializer(ElectionAvailability.serializer()))
        for (name in listOf("disciplines", "empty")) {
            put("election/$name.json", ResultResponse.serializer(ListSerializer(AvailableDiscipline.serializer())))
        }
        put("election/validation.json", ResultResponse.serializer(DisciplineSelectionValidation.serializer()))
        for (name in listOf("limits", "empty-limits")) {
            put("election/$name.json", ResultResponse.serializer(MapSerializer(String.serializer(), FlowLimit.serializer())))
        }
        put("election/chains.json", ResultResponse.serializer(ListSerializer(ElectionFlowChain.serializer())))
        put("election/chosen.json", ResultResponse.serializer(ListSerializer(Long.serializer())))
        for (name in listOf("change-flows", "change-disciplines", "clear", "order-object", "order-array", "order-scalar", "error")) {
            put("election/$name.json", untyped)
        }
        put("election/change-absent.json", ResultResponse.serializer(ChangeResult.serializer()))

        for (name in listOf("totals", "empty", "error")) {
            put("finances/$name.json", ResultResponse.serializer(ListSerializer(ScholarshipTotal.serializer())))
        }
        for (name in listOf("person", "student", "employee", "service", "missing-person")) {
            put("personalities/$name.json", ResultResponse.serializer(Personality.serializer()))
        }
        for (name in listOf("search", "search-empty")) {
            put("personalities/$name.json", ResultResponse.serializer(CountWrapper.serializer(ListSerializer(PersonalityMin.serializer()))))
        }
        put("qr/pass.json", SimpleResponse.serializer(QrData.serializer()))

        for (name in listOf("record-book", "absence", "empty")) {
            put("recordbook/$name.json", ResultResponse.serializer(ListSerializer(RecordBookEntry.serializer())))
        }
        put("recordbook/specializations.json", ResultResponse.serializer(ListSerializer(Specialization.serializer())))
        put("recordbook/controls.json", ResultResponse.serializer(ListSerializer(ControlEntry.serializer())))
        for (name in listOf("my", "empty", "error")) {
            put("requests/$name.json", ResultResponse.serializer(ListSerializer(RequestSummary.serializer())))
        }
        for (name in listOf("personal", "teacher", "empty", "error")) {
            put("schedule/$name.json", DataResponse.serializer(ListSerializer(Schedule.serializer())))
        }
        put("schedule/time-slots.json", DataResponse.serializer(ListSerializer(ExtendedTimeSlot.serializer())))

        put("sport/time-slots.json", ResultResponse.serializer(ListSerializer(TimeSlot.serializer())))
        put("sport/filters.json", ResultResponse.serializer(SportFilters.serializer()))
        for (name in listOf("schedule", "external-venue", "wrong-end-date", "empty", "calendar", "calendar-empty")) {
            put("sport/$name.json", ResultResponse.serializer(ListSerializer(SportSchedule.serializer())))
        }
        for (name in listOf("score", "score-empty")) put("sport/$name.json", ResultResponse.serializer(SportScore.serializer()))
        put("sport/attempts.json", ResultResponse.serializer(SportAttempts.serializer()))
        put("sport/semesters.json", ResultResponse.serializer(ListSerializer(SportSemesterOption.serializer())))
        put("sport/current-semester.json", ResultResponse.serializer(SportSemester.serializer()))
        for (name in listOf("limits", "limits-empty")) {
            put("sport/$name.json", ResultResponse.serializer(MapSerializer(Long.serializer(), MapSerializer(Long.serializer(), SportSignLimit.serializer()))))
        }
        put("sport/chosen.json", ResultResponse.serializer(ListSerializer(ChosenSportSection.serializer())))
        for (name in listOf("lessons", "sign-in-error", "sign-in-full", "sign-out-error")) {
            put("sport/$name.json", ResultResponse.serializer(ListSerializer(Long.serializer())))
        }
        for (name in listOf("sport-types", "sport-types-empty")) {
            put("sport/$name.json", ResultResponse.serializer(ListSerializer(IdValuePair.serializer())))
        }
        for (name in listOf("sign-attempts", "sign-attempts-zero", "remaining-error")) {
            put("sport/$name.json", ResultResponse.serializer(Int.serializer()))
        }
        for (name in listOf("debt", "debt-none")) put("sport/$name.json", ResultResponse.serializer(SportDebt.serializer()))
        for (name in listOf("externat", "externat-declined", "externat-none")) {
            put("sport/$name.json", ResultResponse.serializer(SportExternat.serializer()))
        }
        put("sport/health-level.json", ResultResponse.serializer(SportHealthLevelResponse.serializer()))
        for (name in listOf("selections", "selections-empty")) {
            put("sport/$name.json", ResultResponse.serializer(ListSerializer(SportSelection.serializer())))
        }
        for (name in listOf("projects", "projects-empty")) {
            put("sport/$name.json", ResultResponse.serializer(ListSerializer(SportProject.serializer())))
        }

        for (name in listOf("programs", "empty-programs")) put("studyplan/$name.json", ResultResponse.serializer(StudyPlanPrograms.serializer()))
        for (name in listOf("study-plan", "absence")) put("studyplan/$name.json", ResultResponse.serializer(StudyPlan.serializer()))
        for (name in listOf("dashboard", "empty-dashboard")) {
            put("system/$name.json", ResultResponse.serializer(ListSerializer(DashboardItem.serializer())))
        }
        for (name in listOf("items", "services", "empty-menu", "error")) put("system/$name.json", ResultResponse.serializer(MenuResponse.serializer()))
    }

    @Test
    fun everyEndpointFixtureHasAModel() {
        val dedicated = setOf("core", "errors", "itmoid")
        val endpointFixtures = fixturePaths().filter { it.substringBefore('/') !in dedicated }.toSet()
        assertEquals(endpointFixtures, models.keys)
    }

    @Test
    fun everyEndpointFixtureDecodesIntoItsModel() {
        for ((path, serializer) in models) {
            val decoded = runCatching { ItmoApiJson.decodeFromString(serializer, fixture(path)) }
            assertTrue(decoded.isSuccess, "Fixture does not decode: $path")
        }
    }
}
