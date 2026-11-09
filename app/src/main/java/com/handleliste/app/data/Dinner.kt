package com.handleliste.app.data

import com.handleliste.app.util.Lang

/**
 * Dinner ideas from your lists. The question goes to ChatGPT (no login, no key) in the
 * background, and the answer is shown in the app's own chat.
 */
object Dinner {
    /** ChatGPT is asked to end with this line, so the missing groceries become a button. */
    private val BUY_LINE = Regex("(?im)^\\W*(HANDLE|KJØP|BUY)[*\\s]*:[*\\s]*(.+)$")

    fun prompt(list: List<String>, extra: String, onSale: List<String>, wishes: String, avoid: List<String> = emptyList()): String {
        val b = StringBuilder()
        if (Lang.no) {
            b.append("Hva kan jeg lage til middag? Foreslå 3 middager, mest mulig laget av det jeg skal handle.\n")
            b.append("Handlelisten min: ${list.joinToString(", ").ifBlank { "(tom)" }}\n")
            if (extra.isNotBlank()) b.append("Dette har jeg hjemme fra før: $extra\n")
            if (onSale.isNotEmpty()) b.append("På tilbud nær meg denne uken (bruk gjerne): ${onSale.joinToString(", ")}\n")
            if (wishes.isNotBlank()) b.append("Dette er viktig for meg, følg det: $wishes\n")
            if (avoid.isNotEmpty()) b.append("Jeg har nylig fått disse forslagene, så foreslå noe annet enn dem (og varier gjerne kjøkken, f.eks. norsk, italiensk, asiatisk, indisk, meksikansk): ${avoid.joinToString("; ")}\n")
            b.append("Anta at jeg har salt, pepper, olje, smør og vanlige krydder. Svar på norsk og KORT: for hver rett bare navnet som overskrift, " +
                "tid, og én setning om hvorfor den passer. Ikke skriv oppskriften ennå, den ber jeg om etterpå.\n")
            b.append("Gi hver rett en kort overskrift med navnet. Avslutt svaret med én linje per rett, nøyaktig slik: " +
                "«RETT: <navn> | HANDLE: <det jeg må kjøpe, korte varenavn slik de heter i norske butikker, adskilt med komma> | " +
                "BRUKER: <varer fra handlelisten min som retten bruker> | ALT: <alle ingrediensene retten trenger, unntatt salt, pepper, olje og krydder> | BILDE: <2–4 engelske ord som beskriver retten, til bildesøk>». Skriv «ingenting» der det er tomt.")
        } else {
            b.append("What can I make for dinner? Suggest 3 dinners, built as much as possible from what I'm buying.\n")
            b.append("My shopping list: ${list.joinToString(", ").ifBlank { "(empty)" }}\n")
            if (extra.isNotBlank()) b.append("Already at home: $extra\n")
            if (onSale.isNotEmpty()) b.append("On sale near me this week (use if it fits): ${onSale.joinToString(", ")}\n")
            if (wishes.isNotBlank()) b.append("This matters to me, follow it: $wishes\n")
            if (avoid.isNotEmpty()) b.append("I was recently suggested these, so suggest something different (and vary the cuisine, e.g. Nordic, Italian, Asian, Indian, Mexican): ${avoid.joinToString("; ")}\n")
            b.append("Assume I have salt, pepper, oil, butter and common spices. Keep it SHORT: for each dish just its name as a heading, " +
                "the time, and one sentence on why it fits. Don't write the recipe yet; I'll ask for it afterwards.\n")
            b.append("Give each dish a short heading with its name. End the answer with one line per dish, exactly like this: " +
                "\"DISH: <name> | BUY: <what I must buy, short grocery names, comma-separated> | USES: <items from my shopping list the dish uses> | ALL: <every ingredient the dish needs, except salt, pepper, oil and spices> | IMAGE: <2-4 English words describing the dish, for an image search>\". " +
                "Write \"nothing\" where empty.")
        }
        return b.toString()
    }

    /** "Make me dinner for 100 kr": real prices near you this week, so the plan can actually be bought. */
    fun budgetPrompt(budget: Int, servings: Int, list: List<String>, extra: String, deals: List<String>, wishes: String, avoid: List<String> = emptyList()): String {
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
            if (wishes.isNotBlank()) b.append("Dette er viktig for meg, følg det: $wishes\n")
            if (avoid.isNotEmpty()) b.append("Jeg har nylig fått disse forslagene, så foreslå noe annet enn dem (og varier gjerne kjøkken, f.eks. norsk, italiensk, asiatisk, indisk, meksikansk): ${avoid.joinToString("; ")}\n")
            b.append("Anta at jeg har salt, pepper, olje, smør og vanlige krydder. Tell med hele pakninger slik de selges i butikken. " +
                "Foreslå 1 middag (og gjerne 1 billigere reserve). Svar KORT: navnet som overskrift, tid, én setning om hvorfor, og omtrentlig totalsum. Ikke skriv oppskriften ennå. " +
                "Bruk norske priser for 2025–2026 der tilbudene ikke dekker det. Hold deg under $budget kr. Svar på norsk.\n")
            b.append("Totalen skal være alt middagen koster: varene fra handlelisten som brukes + det jeg må kjøpe i tillegg.\n")
            b.append("Avslutt svaret med én linje per rett (hovedforslaget og reserven), nøyaktig slik: " +
                "«RETT: <navn> | HANDLE: <det jeg må kjøpe i tillegg, korte varenavn adskilt med komma> | " +
                "BRUKER: <varer fra handlelisten min som retten bruker> | ALT: <alle ingrediensene retten trenger, unntatt salt, pepper, olje og krydder> | BILDE: <2–4 engelske ord som beskriver retten, til bildesøk>». Skriv «ingenting» der det er tomt.")
        } else {
            b.append("Make a dinner for $servings ${if (servings == 1) "person" else "people"} that costs at most $budget NOK in total to buy.\n")
            if (deals.isNotEmpty()) {
                b.append("These are this week's deals in stores near me (real prices, use them):\n")
                deals.forEach { b.append(it).append("\n") }
            }
            if (list.isNotEmpty()) b.append("On my shopping list, to be bought anyway: ${list.joinToString(", ")}. " +
                "Use them if they fit, but they are NOT free: the price of the list items the dinner uses must be counted in the total.\n")
            if (extra.isNotBlank()) b.append("Already at home (free): $extra\n")
            if (wishes.isNotBlank()) b.append("This matters to me, follow it: $wishes\n")
            if (avoid.isNotEmpty()) b.append("I was recently suggested these, so suggest something different (and vary the cuisine, e.g. Nordic, Italian, Asian, Indian, Mexican): ${avoid.joinToString("; ")}\n")
            b.append("Assume I have salt, pepper, oil, butter and common spices. Count whole packs as sold in Norwegian stores. " +
                "Suggest 1 dinner (and maybe 1 cheaper backup). Answer SHORT: the name as a heading, time, one sentence on why, and the rough total. Don't write the recipe yet. " +
                "Use Norwegian 2025–2026 prices where the deals don't cover it. Stay under $budget NOK.\n")
            b.append("The total is everything the dinner costs: the shopping-list items it uses + what I must buy extra.\n")
            b.append("End the answer with one line per dish (the main one and the backup), exactly like this: " +
                "\"DISH: <name> | BUY: <what I must buy extra, short grocery names, comma-separated> | USES: <items from my shopping list the dish uses> | ALL: <every ingredient the dish needs, except salt, pepper, oil and spices> | IMAGE: <2-4 English words describing the dish, for an image search>\". " +
                "Write \"nothing\" where empty.")
        }
        return b.toString()
    }

    /** Asked when you pick a dish: just its recipe, short. */
    fun recipeQuestion(d: Dish): String = if (Lang.no)
        "Gi meg oppskriften på «${d.name}»: ingredienser med mengder, og fremgangsmåte i korte, nummererte steg. " +
            "Denne gangen svarer du BARE med oppskriften, uten RETT-linjer til slutt."
    else "Give me the recipe for \"${d.name}\": ingredients with amounts, and the method in short numbered steps. " +
        "This time answer ONLY with the recipe, without DISH lines at the end."

    fun recipeLabel(d: Dish): String = (if (Lang.no) "Oppskrift: " else "Recipe: ") + d.name

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
    data class Dish(val name: String, val buy: List<String>, val uses: List<String>, val imageQuery: String = "", val all: List<String> = emptyList())

    private fun items(s: String) = s.replace("*", "").split(",", ";").map { it.trim().trimEnd('.') }
        .filter { it.lowercase() !in EMPTY && it.length < 40 }

    /** The dishes from ChatGPT's "RETT: name | HANDLE: … | BRUKER: …" lines. */
    fun dishes(answer: String): List<Dish> = DISH_LINE.findAll(answer).mapNotNull { m ->
        val parts = m.groupValues[2].split("|").map { it.trim() }
        val name = parts.first().replace("*", "").trim().trimEnd('.')
        if (name.isBlank()) return@mapNotNull null
        fun part(vararg keys: String) = parts.drop(1).firstOrNull { p -> keys.any { p.uppercase().startsWith(it) } }
            ?.substringAfter(":")?.let { items(it) }.orEmpty()
        val img = parts.drop(1).firstOrNull { p -> p.uppercase().startsWith("BILDE") || p.uppercase().startsWith("IMAGE") }
            ?.substringAfter(":")?.replace("*", "")?.trim().orEmpty()
        Dish(name, part("HANDLE", "BUY", "KJØP"), part("BRUKER", "USES"), img, part("ALT", "ALL", "INGREDIENSER", "INGREDIENTS"))
    }.distinctBy { it.name.lowercase() }.toList()

    private fun isHeading(l: String): Boolean {
        val t = l.trim()
        return t.startsWith("#") || (t.startsWith("**") && t.endsWith("**") && t.length < 90) ||
            Regex("^\\d+[.)]\\s+\\S").containsMatchIn(t) && t.length < 90
    }

    /**
     * The recipe for one dish, cut out of ChatGPT's answer: from the heading that names the dish to
     * the heading of the next dish. Without a matching heading: the whole answer.
     */
    fun recipeFor(answer: String, dish: Dish, all: List<Dish>): String {
        val lines = display(answer).lines()
        fun startOf(d: Dish): Int {
            val words = Parse.keywords(d.name).filter { it.length >= 4 }.ifEmpty { Parse.keywords(d.name) }
            var best = -1; var bestScore = 0.0
            lines.forEachIndexed { i, l ->
                if (!isHeading(l)) return@forEachIndexed
                val h = l.lowercase()
                val score = if (words.isEmpty()) 0.0 else words.count { h.contains(it) }.toDouble() / words.size
                if (score > bestScore) { bestScore = score; best = i }
            }
            return if (bestScore >= 0.5) best else -1
        }
        val start = startOf(dish)
        if (start < 0) return display(answer)
        val next = all.filter { it != dish }.map { startOf(it) }.filter { it > start }.minOrNull() ?: lines.size
        // Drop a closing shopping summary ("Handleoversikt", "Totalt") if the last dish runs to the end.
        var end = next
        if (next == lines.size) {
            val summary = (start + 1 until lines.size).firstOrNull { i -> isHeading(lines[i]) &&
                Regex("(?i)(handleoversikt|handleliste|oppsummering|totalt|total|shopping list|summary)").containsMatchIn(lines[i]) }
            if (summary != null) end = summary
        }
        return lines.subList(start, end).joinToString("\n").trim()
    }

    private val UNITS = setOf("g", "gram", "kg", "hg", "dl", "l", "liter", "ml", "cl", "ss", "ts", "spiseskje", "spiseskjeer", "teskje", "teskjeer",
        "stk", "stykk", "boks", "bokser", "pakke", "pakker", "pk", "pose", "poser", "beger", "fedd", "klype", "neve", "skive", "skiver",
        "kopp", "kopper", "glass", "flaske", "bunt", "ca", "cirka", "liten", "lite", "små", "stor", "stort", "store", "middels", "evt", "eventuelt",
        "cup", "cups", "tbsp", "tsp", "can", "cans", "pack", "packet", "bag", "jar", "slice", "slices", "clove", "cloves", "pinch", "small", "large", "medium", "x")
    private val SKIP = setOf("vann", "water", "kaldt vann", "varmt vann")

    /** "400 g kjøttdeig", "1 liten boks mais (ca. 200 g)", "2 ss tacokrydder, gjerne mild" → "Kjøttdeig", "Mais", "Tacokrydder". */
    fun ingredientName(line: String): String? {
        var t = line.trim().trimStart('•', '-', '*', '–', ' ').replace("**", "")
        t = t.replace(Regex("\\([^)]*\\)"), " ").substringBefore(",").substringBefore(" – ").substringBefore(" - ")
        t = t.replace(Regex("(?i)\\b(etter smak|til steking|til servering|til pynt|for serving|to taste)\\b.*"), "")
        val words = t.trim().split(Regex("\\s+")).dropWhile { w ->
            val l = w.lowercase().trimEnd('.', ':')
            l.isEmpty() || l in UNITS || l.matches(Regex("[\\d.,/½¼¾⅓⅔–-]+")) || l.matches(Regex("\\d+[a-zæøå]{1,2}"))
        }
        val name = words.joinToString(" ").trim().trimEnd('.', ':', ';')
        if (name.length < 2 || name.length > 45 || name.lowercase() in SKIP) return null
        return name.replaceFirstChar { it.uppercase() }
    }

    /** The ingredients section of a recipe (between "Ingredienser" and "Fremgangsmåte"), one name per line. */
    fun ingredientsFromRecipe(recipe: String): List<String> {
        val lines = recipe.lines().map { it.trim() }
        val start = lines.indexOfFirst { Regex("(?i)(ingrediens|ingredients|du trenger|you need)").containsMatchIn(it) && it.length < 60 }
        if (start < 0) return emptyList()
        val end = (start + 1 until lines.size).firstOrNull { i ->
            Regex("(?i)^\\W*(fremgangsmåte|framgangsmåte|slik gjør du|tilberedning|method|instructions|directions|steps|steg|servering|tips)").containsMatchIn(lines[i])
        } ?: lines.size
        return lines.subList(start + 1, end)
            .filter { it.isNotBlank() && !it.endsWith(":") && !Regex("(?i)^\\W*(til|for|topping|saus|sauce|dressing)\\W*$").matches(it) }
            .mapNotNull { ingredientName(it) }
            .fold(ArrayList<String>()) { acc, n -> if (acc.none { same(it, n) }) acc += n; acc }
    }

    /** "Taco chips", "tacochips" and "2 pk Tacochips" are the same thing. */
    private fun same(a: String, b: String): Boolean {
        val x = Parse.searchTerm(a).replace(" ", "").replace("-", "")
        val y = Parse.searchTerm(b).replace(" ", "").replace("-", "")
        return x.isNotEmpty() && y.isNotEmpty() && (x == y || (x.length >= 4 && y.contains(x)) || (y.length >= 4 && x.contains(y)))
    }

    /** A dish's "BRUKER" items matched back to the names on your list. */
    fun usesFromList(d: Dish, list: List<String>): List<String> = list.filter { item -> d.uses.any { same(it, item) } }

    /**
     * Everything to put on a new list for the dish: every ingredient (ChatGPT's "ALT"), named the way
     * your list names it where it's the same thing, plus anything to buy that wasn't in "ALT".
     * Without an "ALT" line: what it uses from your list (matched or not) + what to buy.
     */
    fun ingredientsForList(d: Dish, list: List<String>, recipe: String? = null): List<String> {
        // The recipe's own ingredient list is the most complete; ChatGPT's ALT line fills in the rest.
        val fromRecipe = recipe?.let { ingredientsFromRecipe(it) }.orEmpty()
        val base = (fromRecipe + d.all.ifEmpty { d.uses + d.buy }).fold(ArrayList<String>()) { acc, n -> if (acc.none { same(it, n) }) acc += n; acc }
        val named = base.map { ing -> list.firstOrNull { same(it, ing) } ?: ing.replaceFirstChar { it.uppercase() } }
        val extra = d.buy.filter { b -> named.none { same(it, b) } }.map { it.replaceFirstChar { c -> c.uppercase() } }
        val out = ArrayList<String>()
        (named + extra).forEach { n -> if (out.none { same(it, n) }) out += n }
        return out
    }

    /** The answer without the machine lines (they become dish cards, the price check and buttons). */
    fun display(answer: String): String = answer.replace(DISH_LINE, "").replace(BUY_LINE, "").replace(USES_LINE, "").trim()
}
