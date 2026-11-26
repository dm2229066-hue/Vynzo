package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

object MediaUtils {

  fun createTempCameraUri(context: Context, prefix: String = "vynzo_cam", ext: String = "jpg"): Pair<Uri, File> {
    val file = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.$ext")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return uri to file
  }

  suspend fun persistMediaFile(context: Context, sourceUri: Uri, extension: String = "mp4"): String? =
    withContext(Dispatchers.IO) {
      runCatching {
        val dir = File(context.filesDir, "media").apply { mkdirs() }
        val destFile = File(dir, "media_${UUID.randomUUID()}.$extension")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
          destFile.outputStream().use { output -> input.copyTo(output) }
        } ?: return@withContext null
        Uri.fromFile(destFile).toString()
      }.getOrNull()
    }

  suspend fun decodeBitmapFromUri(context: Context, uri: Uri, maxDim: Int = 1080): Bitmap? =
    withContext(Dispatchers.IO) {
      runCatching {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }
        var sample = 1
        while (boundsOpts.outWidth / sample > maxDim || boundsOpts.outHeight / sample > maxDim) {
          sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use {
          BitmapFactory.decodeStream(it, null, decodeOpts)
        }
      }.getOrNull()
    }

  fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    if (degrees % 360f == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }

  fun cropBitmapToAspect(bitmap: Bitmap, aspectRatio: Float?): Bitmap {
    if (aspectRatio == null) return bitmap
    val srcRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    return if (srcRatio > aspectRatio) {
      val targetW = (bitmap.height * aspectRatio).toInt().coerceAtLeast(1)
      val x = ((bitmap.width - targetW) / 2).coerceAtLeast(0)
      Bitmap.createBitmap(bitmap, x, 0, targetW, bitmap.height)
    } else {
      val targetH = (bitmap.width / aspectRatio).toInt().coerceAtLeast(1)
      val y = ((bitmap.height - targetH) / 2).coerceAtLeast(0)
      Bitmap.createBitmap(bitmap, 0, y, bitmap.width, targetH)
    }
  }

  /**
   * Encodes a bitmap into a compact JPEG data URI (<= ~150KB) so photos sync
   * seamlessly through Cloud Firestore across devices and sessions.
   */
  suspend fun encodeBitmapToDataUri(bitmap: Bitmap, maxBytes: Int = 155_000): String =
    withContext(Dispatchers.Default) {
      val scaled = scaleDown(bitmap, 720)
      var quality = 78
      var bytes: ByteArray
      do {
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        bytes = baos.toByteArray()
        quality -= 10
      } while (bytes.size > maxBytes && quality >= 28)

      val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
      "data:image/jpeg;base64,$b64"
    }

  private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxEdge && h <= maxEdge) return bitmap
    val scale = maxEdge.toFloat() / maxOf(w, h).toFloat()
    return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1), true)
  }

  fun decodeDataUriBitmap(dataUri: String): Bitmap? {
    return runCatching {
      val base64Part = dataUri.substringAfter("base64,", "")
      if (base64Part.isBlank()) return null
      val bytes = Base64.decode(base64Part, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
  }
}

class VoiceNoteRecorder(private val context: Context) {
  private var recorder: MediaRecorder? = null
  private var outputFile: File? = null

  fun startRecording(): Boolean {
    return runCatching {
      val dir = File(context.filesDir, "media").apply { mkdirs() }
      val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
      outputFile = file
      val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
      } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
      }
      mr.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setAudioEncodingBitRate(64_000)
        setAudioSamplingRate(22_050)
        setOutputFile(file.absolutePath)
        prepare()
        start()
      }
      recorder = mr
      true
    }.getOrElse {
      stopAndRelease()
      false
    }
  }

  fun stopRecording(): String? {
    return runCatching {
      recorder?.apply {
        stop()
        release()
      }
      recorder = null
      outputFile?.let { if (it.exists() && it.length() > 0) Uri.fromFile(it).toString() else null }
    }.getOrElse {
      stopAndRelease()
      null
    }
  }

  private fun stopAndRelease() {
    runCatching { recorder?.release() }
    recorder = null
  }
}

class VoiceNotePlayer {
  private var player: MediaPlayer? = null

  fun play(uriString: String, onCompleted: () -> Unit) {
    stop()
    runCatching {
      val path = Uri.parse(uriString).path ?: return
      player = MediaPlayer().apply {
        setDataSource(path)
        setOnCompletionListener {
          onCompleted()
          stop()
        }
        prepare()
        start()
      }
    }.onFailure { onCompleted() }
  }

  fun stop() {
    runCatching {
      player?.stop()
      player?.release()
    }
    player = null
  }
}
