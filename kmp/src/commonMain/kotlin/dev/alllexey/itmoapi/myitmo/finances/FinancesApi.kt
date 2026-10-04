package dev.alllexey.itmoapi.myitmo.finances

import dev.alllexey.itmoapi.core.ItmoTransport

/** Typed MyITMO finances operations; endpoint members are added by the owning area card. */
public interface FinancesApi

internal class FinancesApiImpl(private val transport: ItmoTransport) : FinancesApi
