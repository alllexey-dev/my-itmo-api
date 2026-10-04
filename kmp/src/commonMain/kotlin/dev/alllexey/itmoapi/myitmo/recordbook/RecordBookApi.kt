package dev.alllexey.itmoapi.myitmo.recordbook

import dev.alllexey.itmoapi.core.ItmoTransport

/** Typed MyITMO recordbook operations; endpoint members are added by the owning area card. */
public interface RecordBookApi

internal class RecordBookApiImpl(private val transport: ItmoTransport) : RecordBookApi
