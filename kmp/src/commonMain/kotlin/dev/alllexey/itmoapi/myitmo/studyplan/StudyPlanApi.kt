package dev.alllexey.itmoapi.myitmo.studyplan

import dev.alllexey.itmoapi.core.ItmoTransport

/** Typed MyITMO studyplan operations; endpoint members are added by the owning area card. */
public interface StudyPlanApi

internal class StudyPlanApiImpl(private val transport: ItmoTransport) : StudyPlanApi
