# Handleliste

A shopping list for Android in Norwegian and English.

- **Your list pops up in the store.** When you walk into a grocery store, you get a notification with your list, and the app opens on a big in-store checklist. Store locations come from OpenStreetMap through the free Overpass API.
- **Check deals.** Searches this week's flyer deals near you for everything on your list, using eTilbudsavis/Tjek (free, no key). It shows which store has the most of your list on sale. You can also add a free [Kassalapp](https://kassal.app/api) key in Settings to compare everyday shelf prices.
- **Dinner ideas.** AI turns your list into dinner suggestions, using Google Gemini with your own free key from [AI Studio](https://aistudio.google.com/apikey). It can add missing ingredients to the list in one tap. Without a key, it can send the same question to ChatGPT instead.
- **Updates itself.** Every push to `main` builds an APK and publishes it under [Releases](../../releases). The app checks for a new release when it opens and installs it.

## Battery

When you're far from any store, the app asks for a rough position every 2 minutes. Within about 300 m of a store it switches to GPS every 20 seconds. You count as in a store after two checks at least 30 seconds apart, so driving past doesn't set it off. Each store pops up at most once every 2 hours.
