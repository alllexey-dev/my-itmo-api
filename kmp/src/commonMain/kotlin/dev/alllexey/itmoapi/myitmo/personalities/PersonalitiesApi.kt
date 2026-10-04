package dev.alllexey.itmoapi.myitmo.personalities

import dev.alllexey.itmoapi.core.ItmoTransport

/** Typed MyITMO personalities operations; endpoint members are added by the owning area card. */
public interface PersonalitiesApi

internal class PersonalitiesApiImpl(private val transport: ItmoTransport) : PersonalitiesApi
