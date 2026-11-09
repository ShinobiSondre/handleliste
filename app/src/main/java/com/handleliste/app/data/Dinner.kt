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

    /** "Make me dinner for 100 kr": real prices near you this week, so the plan can actually be bought. */
    fun budgetPrompt(budget: Int, servings: Int, list: List<String>, extra: String, deals: List<String>, wishes: String): String {
        val b = StringBuilder()
        if (Lang.no) {
            b.append("Lag en middag til $servings ${if (servings == 1) "person" else "personer"} som koster maks $budget kr totalt å handle inn.\n")
            if (deals.isNotEmpty()) {
                b.append("Dette er ukens tilbud i butikker nær meg (ekte priser, bruk dem):\n")
                deals.forEach { b.append(it).append("\n") }
            }
            if (list.isNotEmpty()) b.append("Dette står allerede på handlelisten min: ${list.joinToString(", ")}\n")
            if (extra.isNotBlank()) b.append("Dette har jeg hjemme fra før (koster ingenting): $extra\n")
            if (wishes.isNotBlank()) b.append("Ønsker: $wishes\n")
            b.append("Anta at jeg har salt, pepper, olje, smør og vanlige krydder. Tell med hele pakninger slik de selges i butikken. " +
                "Foreslå 1 middag (og gjerne 1 billigere reserve). Vis en kort handleliste med pris per vare og butikk, totalsum, og en enkel oppskrift. " +
                "Bruk norske priser for 2025–2026 der tilbudene ikke dekker det. Hold deg under $budget kr. Svar på norsk.\n")
            b.append("Avslutt med én linje som starter med «HANDLE:» med varene jeg må kjøpe for hovedforslaget, korte varenavn, adskilt med komma.")
        } else {
            b.append("Make a dinner for $servings ${if (servings == 1) "person" else "people"} that costs at most $budget NOK in total to buy.\n")
            if (deals.isNotEmpty()) {
                b.append("These are this week's deals in stores near me (real prices, use them):\n")
                deals.forEach { b.append(it).append("\n") }
            }
            if (list.isNotEmpty()) b.append("Already on my shopping list: ${list.joinToString(", ")}\n")
            if (extra.isNotBlank()) b.append("Already at home (free): $extra\n")
            if (wishes.isNotBlank()) b.append("Wishes: $wishes\n")
            b.append("Assume I have salt, pepper, oil, butter and common spices. Count whole packs as sold in Norwegian stores. " +
                "Suggest 1 dinner (and maybe 1 cheaper backup). Show a short shopping list with price per item and store, the total, and a simple recipe. " +
                "Use Norwegian 2025–2026 prices where the deals don't cover it. Stay under $budget NOK.\n")
            b.append("End with one line starting with \"BUY:\" listing what I must buy for the main suggestion, short grocery names, separated by commas.")
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
