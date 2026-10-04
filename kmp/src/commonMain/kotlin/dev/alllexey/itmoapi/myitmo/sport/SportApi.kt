package dev.alllexey.itmoapi.myitmo.sport

import dev.alllexey.itmoapi.core.ItmoTransport

/** Typed MyITMO sport operations; endpoint members are added by the owning area card. */
public interface SportApi

internal class SportApiImpl(private val transport: ItmoTransport) : SportApi
