package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.R

object UrlHandler {

    private const val VVT_HOST = "vvt.org.in"
    private const val VVT_HOST_WWW = "www.vvt.org.in"

    /**
     * Checks if the url is internal to the VVT website.
     */
    fun isInternalUrl(url: String): Boolean {
        val uri = try {
            Uri.parse(url)
        } catch (_: Exception) {
            return false
        }
        val host = uri.host?.lowercase() ?: return false
        return host == VVT_HOST || host == VVT_HOST_WWW || host.endsWith(".$VVT_HOST")
    }

    /**
     * Dispatches external URLs to system apps:
     * - tel: -> Dial intent (shows phone dialer without direct calling)
     * - mailto: -> Email app
     * - whatsapp: / api.whatsapp.com / wa.me -> WhatsApp app or web fallback
     * - maps: / maps.google.com / geo: -> Google Maps
     * - external http(s) -> System browser
     *
     * Returns true if handled externally, false otherwise.
     */
    fun handleExternalUrl(context: Context, url: String): Boolean {
        val uri = try {
            Uri.parse(url)
        } catch (_: Exception) {
            return false
        }

        val scheme = uri.scheme?.lowercase() ?: ""

        // 1. Telephone dialer
        if (scheme == "tel") {
            return try {
                val intent = Intent(Intent.ACTION_DIAL, uri)
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.cannot_open_link), Toast.LENGTH_SHORT).show()
                false
            }
        }

        // 2. Email client
        if (scheme == "mailto") {
            return try {
                val intent = Intent(Intent.ACTION_SENDTO, uri)
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.cannot_open_link), Toast.LENGTH_SHORT).show()
                false
            }
        }

        // 3. SMS client
        if (scheme == "sms" || scheme == "smsto") {
            return try {
                val intent = Intent(Intent.ACTION_SENDTO, uri)
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                false
            }
        }

        // 4. WhatsApp links
        val host = uri.host?.lowercase() ?: ""
        if (scheme == "whatsapp" || host == "wa.me" || host == "api.whatsapp.com" || host.contains("whatsapp")) {
            return try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.whatsapp")
                }
                context.startActivity(intent)
                true
            } catch (_: Exception) {
                // Try WhatsApp Business or Browser fallback
                try {
                    val bizIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.whatsapp.w4b")
                    }
                    context.startActivity(bizIntent)
                    true
                } catch (_: Exception) {
                    // Browser fallback for wa.me / api.whatsapp.com
                    openInBrowser(context, uri)
                }
            }
        }

        // 5. Maps links
        if (scheme == "geo" || host.contains("maps.google") || host.contains("goo.gl/maps")) {
            return try {
                val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                context.startActivity(mapIntent)
                true
            } catch (_: Exception) {
                openInBrowser(context, uri)
            }
        }

        // 6. Generic external schemes (intent://, etc.)
        if (scheme != "http" && scheme != "https") {
            return try {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                false
            }
        }

        // 7. External HTTP/HTTPS websites (Facebook, YouTube, Instagram, LinkedIn, etc.)
        if (!isInternalUrl(url)) {
            return openInBrowser(context, uri)
        }

        return false
    }

    private fun openInBrowser(context: Context, uri: Uri): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.cannot_open_link), Toast.LENGTH_SHORT).show()
            false
        }
    }
}
