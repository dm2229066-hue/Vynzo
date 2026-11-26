package com.example.ui.create

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.media.MediaUtils
import com.example.ui.components.VynzoMediaImage
import com.example.ui.components.VynzoVideoPlayer
import com.example.ui.theme.LocalVynzoColors
import com.example.ui.viewmodel.VynzoSessionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateScreen(
  viewModel: VynzoSessionViewModel,
  initialMode: Int = 0, // 0 = Post, 1 = Reel, 2 = Story
  onPublishSuccess: (targetTab: String) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val colors = LocalVynzoColors.current
  val creators by viewModel.activeCreators.collectAsStateWithLifecycle()

  var publishMode by remember(initialMode) { mutableIntStateOf(initialMode) } // 0=Post, 1=Reel, 2=Story
  val selectedBitmaps = remember { mutableStateListOf<Bitmap>() }
  var selectedVideoUri by remember { mutableStateOf<String?>(null) }
  var selectedCoverDataUri by remember { mutableStateOf("") }
  var activePreviewIndex by remember { mutableIntStateOf(0) }
  var selectedAspectRatio by remember { mutableStateOf<Float?>(null) } // null = original
  var trimRange by remember { mutableStateOf(0f..1f) }

  var caption by remember { mutableStateOf("") }
  var hashtagsInput by remember { mutableStateOf("") }
  var locationInput by remember { mutableStateOf("") }
  var musicInput by remember { mutableStateOf("") }
  var commentsDisabled by remember { mutableStateOf(false) }
  var storyCloseFriendsOnly by remember { mutableStateOf(false) }
  var showTagDialog by remember { mutableStateOf(false) }
  val taggedUserIds = remember { mutableStateListOf<String>() }

  var isUploading by remember { mutableStateOf(false) }
  var uploadProgress by remember { mutableFloatStateOf(0f) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
  var pendingVideoUri by remember { mutableStateOf<Uri?>(null) }

  // 1. Multi-photo Android Photo Picker (Zero permissions!)
  val multiPhotoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(10)
  ) { uris ->
    if (uris.isNotEmpty()) {
      scope.launch {
        selectedVideoUri = null
        selectedBitmaps.clear()
        uris.forEach { uri ->
          MediaUtils.decodeBitmapFromUri(context, uri, maxDim = 1080)?.let { selectedBitmaps.add(it) }
        }
        activePreviewIndex = 0
      }
    }
  }

  // 2. Video Picker
  val videoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val persisted = MediaUtils.persistMediaFile(context, uri, "mp4")
        if (persisted != null) {
          selectedBitmaps.clear()
          selectedVideoUri = persisted
          if (publishMode == 0) publishMode = 1
        }
      }
    }
  }

  // 3. Cover Image Picker for Video/Reel
  val coverPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val bmp = MediaUtils.decodeBitmapFromUri(context, uri, maxDim = 720)
        if (bmp != null) {
          selectedCoverDataUri = MediaUtils.encodeBitmapToDataUri(bmp, maxBytes = 120_000)
        }
      }
    }
  }

  // 4. Camera Capture
  val takePhotoLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture()
  ) { success ->
    val uri = pendingCameraUri
    if (success && uri != null) {
      scope.launch {
        MediaUtils.decodeBitmapFromUri(context, uri, maxDim = 1080)?.let { bmp ->
          selectedVideoUri = null
          selectedBitmaps.add(bmp)
          activePreviewIndex = selectedBitmaps.lastIndex
        }
      }
    }
  }

  val captureVideoLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CaptureVideo()
  ) { success ->
    val uri = pendingVideoUri
    if (success && uri != null) {
      scope.launch {
        val persisted = MediaUtils.persistMediaFile(context, uri, "mp4")
        if (persisted != null) {
          selectedBitmaps.clear()
          selectedVideoUri = persisted
        }
      }
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      val (uri, _) = MediaUtils.createTempCameraUri(context, "vynzo_photo", "jpg")
      pendingCameraUri = uri
      takePhotoLauncher.launch(uri)
    } else {
      viewModel.notifyMessage("Camera permission denied. Enable it in Settings to capture photos.")
    }
  }

  val videoPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { grants ->
    if (grants[Manifest.permission.CAMERA] == true) {
      val (uri, _) = MediaUtils.createTempCameraUri(context, "vynzo_vid", "mp4")
      pendingVideoUri = uri
      captureVideoLauncher.launch(uri)
    } else {
      viewModel.notifyMessage("Camera permission is required to record video.")
    }
  }

  @SuppressLint("MissingPermission")
  fun fetchApproximateCity() {
    scope.launch {
      val city = withContext(Dispatchers.IO) {
        runCatching {
          val lm = context.getSystemService(LocationManager::class.java)
          val loc = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
          if (loc != null) {
            @Suppress("DEPRECATION")
            val list = Geocoder(context, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1)
            list?.firstOrNull()?.locality ?: list?.firstOrNull()?.adminArea
          } else null
        }.getOrNull()
      }
      if (!city.isNullOrBlank()) {
        locationInput = city
      } else {
        viewModel.notifyMessage("Location unavailable — enter your city manually.")
      }
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) fetchApproximateCity()
    else viewModel.notifyMessage("Location permission denied — you can type a location manually.")
  }

  fun publishContent(asDraft: Boolean) {
    val actor = viewModel.currentProfileOrNull()
    if (actor == null) {
      errorMsg = "Profile still loading. Please try again."
      return
    }
    if (selectedBitmaps.isEmpty() && selectedVideoUri == null) {
      errorMsg = "Select at least one photo or video to continue."
      return
    }
    errorMsg = null
    isUploading = true
    uploadProgress = 0.2f

    scope.launch {
      try {
        if (publishMode == 2) {
          // 24h Story
          val mediaType = if (selectedVideoUri != null) "video" else "image"
          val mediaUrl = if (selectedVideoUri != null) {
            selectedVideoUri!!
          } else {
            uploadProgress = 0.55f
            MediaUtils.encodeBitmapToDataUri(selectedBitmaps.first(), maxBytes = 155_000)
          }
          uploadProgress = 0.85f
          viewModel.storyRepository.createStory(
            author = actor,
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            caption = caption,
            visibility = if (storyCloseFriendsOnly) "close_friends" else "public"
          ).onSuccess {
            uploadProgress = 1f
            isUploading = false
            viewModel.notifyMessage("Story shared for 24 hours!")
            onPublishSuccess("home")
          }.onFailure {
            isUploading = false
            errorMsg = it.localizedMessage ?: "Story upload failed"
          }
        } else {
          // Post or Reel
          val mediaType: String
          val mediaUrls: List<String>
          if (selectedVideoUri != null) {
            mediaType = if (publishMode == 1) "reel" else "video"
            mediaUrls = listOf(selectedVideoUri!!)
            uploadProgress = 0.7f
          } else {
            mediaType = if (publishMode == 1) "reel" else if (selectedBitmaps.size > 1) "carousel" else "image"
            mediaUrls = selectedBitmaps.mapIndexed { idx, bmp ->
              uploadProgress = 0.2f + (0.55f * (idx + 1) / selectedBitmaps.size)
              MediaUtils.encodeBitmapToDataUri(bmp, maxBytes = 140_000)
            }
          }
          val parsedTags = (hashtagsInput.split(",", " ", "#") +
            caption.split(" ").filter { it.startsWith("#") }.map { it.removePrefix("#") })
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()

          uploadProgress = 0.9f
          viewModel.postRepository.createPost(
            author = actor,
            mediaType = mediaType,
            mediaUrls = mediaUrls,
            coverUrl = selectedCoverDataUri,
            caption = caption,
            hashtags = parsedTags,
            taggedUserIds = taggedUserIds.toList(),
            locationName = locationInput,
            musicTitle = musicInput,
            isDraft = asDraft,
            commentsDisabled = commentsDisabled
          ).onSuccess {
            uploadProgress = 1f
            isUploading = false
            viewModel.notifyMessage(
              when {
                asDraft -> "Saved to drafts"
                mediaType == "reel" -> "Reel published!"
                else -> "Post published!"
              }
            )
            onPublishSuccess(if (mediaType == "reel" && !asDraft) "reels" else "home")
          }.onFailure {
            isUploading = false
            errorMsg = it.localizedMessage ?: "Upload failed"
          }
        }
      } catch (e: Exception) {
        isUploading = false
        errorMsg = e.localizedMessage ?: "Upload error"
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 620.dp)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Text(
        text = "Create Studio",
        style = MaterialTheme.typography.headlineMedium
      )

      // Target Type Selector
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = publishMode == 0,
          onClick = { publishMode = 0 },
          label = { Text("Feed Post") },
          modifier = Modifier.testTag("create_mode_post")
        )
        FilterChip(
          selected = publishMode == 1,
          onClick = { publishMode = 1 },
          label = { Text("Vynzo Reel") },
          leadingIcon = { Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(16.dp)) },
          modifier = Modifier.testTag("create_mode_reel")
        )
        FilterChip(
          selected = publishMode == 2,
          onClick = { publishMode = 2 },
          label = { Text("24h Story") },
          leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
          modifier = Modifier.testTag("create_mode_story")
        )
      }

      // Media Pickers Row
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = {
            multiPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
          modifier = Modifier.testTag("pick_photos_button")
        ) {
          Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Gallery Photos")
        }
        OutlinedButton(
          onClick = {
            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
          },
          modifier = Modifier.testTag("pick_video_button")
        ) {
          Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Gallery Video")
        }
        OutlinedButton(
          onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
              val (uri, _) = MediaUtils.createTempCameraUri(context, "vynzo_photo", "jpg")
              pendingCameraUri = uri
              takePhotoLauncher.launch(uri)
            } else {
              cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
          },
          modifier = Modifier.testTag("camera_photo_button")
        ) {
          Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Camera")
        }
        OutlinedButton(
          onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
              val (uri, _) = MediaUtils.createTempCameraUri(context, "vynzo_vid", "mp4")
              pendingVideoUri = uri
              captureVideoLauncher.launch(uri)
            } else {
              videoPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            }
          }
        ) {
          Icon(Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Record Video")
        }
      }

      // Preview & Editing Stage
      if (selectedBitmaps.isNotEmpty()) {
        val currentBmp = selectedBitmaps.getOrNull(activePreviewIndex) ?: selectedBitmaps.first()
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(1.dp, colors.hairlineBorder)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(selectedAspectRatio ?: 1f)
                .clip(RoundedCornerShape(14.dp))
            ) {
              Image(
                bitmap = currentBmp.asImageBitmap(),
                contentDescription = "Selected photo preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
              IconButton(
                onClick = {
                  selectedBitmaps.removeAt(activePreviewIndex)
                  activePreviewIndex = (activePreviewIndex - 1).coerceAtLeast(0)
                },
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(6.dp)
                  .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(50))
              ) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove photo")
              }
            }

            Spacer(Modifier.height(10.dp))
            // Crop & Rotate Controls
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedButton(
                onClick = {
                  val rotated = MediaUtils.rotateBitmap(currentBmp, 90f)
                  selectedBitmaps[activePreviewIndex] = rotated
                }
              ) {
                Icon(Icons.AutoMirrored.Rounded.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Rotate")
              }
              listOf("1:1" to 1f, "4:5" to 0.8f, "16:9" to 1.777f).forEach { (label, ratio) ->
                FilterChip(
                  selected = selectedAspectRatio == ratio,
                  onClick = {
                    selectedAspectRatio = ratio
                    selectedBitmaps[activePreviewIndex] = MediaUtils.cropBitmapToAspect(currentBmp, ratio)
                  },
                  leadingIcon = { Icon(Icons.Rounded.Crop, contentDescription = null, modifier = Modifier.size(14.dp)) },
                  label = { Text(label) }
                )
              }
            }

            if (selectedBitmaps.size > 1) {
              Spacer(Modifier.height(8.dp))
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(selectedBitmaps) { idx, bmp ->
                  Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Slide ${idx + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                      .size(56.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .clickable { activePreviewIndex = idx }
                  )
                }
              }
            }
          }
        }
      } else if (selectedVideoUri != null) {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(1.dp, colors.hairlineBorder)
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 12f)
                .clip(RoundedCornerShape(14.dp))
            ) {
              VynzoVideoPlayer(
                videoUri = selectedVideoUri!!,
                isPlaying = true,
                isMuted = false,
                modifier = Modifier.fillMaxSize()
              )
            }
            Text("Video Trim Window: ${(trimRange.start * 100).toInt()}% – ${(trimRange.endInclusive * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
            RangeSlider(
              value = trimRange,
              onValueChange = { trimRange = it },
              valueRange = 0f..1f
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedButton(
                onClick = {
                  coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
              ) {
                Text(if (selectedCoverDataUri.isBlank()) "Select Cover Image" else "Change Cover")
              }
              if (selectedCoverDataUri.isNotBlank()) {
                VynzoMediaImage(
                  source = selectedCoverDataUri,
                  contentDescription = "Cover preview",
                  modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                )
              }
            }
          }
        }
      }

      // Caption & Metadata Fields
      OutlinedTextField(
        value = caption,
        onValueChange = { caption = it.take(2200) },
        label = { Text(if (publishMode == 2) "Story caption (optional)" else "Write a caption...") },
        maxLines = 4,
        modifier = Modifier.fillMaxWidth().testTag("create_caption_input")
      )

      if (publishMode != 2) {
        OutlinedTextField(
          value = hashtagsInput,
          onValueChange = { hashtagsInput = it.take(200) },
          label = { Text("Hashtags (e.g. vynzo, photography, sunset)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = locationInput,
            onValueChange = { locationInput = it.take(120) },
            label = { Text("Location") },
            leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedButton(
            onClick = {
              if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fetchApproximateCity()
              } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
              }
            }
          ) {
            Text("Detect")
          }
        }

        if (publishMode == 1 || selectedVideoUri != null) {
          OutlinedTextField(
            value = musicInput,
            onValueChange = { musicInput = it.take(120) },
            label = { Text("Audio / Music Track Title") },
            leadingIcon = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(onClick = { showTagDialog = true }) {
            Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (taggedUserIds.isEmpty()) "Tag People" else "Tagged (${taggedUserIds.size})")
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Turn off comments", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(6.dp))
            Switch(checked = commentsDisabled, onCheckedChange = { commentsDisabled = it })
          }
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Close Friends only", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = storyCloseFriendsOnly, onCheckedChange = { storyCloseFriendsOnly = it })
        }
      }

      if (isUploading) {
        LinearProgressIndicator(
          progress = { uploadProgress },
          modifier = Modifier.fillMaxWidth()
        )
      }

      if (errorMsg != null) {
        Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (publishMode != 2) {
          OutlinedButton(
            enabled = !isUploading,
            onClick = { publishContent(asDraft = true) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(52.dp).testTag("save_draft_button")
          ) {
            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Save Draft")
          }
        }
        Button(
          enabled = !isUploading,
          onClick = { publishContent(asDraft = false) },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).height(52.dp).testTag("publish_post_button")
        ) {
          Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(
            when (publishMode) {
              1 -> "Publish Reel"
              2 -> "Share Story"
              else -> "Publish Post"
            }
          )
        }
      }

      Spacer(Modifier.height(24.dp))
    }
  }

  if (showTagDialog) {
    val candidates = creators.filter { it.userId != viewModel.currentUserId }
    AlertDialog(
      onDismissRequest = { showTagDialog = false },
      title = { Text("Tag Creators") },
      text = {
        Column {
          if (candidates.isEmpty()) {
            Text("No other creators found yet.", style = MaterialTheme.typography.bodyMedium)
          } else {
            candidates.take(15).forEach { user ->
              val checked = user.userId in taggedUserIds
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    if (checked) taggedUserIds.remove(user.userId)
                    else taggedUserIds.add(user.userId)
                  }
                  .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = checked,
                  onCheckedChange = {
                    if (checked) taggedUserIds.remove(user.userId)
                    else taggedUserIds.add(user.userId)
                  }
                )
                Text("@${user.username} (${user.displayName})")
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showTagDialog = false }) { Text("Done") }
      }
    )
  }
}
