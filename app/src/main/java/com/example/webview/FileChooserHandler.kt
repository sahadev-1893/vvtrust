package com.example.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileChooserHandler(private val context: Context) {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraImageUri: Uri? = null

    /**
     * Prepares intent for file selection including Camera capture fallback.
     */
    fun startFileChooser(
        fileChooserLauncher: ActivityResultLauncher<Intent>,
        callback: ValueCallback<Array<Uri>>?,
        params: WebChromeClient.FileChooserParams?
    ): Boolean {
        // Clean up previous callback if any was pending
        filePathCallback?.onReceiveValue(null)
        filePathCallback = callback

        val acceptTypes = params?.acceptTypes?.filter { it.isNotBlank() } ?: emptyList()
        val isCapture = params?.isCaptureEnabled == true
        val allowsMultiple = params?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE

        var takePictureIntent: Intent? = null
        val allowsImages = acceptTypes.isEmpty() || acceptTypes.any { it.contains("image") }

        if (allowsImages) {
            val photoFile = createPhotoFile()
            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
            }
        }

        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            if (allowsMultiple) {
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            if (acceptTypes.size == 1) {
                type = acceptTypes[0]
            } else if (acceptTypes.size > 1) {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes.toTypedArray())
            } else {
                type = "*/*"
            }
        }

        val intentArray: Array<Intent> = if (takePictureIntent != null && !isCapture) {
            arrayOf(takePictureIntent)
        } else {
            emptyArray()
        }

        val chooserIntent = Intent(Intent.ACTION_CHOOSER).apply {
            putExtra(
                Intent.EXTRA_INTENT,
                if (isCapture && takePictureIntent != null) takePictureIntent else contentSelectionIntent
            )
            putExtra(Intent.EXTRA_TITLE, "Upload File")
            if (intentArray.isNotEmpty()) {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
            }
        }

        return try {
            fileChooserLauncher.launch(chooserIntent)
            true
        } catch (_: Exception) {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
            false
        }
    }

    fun onActivityResult(resultCode: Int, data: Intent?) {
        if (filePathCallback == null) return

        var results: Array<Uri>? = null

        if (resultCode == Activity.RESULT_OK) {
            if (data == null || data.data == null && data.clipData == null) {
                // If there is no data, then it might be from the camera
                if (cameraImageUri != null) {
                    results = arrayOf(cameraImageUri!!)
                }
            } else {
                val clipData = data.clipData
                if (clipData != null) {
                    val uriList = ArrayList<Uri>()
                    for (i in 0 until clipData.itemCount) {
                        uriList.add(clipData.getItemAt(i).uri)
                    }
                    results = uriList.toTypedArray()
                } else {
                    data.data?.let { uri ->
                        results = arrayOf(uri)
                    }
                }
            }
        }

        filePathCallback?.onReceiveValue(results)
        filePathCallback = null
        cameraImageUri = null
    }

    private fun createPhotoFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.cacheDir
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        } catch (_: Exception) {
            null
        }
    }
}
