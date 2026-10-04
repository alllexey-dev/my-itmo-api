package dev.alllexey.itmoapi.myitmo.requests

import dev.alllexey.itmoapi.core.ItmoTransport

/** Typed MyITMO requests operations; endpoint members are added by the owning area card. */
public interface RequestsApi

internal class RequestsApiImpl(private val transport: ItmoTransport) : RequestsApi
