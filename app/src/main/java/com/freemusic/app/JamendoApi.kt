package com.freemusic.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Track(
    val id: String,
    val name: String,
    val artist: String,
    val album: String,
    val image: String,
    val audioUrl: String
)

/**
 * Minimal Jamendo client (free, legal, ad-free music from independent artists).
 * Needs a free client_id from https://devportal.jamendo.com
 * Call from a background thread.
 */
object JamendoApi {

    fun fetchTracks(clientId: String, query: String): List<Track> {
        val params = buildString {
            append("client_id=").append(enc(clientId))
            append("&format=json&limit=40&audioformat=mp32&imagesize=300")
            if (query.isBlank()) {
                append("&order=popularity_week")
            } else {
                append("&search=").append(enc(query.trim()))
                append("&order=relevance")
            }
        }

        val conn = URL("https://api.jamendo.com/v3.0/tracks/?$params").openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.requestMethod = "GET"

        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("Server returned HTTP $code")

            val root = JSONObject(body)
            val headers = root.optJSONObject("headers")
            if (headers != null && headers.optString("status") != "success") {
                error(headers.optString("error_message").ifBlank { "Jamendo rejected the request" })
            }

            val results = root.optJSONArray("results") ?: return emptyList()
            val tracks = ArrayList<Track>(results.length())
            for (i in 0 until results.length()) {
                val o = results.getJSONObject(i)
                val audio = o.optString("audio")
                if (audio.isBlank()) continue // not streamable
                tracks.add(
                    Track(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        artist = o.optString("artist_name"),
                        album = o.optString("album_name"),
                        image = o.optString("image"),
                        audioUrl = audio
                    )
                )
            }
            return tracks
        } finally {
            conn.disconnect()
        }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}
