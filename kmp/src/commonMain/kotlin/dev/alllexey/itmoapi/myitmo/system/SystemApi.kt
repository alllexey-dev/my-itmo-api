package dev.alllexey.itmoapi.myitmo.system

import dev.alllexey.itmoapi.core.ItmoTransport

/** Typed MyITMO system operations; endpoint members are added by the owning area card. */
public interface SystemApi

internal class SystemApiImpl(private val transport: ItmoTransport) : SystemApi
