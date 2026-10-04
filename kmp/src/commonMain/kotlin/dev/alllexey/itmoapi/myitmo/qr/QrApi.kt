package dev.alllexey.itmoapi.myitmo.qr

import dev.alllexey.itmoapi.core.ItmoTransport

/** Typed MyITMO qr operations; endpoint members are added by the owning area card. */
public interface QrApi

internal class QrApiImpl(private val transport: ItmoTransport) : QrApi
