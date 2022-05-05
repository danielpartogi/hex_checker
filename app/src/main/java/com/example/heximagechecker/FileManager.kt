package com.example.heximagechecker

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.TextUtils
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class MemoryUnitType(val basis: Long) {
    BYTE(1L),
    KB(1024L),
    MB(1024L * 1024L),
    GB(1024L * 1024L * 1024L),
    TB(1024L * 1024L * 1024L * 1024L);

    companion object {
        fun getBasis(unit: MemoryUnitType) = values().firstOrNull { it == unit }
    }
}

@SuppressLint("Recycle")
@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")
object FileManager {

    fun getPath(context: Context, uri: Uri?): String? {
        // check here to KITKAT or new version
        val selection: String?
        val selectionArgs: Array<String>?
        val validUri: Uri = uri ?: return null
        // ExternalStorageProvider
        if (isExternalStorageDocument(validUri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).toTypedArray()
            //val type = split[0]
            val fullPath = getPathFromExtSD(split)
            return if (fullPath !== "") {
                fullPath
            } else {
                null
            }
        }


        // DownloadsProvider
        if (isDownloadsDocument(validUri)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                var cursor: Cursor? = null
                try {
                    cursor = context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
                    if (cursor != null && cursor.moveToFirst()) {
                        val fileName: String = cursor.getString(0)
                        val path: String = Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName
                        if (!TextUtils.isEmpty(path)) {
                            return path
                        }
                    }
                } finally {
                    cursor?.close()
                }
                val id: String = DocumentsContract.getDocumentId(uri)
                if (!TextUtils.isEmpty(id)) {
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:".toRegex(), "")
                    }
                    val contentUriPrefixesToTry = arrayOf("content://downloads/public_downloads", "content://downloads/my_downloads")
                    for (contentUriPrefix in contentUriPrefixesToTry) {
                        return try {
                            val contentUri: Uri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), java.lang.Long.valueOf(id))
                            getDataColumn(context, contentUri, null, null)
                        } catch (e: NumberFormatException) {
                            //In Android 8 and Android P the id is not a number
                            uri.path?.replaceFirst("^/document/raw:", "")?.replaceFirst("^raw:", "")
                        }
                    }
                }
            } else {
                val id = DocumentsContract.getDocumentId(uri)
                if (id.startsWith("raw:")) {
                    return id.replaceFirst("raw:".toRegex(), "")
                }
                var contentUri : Uri?= null
                try {
                    contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }
                if (contentUri != null) {
                    return getDataColumn(context, contentUri, null, null)
                }
            }
        }


        // MediaProvider
        if (isMediaDocument(validUri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).toTypedArray()
            val type = split[0]
            var contentUri: Uri? = null
            when (type) {
                "image" -> {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                "video" -> {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
                "audio" -> {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
            }
            selection = "_id=?"
            selectionArgs = arrayOf(split[1])
            contentUri?.let {
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        }
        if (isGoogleDriveUri(uri)) {
            return getDriveFilePath(context,uri)
        }
        if (isWhatsAppFile(uri)) {
            return getFilePathForWhatsApp(context, uri)
        }
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            if (isGooglePhotosUri(uri)) {
                return uri.lastPathSegment
            }
            if (isGoogleDriveUri(uri)) {
                return getDriveFilePath(context, uri)
            }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                // return getFilePathFromURI(context,uri);
                copyFileToInternalStorage(context, uri, "userfiles")
                // return getRealPathFromURI(context,uri);
            } else {
                getDataColumn(context, uri, null, null)
            }
        }
        if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    fun isContentExist(context: Context, uri: Uri?): Boolean {
        if (uri == null) return false
        val resolver = context.contentResolver

        // 1. Check Uri
        var cursor: Cursor? = null
        val isUriExist: Boolean = try {
            cursor = resolver.query(uri, null, null, null, null)
            // cursor null: content Uri was invalid or some other error occurred
            // cursor.moveToFirst() false: Uri was ok but no entry found.
            (cursor != null && cursor.moveToFirst())
        } catch (t: Throwable) {
            false
        } finally {
            try {
                cursor?.close()
            } catch (t: Throwable) {
            }
        }

        // 2. Check File Exist
        // If the system db has Uri related records, but the file is invalid or damaged
        var ins: InputStream? = null
        val isFileExist: Boolean = try {
            ins = resolver.openInputStream(uri)
            // file exists
            true
        } catch (t: Throwable) {
            // File was not found eg: open failed: ENOENT (No such file or directory)
            false
        } finally {
            try {
                ins?.close()
            } catch (t: Throwable) {

            }
        }

        return isUriExist && isFileExist
    }

    fun getFileSize(localPath: String, unit: MemoryUnitType = MemoryUnitType.MB): Float {
        return File(localPath).length().toFloat() / unit.basis
    }

    fun fileExists(filePath: String): Boolean {
        val file = File(filePath)
        return file.exists()
    }

    private fun getPathFromExtSD(pathData: Array<String>): String {
        val type = pathData[0]
        val relativePath = "/" + pathData[1]
        var fullPath : String

        // on my Sony devices (4.4.4 & 5.1.1), `type` is a dynamic string
        // something like "71F8-2C0A", some kind of unique id per storage
        // don't know any API that can get the root path of that storage based on its id.
        //
        // so no "primary" type, but let the check here for other devices
        if ("primary".equals(type, ignoreCase = true)) {
            fullPath = Environment.getExternalStorageDirectory().toString() + relativePath
            if (fileExists(fullPath)) {
                return fullPath
            }
        }

        // Environment.isExternalStorageRemovable() is `true` for external and internal storage
        // so we cannot relay on it.
        //
        // instead, for each possible path, check if file exists
        // we'll start with secondary storage as this could be our (physically) removable sd card
        fullPath = System.getenv("SECONDARY_STORAGE") + relativePath
        if (fileExists(fullPath)) {
            return fullPath
        }
        fullPath = System.getenv("EXTERNAL_STORAGE") + relativePath
        return if (fileExists(fullPath)) {
            fullPath
        } else fullPath
    }

    private fun getDriveFilePath(context: Context, uri: Uri): String? {
        val returnUri: Uri = uri
        val returnCursor: Cursor = context.contentResolver.query(returnUri, null, null, null, null)?: return null
        /*
     * Get the column indexes of the data in the Cursor,
     *     * move to the first row in the Cursor, get the data,
     *     * and display it.
     * */
        val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        // val sizeIndex: Int = returnCursor.getColumnIndex(OpenableColumns.SIZE)
        returnCursor.moveToFirst()
        val name: String = returnCursor.getString(nameIndex)
        // val size = returnCursor.getLong(sizeIndex).toString()
        val file = File(context.cacheDir, name)
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            var read : Int
            val maxBufferSize = 1 * 1024 * 1024
            val bytesAvailable: Int = inputStream?.available() ?: return null

            //int bufferSize = 1024;
            val bufferSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Integer.min(bytesAvailable, maxBufferSize)
            } else {
                1024
            }
            val buffers = ByteArray(bufferSize)
            while (inputStream.read(buffers).also { read = it } != -1) {
                outputStream.write(buffers, 0, read)
            }
            inputStream.close()
            outputStream.close()
        } catch (e: Exception) {
        }
        return file.path
    }

    /***
     * Used for Android Q+
     * @param uri
     * @param newDirName if you want to create a directory, you can set this variable
     * @return
     */
    private fun copyFileToInternalStorage(context: Context, uri: Uri, newDirName: String): String? {
        val returnUri: Uri = uri
        val returnCursor: Cursor = context.contentResolver.query(returnUri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?: return null

        /*
     * Get the column indexes of the data in the Cursor,
     *     * move to the first row in the Cursor, get the data,
     *     * and display it.
     * */
        val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        //val sizeIndex: Int = returnCursor.getColumnIndex(OpenableColumns.SIZE)
        returnCursor.moveToFirst()
        val name: String = returnCursor.getString(nameIndex)
        // val size = returnCursor.getLong(sizeIndex).toString()
        val output: File = if (newDirName != "") {
            val dir = File(context.filesDir.toString() + "/" + newDirName)
            if (!dir.exists()) {
                dir.mkdir()
            }
            File(context.filesDir.toString() + "/" + newDirName + "/" + name)
        } else {
            File(context.filesDir.toString() + "/" + name)
        }
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(output)
            var read = 0
            val bufferSize = 1024
            val buffers = ByteArray(bufferSize)
            while (inputStream?.read(buffers).also {
                    if (it != null) {
                        read = it
                    }
                } != -1) {
                outputStream.write(buffers, 0, read)
            }
            inputStream?.close()
            outputStream.close()
        } catch (e: Exception) {
        }
        return output.path
    }

    fun saveMediaToStorage(context: Context, bitmap: Bitmap, filename: String) {
        var fos: OutputStream? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
    }

    fun createImageUriFromBitmap(context: Context, bitmap: Bitmap): Uri? {
        val name = "stockbit_img_" + System.currentTimeMillis()
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, name)
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpeg")
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

        val imageUri: Uri? = context.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val fos: OutputStream? = imageUri?.let { context.contentResolver?.openOutputStream(it) }
        fos?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        fos?.close()
        return imageUri
    }


    private fun getFilePathForWhatsApp(context: Context, uri: Uri): String? {
        return copyFileToInternalStorage(context, uri, "whatsapp")
    }

    private fun getDataColumn(context: Context, uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index: Int = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    private fun isWhatsAppFile(uri: Uri): Boolean {
        return "com.whatsapp.provider.media" == uri.authority
    }

    private fun isGoogleDriveUri(uri: Uri): Boolean {
        return "com.google.android.apps.docs.storage" == uri.authority || "com.google.android.apps.docs.storage.legacy" == uri.authority
    }
}
