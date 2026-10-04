package dev.alllexey.itmoapi.myitmo.schedule

import dev.alllexey.itmoapi.core.ItmoTransport

/** Typed MyITMO schedule operations; endpoint members are added by the owning area card. */
public interface ScheduleApi

internal class ScheduleApiImpl(private val transport: ItmoTransport) : ScheduleApi
