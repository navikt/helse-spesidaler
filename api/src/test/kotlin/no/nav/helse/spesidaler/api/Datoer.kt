package no.nav.helse.spesidaler.api

import java.time.LocalDate
import java.time.Month

private fun Int.måned(måned: Month, år: Int) = LocalDate.of(år, måned, this)
internal fun Int.januar(år: Int) = måned(Month.JANUARY, år)
internal fun Int.desember(år: Int) = måned(Month.DECEMBER, år)
internal val Int.januar get() = januar(2018)
