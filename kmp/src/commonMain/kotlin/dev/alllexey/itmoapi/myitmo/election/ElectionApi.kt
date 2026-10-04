package dev.alllexey.itmoapi.myitmo.election

import dev.alllexey.itmoapi.core.ItmoTransport

/** Typed MyITMO election operations; endpoint members are added by the owning area card. */
public interface ElectionApi

internal class ElectionApiImpl(private val transport: ItmoTransport) : ElectionApi
