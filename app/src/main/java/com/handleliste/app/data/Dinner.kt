package com.handleliste.app.data

import com.handleliste.app.util.Lang

/**
 * Dinner ideas from your lists. The question goes to ChatGPT (no login, no key) in the
 * background, and the answer is shown in the app's own chat.
 */
object Dinner {
    /** ChatGPT is asked to end with this line, so the missing groceries become a button. */
    private val BUY_LINE = Regex("(?im)^\\W*(HANDLE|KJØP|BUY)[*\\s]*:[*\\s]*(.+)$")

    fun prompt(list: List<String>, extra: String, onSale: List<String>, wishes: String): String {
        val b = StringBuilder()
        if (Lang.no) {
            b.append("Hva kan jeg lage til middag? Foreslå 3 middager, mest mulig laget av det jeg skal handle.\n")
            b.append("Handlelisten min: ${list.joinToString(", ").ifBlank { "(tom)" }}\n")
            if (extra.isNotBlank()) b.append("Dette har jeg hjemme fra før: $extra\n")
            if (onSale.isNotEmpty()) b.append("På tilbud nær meg denne uken (bruk gjerne): ${onSale.joinToString(", ")}\n")
            if (wishes.isNotBlank()) b.append("Ønsker: $wishes\n")
            b.append("Anta at jeg har salt, pepper, olje, smør og vanlige krydder. Svar på norsk, kort og oversiktlig: " +
                "navn på retten, tid, hvorfor den passer, og en enkel oppskrift med ingredienser og fremgangsmåte for hver.\n")
            b.append("Avslutt med én linje som starter med «HANDLE:» og ramser opp det jeg mangler for alle tre, " +
                "med korte varenavn slik de heter i norske butikker, adskilt med komma. Skriv «HANDLE: ingenting» hvis jeg har alt.")
        } else {
            b.append("What can I make for dinner? Suggest 3 dinners, built as much as possible from what I'm buying.\n")
            b.append("My shopping list: ${list.joinToString(", ").ifBlank { "(empty)" }}\n")
            if (extra.isNotBlank()) b.append("Already at home: $extra\n")
            if (onSale.isNotEmpty()) b.append("On sale near me this week (use if it fits): ${onSale.joinToString(", ")}\n")
            if (wishes.isNotBlank()) b.append("Wishes: $wishes\n")
            b.append("Assume I have salt, pepper, oil, butter and common spices. Keep it short and clear: " +
                "dish name, time, why it fits, and a simple recipe with ingredients and steps for each.\n")
            b.append("End with one line starting with \"BUY:\" listing what I'm missing for all three, short grocery names, " +
                "separated by commas. Write \"BUY: nothing\" if I have everything.")
        }
        return b.toString()
    }

    /** Groceries ChatGPT says you still need (from its last "HANDLE:" line). */
    fun missing(answer: String): List<String> {
        val m = BUY_LINE.findAll(answer).lastOrNull() ?: return emptyList()
        return m.groupValues[2].replace("*", "").split(",", ";").map { it.trim().trimEnd('.') }
            .filter { it.isNotBlank() && !it.equals("ingenting", true) && !it.equals("nothing", true) && it.length < 40 }
    }

    /** The answer without the "HANDLE:" line (that one becomes a button). */
    fun display(answer: String): String = answer.replace(BUY_LINE, "").trim()
}
