package com.base.app.catalog.media.photos

import com.base.app.core.image.MediaCallFactory

/**
 * The one client the catalog's photos and clips load through, as an app without the networking
 * module has. Its User-Agent names the catalog and a contact, as media hosts ask of automated clients,
 * so the perf runs' traffic can be told apart; a generated project's catalog sends its own name.
 */
internal val CatalogMediaClient = MediaCallFactory(userAgent = "BaseAppCatalog (https://android-base.vercel.app; sample media scenes)")
