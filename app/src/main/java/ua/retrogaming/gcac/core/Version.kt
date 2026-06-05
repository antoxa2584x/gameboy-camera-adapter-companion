package ua.retrogaming.gcac.core

/** Dotted-version comparison shared by update checks and feature gates. */
object Version {

    /**
     * Compares "1.2.10"-style versions; positive when [a] > [b], zero when
     * equal. Splits on dots and hyphens to support "1.2.3-beta.1";
     * non-numeric parts are compared lexicographically and rank below numbers.
     */
    fun compare(a: String, b: String): Int {
        val pa = a.split('.', '-')
        val pb = b.split('.', '-')
        val max = maxOf(pa.size, pb.size)

        for (i in 0 until max) {
            val ai = pa.getOrNull(i) ?: "0"
            val bi = pb.getOrNull(i) ?: "0"

            val aiNum = ai.toIntOrNull()
            val biNum = bi.toIntOrNull()

            val cmp = when {
                aiNum != null && biNum != null -> aiNum.compareTo(biNum)
                aiNum != null && biNum == null -> 1              // numeric > text
                aiNum == null && biNum != null -> -1             // text < numeric
                else -> ai.compareTo(bi)                          // both text (e.g., "beta" vs "rc")
            }

            if (cmp != 0) return cmp
        }
        return 0
    }
}
