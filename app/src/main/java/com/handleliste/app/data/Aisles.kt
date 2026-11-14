package com.handleliste.app.data

/** The store's sections, in the order you usually walk through a Norwegian grocery store. */
enum class Aisle(val emoji: String, val en: String, val no: String) {
    PRODUCE("🥦", "Fruit & veg", "Frukt og grønt"),
    BAKERY("🍞", "Bread & bakery", "Brød og bakst"),
    MEAT("🥩", "Meat & chicken", "Kjøtt og kylling"),
    FISH("🐟", "Fish", "Fisk"),
    DELI("🥪", "Cold cuts & spreads", "Pålegg"),
    DAIRY("🥛", "Dairy & eggs", "Meieri og egg"),
    DRY("🍝", "Pasta, rice & baking", "Tørrvarer"),
    CANS("🥫", "Tins, sauces & spices", "Hermetikk, sauser og krydder"),
    BREAKFAST("🥣", "Breakfast", "Frokost"),
    SNACKS("🍫", "Snacks & sweets", "Snacks og godteri"),
    DRINKS("🥤", "Drinks", "Drikke"),
    FROZEN("🧊", "Frozen", "Frys"),
    HOME("🧻", "Home & hygiene", "Hus og hygiene"),
    BABY("🍼", "Baby", "Baby"),
    PET("🐾", "Pets", "Dyr"),
    OTHER("🛒", "Other", "Annet");
}

/**
 * Puts a list item in its section. Norwegian packs words together and the last part is the
 * thing itself ("sjokolademelk" is milk, "kyllingpålegg" is a cold cut), so the longest keyword
 * a word ends with wins, and later words count more than earlier ones.
 */
object Aisles {
    private val words: Map<Aisle, List<String>> = mapOf(
        Aisle.PRODUCE to listOf("eple", "epler", "banan", "bananer", "pære", "pærer", "appelsin", "appelsiner", "klementin", "klementiner", "sitron", "lime",
            "drue", "druer", "jordbær", "blåbær", "bringebær", "melon", "mango", "avokado", "kiwi", "ananas", "tomat", "tomater", "agurk", "salat", "isberg",
            "ruccola", "spinat", "løk", "rødløk", "vårløk", "hvitløk", "purre", "gulrot", "gulrøtter", "potet", "poteter", "søtpotet", "paprika", "brokkoli",
            "blomkål", "kål", "squash", "aubergine", "sopp", "champignon", "mais", "selleri", "ingefær", "chili", "persille", "koriander", "basilikum",
            "frukt", "grønnsaker", "bær", "reddik", "rødbeter", "asparges", "erter", "bønnespirer", "fennikel", "nektarin", "plomme", "fersken"),
        Aisle.BAKERY to listOf("brød", "loff", "rundstykker", "rundstykke", "baguett", "baguette", "ciabatta", "lomper", "lompe", "lefse", "lefser", "tortilla",
            "tortillas", "pitabrød", "pita", "naan", "knekkebrød", "rugbrød", "boller", "bolle", "kanelbolle", "kanelboller", "croissant", "kake", "kaker",
            "wienerbrød", "polarbrød", "pølsebrød", "hamburgerbrød", "brioche", "skillingsbolle", "flatbrød", "kavring"),
        Aisle.MEAT to listOf("kjøtt", "kjøttdeig", "karbonadedeig", "biff", "entrecote", "indrefilet", "ytrefilet", "kylling", "kyllingfilet", "kyllinglår",
            "kalkun", "svin", "svinekjøtt", "koteletter", "kotelett", "ribbe", "pinnekjøtt", "bacon", "pølse", "pølser", "grillpølser", "wienerpølser", "kjøttboller",
            "kjøttkaker", "lam", "lammekjøtt", "fårikål", "stek", "skinke", "medisterdeig", "medisterkaker", "kebab", "burger", "hamburger", "deig", "filet",
            "strimler", "chorizo", "kyllingvinger", "nakke"),
        Aisle.FISH to listOf("fisk", "laks", "laksefilet", "torsk", "sei", "ørret", "reker", "scampi", "tunfisk", "makrell", "fiskekaker", "fiskeboller",
            "fiskepinner", "sild", "kveite", "krabbe", "blåskjell", "sushi", "røkelaks", "fiskegrateng", "fiskeburger", "lutefisk", "klippfisk", "torskefilet", "seifilet", "ørretfilet", "torskeloins", "laksebiter"),
        Aisle.DELI to listOf("pålegg", "salami", "servelat", "kokt skinke", "leverpostei", "postei", "kaviar", "majones", "rekesalat", "italiensk salat",
            "hummus", "syltetøy", "nugatti", "nutella", "peanøttsmør", "honning", "makrell i tomat", "spekeskinke", "spekepølse", "fenalår", "prim", "kjøttpålegg"),
        Aisle.DAIRY to listOf("melk", "lettmelk", "helmelk", "skummet", "fløte", "matfløte", "kremfløte", "rømme", "lettrømme", "crème fraîche", "creme fraiche",
            "yoghurt", "kesam", "cottage", "ost", "brunost", "norvegia", "jarlsberg", "mozzarella", "parmesan", "feta", "fetaost", "smøreost", "kremost", "philadelphia",
            "smør", "margarin", "bremykt", "egg", "skyr", "kefir", "kulturmelk", "biola", "havremelk", "soyamelk", "mandelmelk", "pudding", "sjokolademelk", "iskaffe", "revet"),
        Aisle.DRY to listOf("pasta", "spaghetti", "makaroni", "penne", "fusilli", "lasagneplater", "tagliatelle", "nudler", "ris", "jasminris", "basmatiris",
            "couscous", "bulgur", "quinoa", "mel", "hvetemel", "sammalt", "sukker", "melis", "bakepulver", "gjær", "natron", "vaniljesukker", "kakao", "havregryn",
            "linser", "kikerter", "bønner", "olje", "olivenolje", "rapsolje", "eddik", "potetmos", "posesuppe", "suppe", "taco", "tacokrydder", "tacoskjell",
            "nachos", "lasagne", "kokosmelk", "nøtter", "mandler", "rosiner", "sjokoladebiter", "pizzabunn", "pizzamel", "tørrgjær", "pizzadeig", "pizzasaus"),
        Aisle.CANS to listOf("hermetikk", "hakkede tomater", "tomatpuré", "tomatpure", "passata", "ketchup", "sennep", "dressing", "saus", "pesto", "salsa", "soyasaus",
            "buljong", "kraft", "krydder", "salt", "pepper", "paprikapulver", "karri", "curry", "kanel", "oregano", "timian", "chilipulver", "spisskummen",
            "hvitløkspulver", "mais på boks", "kokosmelk", "sriracha", "worcestershire", "remulade", "aioli", "currypaste", "bearnaise", "brun saus", "sjysaus"),
        Aisle.BREAKFAST to listOf("frokostblanding", "müsli", "musli", "granola", "cornflakes", "havregrøt", "grøt", "cheerios", "frokost"),
        Aisle.SNACKS to listOf("chips", "potetgull", "snacks", "godteri", "sjokolade", "smågodt", "kjeks", "popcorn", "nøttemiks", "lakris", "tyggegummi",
            "kvikk lunsj", "freia", "dip", "dipmix", "pastiller", "seigmenn", "smash", "twist", "kexchoklad"),
        Aisle.DRINKS to listOf("brus", "cola", "pepsi", "solo", "fanta", "sprite", "farris", "mineralvann", "vann", "juice", "appelsinjuice", "eplejuice", "saft",
            "kaffe", "te", "øl", "cider", "energidrikk", "red bull", "monster", "iste", "smoothie", "kaffekapsler", "kakao drikke", "urge", "vørterøl"),
        Aisle.FROZEN to listOf("frossen", "frosne", "frys", "is", "iskrem", "pizza", "grandiosa", "fiskepinner frosne", "frosne grønnsaker", "pommes frites",
            "pommes", "frites", "wok", "frossenpizza", "isbiter", "sorbet", "kroneis", "lasagne frossen", "vårruller"),
        Aisle.HOME to listOf("dopapir", "toalettpapir", "tørkerull", "kjøkkenrull", "servietter", "oppvask", "oppvaskmiddel", "zalo", "maskinoppvask", "tabs",
            "vaskemiddel", "skyllemiddel", "såpe", "håndsåpe", "sjampo", "shampoo", "balsam", "tannkrem", "tannbørste", "deodorant", "barberhøvel", "bind", "tamponger",
            "plastposer", "søppelposer", "bakepapir", "aluminiumsfolie", "plastfolie", "lyspærer", "batterier", "fryseposer", "svamp", "klut", "rengjøring", "zalo"),
        Aisle.BABY to listOf("bleier", "bleie", "våtservietter", "barnemat", "morsmelkerstatning", "smokk", "grøt baby"),
        Aisle.PET to listOf("kattemat", "hundemat", "kattesand", "hundegodbit", "dyrefor"),
    )

    // keyword → aisle, longest keywords first.
    private val index: List<Pair<String, Aisle>> =
        words.flatMap { (a, l) -> l.map { it to a } }.sortedByDescending { it.first.length }

    fun of(name: String): Aisle {
        val n = name.lowercase().trim()
        // Whole phrases first ("hakkede tomater", "kokt skinke").
        index.firstOrNull { (k, _) -> k.contains(' ') && n.contains(k) }?.let { return it.second }
        val ws = n.split(Regex("[^\\p{L}]+")).filter { it.length >= 2 }
        // The last word is the thing itself; earlier words only count if it's unknown.
        for (w in ws.reversed()) {
            index.firstOrNull { (k, _) -> !k.contains(' ') && (w == k || (k.length >= 3 && w.endsWith(k))) }?.let { return it.second }
        }
        for (w in ws.reversed()) {
            index.firstOrNull { (k, _) -> !k.contains(' ') && k.length >= 4 && w.startsWith(k) }?.let { return it.second }
        }
        return Aisle.OTHER
    }

    fun of(item: Item): Aisle = of(item.name).takeIf { it != Aisle.OTHER } ?: item.pin?.let { of(it.name) } ?: Aisle.OTHER

    /** Items grouped by section in walking order. */
    fun group(items: List<Item>): List<Pair<Aisle, List<Item>>> =
        items.groupBy { of(it) }.toSortedMap(compareBy { it.ordinal }).map { it.key to it.value }
}
