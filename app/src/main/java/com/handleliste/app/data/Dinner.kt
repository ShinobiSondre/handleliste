package com.handleliste.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.handleliste.app.util.Http
import com.handleliste.app.util.HttpException
import com.handleliste.app.util.Lang
import com.handleliste.app.util.tr
import org.json.JSONArray
import org.json.JSONObject

/**
 * Dinner ideas made from your shopping list by an AI.
 * Uses Google's Gemini API with your own free key (aistudio.google.com/apikey).
 * Without a key, the same question can be opened in ChatGPT or Gemini in the browser.
 */
object Dinner {
    var recipes by mutableStateOf<List<Recipe>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    // "-latest" names always point at Google's newest free Flash models, so this keeps working as models change.
    private val MODELS = listOf("gemini-flash-latest", "gemini-flash-lite-latest", "gemini-2.5-flash")

    fun prompt(list: List<String>, extra: String, onSale: List<String>, wishes: String): String {
        val no = Lang.no
        val b = StringBuilder()
        if (no) {
            b.append("Du er en norsk hjemmekokk. Foreslå 3 middager jeg kan lage, mest mulig basert på handlelisten min.\n")
            b.append("Handlelisten: ${list.joinToString(", ").ifBlank { "(tom)" }}\n")
            if (extra.isNotBlank()) b.append("Dette har jeg allerede hjemme: $extra\n")
            if (onSale.isNotEmpty()) b.append("Dette er på tilbud i butikker nær meg nå (bruk gjerne): ${onSale.joinToString(", ")}\n")
            if (wishes.isNotBlank()) b.append("Ønsker: $wishes\n")
            b.append("Anta at jeg har salt, pepper, olje, smør og vanlige krydder. Skriv alt på norsk (bokmål), med norske varenavn slik de heter i butikken.\n")
        } else {
            b.append("You are a home cook. Suggest 3 dinners I can make, built as much as possible from my shopping list.\n")
            b.append("Shopping list: ${list.joinToString(", ").ifBlank { "(empty)" }}\n")
            if (extra.isNotBlank()) b.append("I already have at home: $extra\n")
            if (onSale.isNotEmpty()) b.append("On sale in stores near me right now (use if it fits): ${onSale.joinToString(", ")}\n")
            if (wishes.isNotBlank()) b.append("Wishes: $wishes\n")
            b.append("Assume I have salt, pepper, oil, butter and common spices. Write everything in English.\n")
        }
        b.append(
            "Answer ONLY with a JSON array of 3 objects with these keys: " +
                "\"title\" (string), \"minutes\" (int), \"servings\" (int), " +
                "\"uses\" (array: items from my list it uses), \"missing\" (array: things I'd still need to buy, short shop names, empty if none), " +
                "\"ingredients\" (array with amounts), \"steps\" (array, short steps), \"why\" (one short sentence)."
        )
        return b.toString()
    }

    suspend fun suggest(ctx: Context, list: List<String>, extra: String, onSale: List<String>, wishes: String) {
        if (loading) return
        val key = Settings.geminiKey(ctx)
        if (key.isBlank()) { error = null; return }
        loading = true
        error = null
        try {
            val body = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", prompt(list, extra, onSale, wishes))))))
                .put("generationConfig", JSONObject().put("temperature", 0.9).put("responseMimeType", "application/json"))
                .toString()
            var last: Exception? = null
            for (model in MODELS) {
                try {
                    val res = Http.post(
                        "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent",
                        body, "application/json", mapOf("x-goog-api-key" to key), timeoutMs = 60000
                    )
                    val r = Parse.recipes(Parse.geminiText(res))
                    if (r.isNotEmpty()) { recipes = r; return }
                    last = Exception("empty")
                } catch (e: HttpException) {
                    last = e
                    // A missing model: try the next one. Anything else (bad key, quota) won't get better.
                    if (e.code != 404) break
                } catch (e: Exception) { last = e }
            }
            error = when ((last as? HttpException)?.code) {
                400, 401, 403 -> tr("The Gemini key didn't work. Check it in Settings.", "Gemini-nøkkelen virket ikke. Sjekk den i Innstillinger.")
                429 -> tr("The free AI quota is used up for now. Try again in a minute.", "Den gratis AI-kvoten er brukt opp akkurat nå. Prøv igjen om et minutt.")
                else -> tr("Couldn't get dinner ideas. Check your internet and try again.", "Fikk ikke middagsforslag. Sjekk internett og prøv igjen.")
            }
        } finally {
            loading = false
        }
    }

    /** No key: open the same question in ChatGPT in the browser. */
    fun openInChatGpt(ctx: Context, prompt: String) {
        val text = prompt.substringBefore("Answer ONLY with").trim() +
            if (Lang.no) "\nGi meg oppskrift med ingredienser og fremgangsmåte for hver." else "\nGive me a recipe with ingredients and steps for each."
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com/?q=" + Http.enc(text)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
