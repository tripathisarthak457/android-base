package com.example.everything.catalog.media.photos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.example.everything.catalog.catalogContentPadding
import com.example.everything.core.designsystem.component.container.AppScaffold
import com.example.everything.core.designsystem.component.navigation.AppTopBar
import com.example.everything.core.designsystem.component.text.AppText
import com.example.everything.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.everything.core.designsystem.theme.AppTheme
import com.example.everything.core.image.AppNetworkImage
import com.example.everything.core.image.PrefetchImages

/**
 * A shop's grid of product photos loaded over the network: two columns on a phone, more as the window
 * widens. Each photo is requested at a CDN's thumbnail size, larger than its slot, so the decoder scales
 * it down to the slot as it would for a real catalogue. Opened with `--es section Shop`.
 */
@Composable
internal fun ShopGridScreen(onBack: (() -> Unit)?) {
    val grid = rememberLazyGridState()
    AppScaffold(topBar = { AppTopBar(title = "Shop grid", onBack = onBack, scrolled = { grid.scrolledFromTop() }) }) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(AppTheme.layout.gridMinTileWidth),
            state = grid,
            modifier = Modifier
                .fillMaxSize()
                .testTag("media_shop"),
            contentPadding = catalogContentPadding(PaddingValues(AppTheme.spacing.gutter)),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
            flingBehavior = AppScrollDefaults.flingBehavior(),
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                AppText(text = ShopHeader, style = AppTheme.typography.subheadline, color = AppTheme.colors.contentSecondary)
            }
            items(Products, key = { "product-${it.index}" }, contentType = { "product" }) { product -> ProductTile(product) }
            item(key = "credits", span = { GridItemSpan(maxLineSpan) }, contentType = "credits") { Credits(ShopCredits) }
        }
    }
    // The first item is the header, so product n is item n + 1.
    PrefetchImages(grid) { index -> Products.getOrNull(index - 1)?.photoUrl }
}

@Composable
private fun ProductTile(product: Product) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs)) {
        AppNetworkImage(
            url = product.photoUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PHOTO_RATIO)
                .clip(AppTheme.shapes.md),
        )
        AppText(text = product.title, style = AppTheme.typography.subheadline, maxLines = 1, overflow = TextOverflow.Ellipsis)
        AppText(text = product.price, style = AppTheme.typography.headline)
        AppText(text = product.rating, style = AppTheme.typography.caption1, color = AppTheme.colors.contentTertiary)
    }
}

/** How far the grid has scrolled from its top, as `LazyListState.scrolledFromTop` tells a list's top bar. */
private fun LazyGridState.scrolledFromTop(): Int {
    val first = layoutInfo.visibleItemsInfo.firstOrNull() ?: return 0
    return if (first.index == 0) (-first.offset.y).coerceAtLeast(0) else Int.MAX_VALUE
}

/** One product, its strings formatted before the grid draws it. */
@Immutable
private class Product(val index: Int, val title: String, val price: String, val rating: String, val photoUrl: String)

private val ProductNames = listOf(
    "Linen shirt", "Desk lamp", "Ceramic mug", "Wool scarf", "Travel bag", "Plant pot",
    "Notebook", "Water bottle", "Canvas tote", "Throw blanket", "Candle", "Picture frame",
)

/** The sample photos in the other order from the feed, so the first screen of each is different. */
private val Products: List<Product> = SamplePhotos.asReversed().take(PRODUCT_COUNT).mapIndexed { index, photo ->
    val cents = (photo.id * 731 + index * 97) % 15_000 + 900
    Product(
        index = index,
        title = ProductNames[index % ProductNames.size],
        price = "$${cents / 100}.${(cents % 100).toString().padStart(2, '0')}",
        rating = "${4 + (index % 10) / 10f} (${(photo.id * 13) % 900 + 20})",
        photoUrl = "https://picsum.photos/id/${photo.id}/600/750.${if (photo.id % 2 == 0) "webp" else "jpg"}",
    )
}

private const val ShopHeader = "120 products"
private const val ShopCredits = "Photos from Unsplash, through Lorem Picsum"
private const val PRODUCT_COUNT = 120
private const val PHOTO_RATIO = 4f / 5f
