package com.example.videoalone.catalog.media.photos

import com.example.videoalone.core.image.MediaCallFactory

/**
 * The one client the catalog's photos and clips load through, as an app without the networking
 * module has. It names the catalog to the sample hosts, which ask automated clients for a name and a
 * contact; a generated project's catalog sends its own name.
 */
internal val CatalogMediaClient = MediaCallFactory(userAgent = "VideoAloneCatalog (https://android-base.vercel.app; sample media scenes)")
