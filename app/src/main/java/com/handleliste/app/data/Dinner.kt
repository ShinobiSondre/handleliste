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
            b.append("Gi hver rett en kort overskrift med navnet. Avslutt svaret med én linje per rett, nøyaktig slik: " +
                "«RETT: <navn> | HANDLE: <det jeg må kjøpe, korte varenavn slik de heter i norske butikker, adskilt med komma> | " +
                "BRUKER: <varer fra handlelisten min som retten bruker>». Skriv «ingenting» der det er tomt.")
        } else {
            b.append("What can I make for dinner? Suggest 3 dinners, built as much as possible from what I'm buying.\n")
            b.append("My shopping list: ${list.joinToString(", ").ifBlank { "(empty)" }}\n")
            if (extra.isNotBlank()) b.append("Already at home: $extra\n")
            if (onSale.isNotEmpty()) b.append("On sale near me this week (use if it fits): ${onSale.joinToString(", ")}\n")
            if (wishes.isNotBlank()) b.append("Wishes: $wishes\n")
            b.append("Assume I have salt, pepper, oil, butter and common spices. Keep it short and clear: " +
                "dish name, time, why it fits, and a simple recipe with ingredients and steps for each.\n")
            b.append("Give each dish a short heading with its name. End the answer with one line per dish, exactly like this: " +
                "\"DISH: <name> | BUY: <what I must buy, short grocery names, comma-separated> | USES: <items from my shopping list the dish uses>\". " +
                "Write \"nothing\" where empty.")
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
            if (list.isNotEmpty()) b.append("Dette står på handlelisten min og skal kjøpes uansett: ${list.joinToString(", ")}. " +
                "Bruk gjerne disse varene, men de er IKKE gratis: prisen på varene fra handlelisten som middagen bruker, må telles med i totalen.\n")
            if (extra.isNotBlank()) b.append("Dette har jeg hjemme fra før (koster ingenting): $extra\n")
            if (wishes.isNotBlank()) b.append("Ønsker: $wishes\n")
            b.append("Anta at jeg har salt, pepper, olje, smør og vanlige krydder. Tell med hele pakninger slik de selges i butikken. " +
                "Foreslå 1 middag (og gjerne 1 billigere reserve). Vis en kort handleliste med pris per vare og butikk, totalsum, og en enkel oppskrift. " +
                "Bruk norske priser for 2025–2026 der tilbudene ikke dekker det. Hold deg under $budget kr. Svar på norsk.\n")
            b.append("Totalen skal være alt middagen koster: varene fra handlelisten som brukes + det jeg må kjøpe i tillegg.\n")
            b.append("Avslutt svaret med én linje per rett (hovedforslaget og reserven), nøyaktig slik: " +
                "«RETT: <navn> | HANDLE: <det jeg må kjøpe i tillegg, korte varenavn adskilt med komma> | " +
                "BRUKER: <varer fra handlelisten min som retten bruker>». Skriv «ingenting» der det er tomt.")
        } else {
            b.append("Make a dinner for $servings ${if (servings == 1) "person" else "people"} that costs at most $budget NOK in total to buy.\n")
            if (deals.isNotEmpty()) {
                b.append("These are this week's deals in stores near me (real prices, use them):\n")
                deals.forEach { b.append(it).append("\n") }
            }
            if (list.isNotEmpty()) b.append("On my shopping list, to be bought anyway: ${list.joinToString(", ")}. " +
                "Use them if they fit, but they are NOT free: the price of the list items the dinner uses must be counted in the total.\n")
            if (extra.isNotBlank()) b.append("Already at home (free): $extra\n")
            if (wishes.isNotBlank()) b.append("Wishes: $wishes\n")
            b.append("Assume I have salt, pepper, oil, butter and common spices. Count whole packs as sold in Norwegian stores. " +
                "Suggest 1 dinner (and maybe 1 cheaper backup). Show a short shopping list with price per item and store, the total, and a simple recipe. " +
                "Use Norwegian 2025–2026 prices where the deals don't cover it. Stay under $budget NOK.\n")
            b.append("The total is everything the dinner costs: the shopping-list items it uses + what I must buy extra.\n")
            b.append("End the answer with one line per dish (the main one and the backup), exactly like this: " +
                "\"DISH: <name> | BUY: <what I must buy extra, short grocery names, comma-separated> | USES: <items from my shopping list the dish uses>\". " +
                "Write \"nothing\" where empty.")
        }
        return b.toString()
    }

    /** Groceries ChatGPT says you still need (from its last "HANDLE:" line). */
    fun missing(answer: String): List<String> {
        val m = BUY_LINE.findAll(answer).lastOrNull() ?: return emptyList()
        return m.groupValues[2].replace("*", "").split(",", ";").map { it.trim().trimEnd('.') }
            .filter { it.isNotBlank() && !it.equals("ingenting", true) && !it.equals("nothing", true) && it.length < 40 }
    }

    private val USES_LINE = Regex("(?im)^\\W*(BRUKER|USES)[*\\s]*:[*\\s]*(.+)$")

    /**
     * Shopping-list items the dinner uses (they cost money too). From ChatGPT's "BRUKER:" line, matched back
     * to your list; if it left the line out, the list items its answer mentions.
     */
    fun usedFromList(answer: String, list: List<String>): List<String> {
        val line = USES_LINE.findAll(answer).lastOrNull()?.groupValues?.get(2)
        val body = display(answer).lowercase()
        if (line == null) return list.filter { item -> Parse.searchTerm(item).split(" ").filter { it.length >= 3 }.let { w -> w.isNotEmpty() && w.all { body.contains(it) } } }
        val named = line.replace("*", "").split(",", ";").map { it.trim().trimEnd('.').lowercase() }
            .filter { it.isNotBlank() && it !in setOf("ingen", "none", "ingenting", "nothing") }
        return list.filter { item ->
            val t = Parse.searchTerm(item)
            named.any { n -> n.contains(t) || t.contains(n) || Parse.searchTerm(n) == t }
        }
    }

    private val DISH_LINE = Regex("(?im)^\\W*(RETT|DISH)\\s*\\d*[*\\s]*[:.][*\\s]*([^\\n|]+\\|[^\\n]*)$")
    private val EMPTY = setOf("ingenting", "nothing", "ingen", "none", "-", "")

    /** One dish ChatGPT suggested: what to buy for it, and which list items it uses. */
    data class Dish(val name: String, val buy: List<String>, val uses: List<String>)

    private fun items(s: String) = s.replace("*", "").split(",", ";").map { it.trim().trimEnd('.') }
        .filter { it.lowercase() !in EMPTY && it.length < 40 }

    /** The dishes from ChatGPT's "RETT: name | HANDLE: … | BRUKER: …" lines. */
    fun dishes(answer: String): List<Dish> = DISH_LINE.findAll(answer).mapNotNull { m ->
        val parts = m.groupValues[2].split("|").map { it.trim() }
        val name = parts.first().replace("*", "").trim().trimEnd('.')
        if (name.isBlank()) return@mapNotNull null
        fun part(vararg keys: String) = parts.drop(1).firstOrNull { p -> keys.any { p.uppercase().startsWith(it) } }
            ?.substringAfter(":")?.let { items(it) }.orEmpty()
        Dish(name, part("HANDLE", "BUY", "KJØP"), part("BRUKER", "USES"))
    }.distinctBy { it.name.lowercase() }.toList()

    /** A dish's "BRUKER" items matched back to the names on your list. */
    fun usesFromList(d: Dish, list: List<String>): List<String> = list.filter { item ->
        val t = Parse.searchTerm(item)
        d.uses.any { n -> val l = n.lowercase(); l.contains(t) || t.contains(l) || Parse.searchTerm(l) == t }
    }

    /** The answer without the machine lines (they become dish cards, the price check and buttons). */
    fun display(answer: String): String = answer.replace(DISH_LINE, "").replace(BUY_LINE, "").replace(USES_LINE, "").trim()
}
