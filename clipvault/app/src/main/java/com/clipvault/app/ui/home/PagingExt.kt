package com.clipvault.app.ui.home

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.runtime.Composable
import androidx.paging.compose.LazyPagingItems

fun <T : Any> LazyStaggeredGridScope.pagingItems(
    items: LazyPagingItems<T>,
    key: ((item: T) -> Any)? = null,
    span: ((item: T) -> StaggeredGridItemSpan)? = null,
    itemContent: @Composable LazyStaggeredGridItemScope.(value: T?) -> Unit
) {
    items(
        count = items.itemCount,
        key = if (key != null) { index ->
            val item = items.peek(index) // use peek to avoid triggering load
            if (item != null) key(item) else index
        } else null,
        span = if (span != null) { index ->
            val item = items.peek(index)
            if (item != null) span(item) else StaggeredGridItemSpan.SingleLane
        } else null
    ) { index ->
        val item = items[index]
        itemContent(item)
    }
}
