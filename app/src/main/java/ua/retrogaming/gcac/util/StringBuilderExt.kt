package ua.retrogaming.gcac.util

fun StringBuilder.linesFromBuffer(): List<String> {
    val text = this.toString()
    val lines = text.split("\n")
    if (lines.size < 2) return emptyList()

    // keep the last (possibly incomplete) line in the buffer
    this.setLength(0)
    this.append(lines.last())

    // return all full lines (dropping the last one)
    return lines.dropLast(1)
}