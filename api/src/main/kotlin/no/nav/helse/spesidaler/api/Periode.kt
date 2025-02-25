package no.nav.helse.spesidaler.api

import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import java.time.Year
import java.time.format.DateTimeFormatter

class Periode(fom: LocalDate, tom: LocalDate) : ClosedRange<LocalDate>, Iterable<LocalDate>  {
    override val start: LocalDate = fom
    override val endInclusive: LocalDate = tom

    init {
        require(start <= endInclusive) { "fom ($start) kan ikke være etter tom ($endInclusive)" }
    }

    internal companion object {
        private val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        internal fun Iterable<Periode>.trim(other: Periode): List<Periode> =
            fold(listOf(other)) { result, trimperiode ->
                result.dropLast(1) + (result.lastOrNull()?.trim(trimperiode) ?: emptyList())
            }
    }

    internal fun overlapperMed(other: Periode) = overlappendePeriode(other) != null

    internal fun overlappendePeriode(other: Periode): Periode? {
        val start = maxOf(this.start, other.start)
        val slutt = minOf(this.endInclusive, other.endInclusive)
        if (start > slutt) return null
        return start til slutt
    }

    override fun toString(): String {
        return start.format(formatter) + " til " + endInclusive.format(formatter)
    }

    private fun trim(other: Periode): List<Periode> {
        val felles = this.overlappendePeriode(other) ?: return listOf(this)
        return when {
            felles == this -> emptyList()
            felles.start == this.start -> listOf(this.beholdDagerEtter(felles))
            felles.endInclusive == this.endInclusive -> listOf(this.beholdDagerFør(felles))
            else -> listOf(this.beholdDagerFør(felles), this.beholdDagerEtter(felles))
        }
    }

    private fun beholdDagerFør(other: Periode) = this.start til other.start.minusDays(1)

    private fun beholdDagerEtter(other: Periode) = other.endInclusive.plusDays(1) til this.endInclusive

    override fun equals(other: Any?): Boolean {
        if (other !is Periode) return false
        if (this === other) return true
        return this.start == other.start && this.endInclusive == other.endInclusive
    }

    override fun hashCode() = start.hashCode() + endInclusive.hashCode()

    override operator fun iterator() = object : Iterator<LocalDate> {
        private var currentDate: LocalDate = start

        override fun hasNext() = endInclusive >= currentDate

        override fun next() =
            currentDate.also { currentDate = it.plusDays(1) }
    }

    // Antall virkedager i perioden
    internal val virkedager get(): Int = run {
        val fom = this.start
        val tom = this.endInclusive.plusDays(1)
        val epochStart = fom.toEpochDay()
        val epochEnd = tom.toEpochDay()
        if (epochStart >= epochEnd) return 0
        val dagerMellom = (epochEnd - epochStart).toInt()
        val heleHelger = (dagerMellom + fom.dayOfWeek.value - 1) / 7 * 2
        val justerFørsteHelg = if (fom.dayOfWeek == SUNDAY) 1 else 0
        val justerSisteHelg = if (tom.dayOfWeek == SUNDAY) 1 else 0
        return dagerMellom - heleHelger + justerFørsteHelg - justerSisteHelg
    }
}

infix fun LocalDate.til(tom: LocalDate) = Periode(this, tom)
internal val Year.virkedager get() = (LocalDate.of(this.value, 1,1) til LocalDate.of(this.value, 12, 31)).virkedager
