package ua.retrogaming.gcac.helper

fun StringBuilder.linesFromBuffer(): List<String> {
    val text = this.toString()
    val parts = text.split("\n")
    if (parts.isEmpty()) return emptyList()

    // keep the last (possibly incomplete) line in the buffer
    this.clear()
    this.append(parts.last())

    // return all full lines
    return parts.dropLast(1)
}