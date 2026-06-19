package com.tunjid.snapshottable.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun Screen(
    state: State,
    mode: Mode,
    toggleSector: (Sector) -> Unit,
    toggleMode: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock Ticker") },
                actions = {
                    Button(
                        onClick = toggleMode,
                        modifier = Modifier
                            .padding(16.dp)
                            .semantics { contentDescription = "toggleModeButton" },
                    ) { Text("Mode: ${mode.name}") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val selected = state.selectedSectors
            val pagerState = rememberPagerState(pageCount = { selected.size })

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Sector.entries.forEach { sector ->
                    FilterChip(
                        selected = sector in selected,
                        onClick = { toggleSector(sector) },
                        label = { Text(sector.name) },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .semantics {
                                contentDescription = "toggle_${sector.name}"
                            },
                    )
                }
            }

            if (selected.isEmpty()) {
                Text(
                    text = "Select a sector to see its stocks",
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                val scope = rememberCoroutineScope()
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage
                        .coerceIn(0, selected.lastIndex),
                ) {
                    selected.forEachIndexed { index, sector ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(sector.name) },
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = "sectorPager" },
                ) { page ->
                    val sector = selected[page]
                    val stockState = state.stockStates
                        .firstOrNull { it.sector == sector }
                    SectorPage(
                        sector = sector,
                        stocks = stockState?.stocks ?: emptyList(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectorPage(
    sector: Sector,
    stocks: List<Stock>,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "stockList_${sector.name}" },
    ) {
        items(stocks, key = { it.ticker }) { stock ->
            StockRow(stock)
        }
    }
}

@Composable
private fun StockRow(stock: Stock) {
    val prices = remember {
        mutableStateListOf<Double>()
    }
    DisposableEffect(stock.price) {
        prices.add(stock.price)
        if (prices.size > 2) prices.removeRange(2, prices.lastIndex)
        onDispose { }
    }
    val previousPrice = when {
        prices.size < 2 -> null
        else -> prices.firstOrNull()
    }
    val (color, arrow) = when {
        previousPrice == null -> Color.Unspecified to "—"
        stock.price > previousPrice -> Color(0xFF1B8E3C) to "▲"
        stock.price < previousPrice -> Color(0xFFC62828) to "▼"
        else -> Color.Unspecified to "—"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stock.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stock.ticker,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "$arrow  $${"%.2f".format(stock.price)}",
                color = color,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
