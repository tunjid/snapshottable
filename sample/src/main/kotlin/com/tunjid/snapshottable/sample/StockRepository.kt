package com.tunjid.snapshottable.sample

import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val STOCKS_PER_SECTOR = 60

private val ADJECTIVES_BY_SECTOR: Map<Sector, List<String>> = mapOf(
    Sector.TECH to listOf(
        "Quantum", "Hyper", "Neural", "Cyber", "Nimbus", "Vertex",
        "Pixel", "Lumen", "Synth", "Orbit", "Fusion", "Echo",
    ),
    Sector.FINANCE to listOf(
        "Sterling", "Apex", "Summit", "Granite", "Meridian", "Beacon",
        "Cardinal", "Liberty", "Anchor", "Pinnacle", "Vanguard", "Crown",
    ),
    Sector.AGRICULTURE to listOf(
        "Harvest", "Golden", "Verdant", "Prairie", "Sunny", "Evergreen",
        "Meadow", "Heartland", "Orchard", "Fertile", "Amber", "Willow",
    ),
    Sector.ENERGY to listOf(
        "Helios", "Volt", "Ember", "Tidal", "Aurora", "Magma",
        "Photon", "Cinder", "Gale", "Ignite", "Pulse", "Radiant",
    ),
    Sector.HEALTHCARE to listOf(
        "Vital", "Helix", "Pulse", "Mend", "Sera", "Thrive",
        "Lifeline", "Bionic", "Pristine", "Restore", "Clarity", "Haven",
    ),
)

private val NOUNS_BY_SECTOR: Map<Sector, List<String>> = mapOf(
    Sector.TECH to listOf("Labs", "Systems", "AI", "Dynamics", "Works", "Cloud", "Logic", "Forge"),
    Sector.FINANCE to listOf("Capital", "Holdings", "Partners", "Trust", "Securities", "Group", "Equity", "Bancorp"),
    Sector.AGRICULTURE to listOf("Farms", "Growers", "Agronomy", "Harvesters", "Fields", "Organics", "Produce", "Ranch"),
    Sector.ENERGY to listOf("Power", "Energy", "Grid", "Renewables", "Reactor", "Turbines", "Solar", "Fuels"),
    Sector.HEALTHCARE to listOf("Health", "Biotech", "Therapeutics", "Pharma", "Diagnostics", "Care", "Medical", "Genomics"),
)

private data class Company(val ticker: String, val name: String)

class StockRepository {

    private val companiesBySector: Map<Sector, List<Company>> =
        Sector.entries.associateWith { sector ->
            val adjectives = ADJECTIVES_BY_SECTOR.getValue(sector)
            val nouns = NOUNS_BY_SECTOR.getValue(sector)
            buildList {
                var index = 0
                while (size < STOCKS_PER_SECTOR) {
                    val adjective = adjectives[index % adjectives.size]
                    val noun = nouns[(index / adjectives.size) % nouns.size]
                    val ticker = buildString {
                        append(adjective.take(2).uppercase())
                        append(noun.take(1).uppercase())
                        append(index + 1)
                    }
                    add(Company(ticker = ticker, name = "$adjective $noun"))
                    index++
                }
            }
        }

    private fun seedPrice(ticker: String): Double =
        50.0 + ticker.hashCode().mod(450)

    fun stocks(sector: Sector): Flow<List<Stock>> = flow {
        val companies = companiesBySector.getValue(sector)
        var current = companies.map {
            Stock(ticker = it.ticker, name = it.name, price = seedPrice(it.ticker))
        }
        emit(current)
        while (true) {
            delay(3_000)
            current = current.map { stock ->
                val drift = (Random.nextDouble() - 0.5) * 8.0
                stock.copy(price = (stock.price + drift).coerceAtLeast(1.0))
            }
            emit(current)
        }
    }
}
