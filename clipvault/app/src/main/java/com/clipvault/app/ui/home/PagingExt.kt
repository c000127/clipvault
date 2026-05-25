package com.clipvault.app.ui.home

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.runtime.Composable
import androidx.paging.compose.LazyPagingItems

fun <T : Any> LazyStaggeredGridScope.pagingItems(
    items: LazyPagingItems<T>,
    key: ((item: T) -> Any)? = null,
    itemContent: @Composable LazyStaggeredGridItemScope.(value: T?) -> Unit
) {
    items(count = items.itemCount) { index ->
        val item = items[index]
        itemContent(item)
    }
}
