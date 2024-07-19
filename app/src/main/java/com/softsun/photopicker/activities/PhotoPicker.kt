package com.softsun.photopicker.activities

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.softsun.photopicker.adapters.AlbumListAdapter
import com.softsun.photopicker.adapters.ImagesListAdapter
import com.softsun.photopicker.R
import com.softsun.photopicker.databinding.ActivityPhotoPickerBinding
import com.softsun.photopicker.models.Album
import com.softsun.photopicker.models.MediaItem
import com.softsun.photopicker.recyclerview.AdaptiveSpacingItemDecoration
import com.softsun.photopicker.recyclerview.RecyclerViewPauseImageLoadOnScrollListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

class PhotoPicker : AppCompatActivity() {

    companion object {
        fun getIntent(context: Context, type: Type, maxCount: Int): Intent {
            val intent = Intent(context, PhotoPicker::class.java)
            intent.putExtra("type", type)
            intent.putExtra("maxCount", maxCount)
            return intent
        }

        private const val STORAGE_PERMISSION_CODE = 4894
        private const val CAMERA_PERMISSION_CODE = 122

        const val MEDIA = "picked_files"
    }

    enum class FileType {
        IMAGE,
        VIDEO
    }

    private lateinit var binding: ActivityPhotoPickerBinding

    private lateinit var albumListAdapter: AlbumListAdapter
    private lateinit var imagesListAdapter: ImagesListAdapter

    private lateinit var takeImageResultLauncher: ActivityResultLauncher<Uri>
    private lateinit var recordVideoResultLauncher: ActivityResultLauncher<Uri>
    private var cameraImageUri: Uri? = null
    private var recordVideoUri: Uri? = null

    private var selectedAlbumName = ""
    var maxCount = 1
    private lateinit var type: Type

    enum class Type {
        IMAGES,
        VIDEOS,
        BOTH
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        maxCount = intent.getIntExtra("maxCount", 1)
        type = intent.getSerializableExtra("type") as Type

        initResultLaunchers()
        setListeners()
        setImagesAdapter()
        setAlbumsAdapter()

        checkForStoragePermission()
    }

    private fun storagePermissionsGranted() {
        init()
    }

    private fun init() {
        val options = arrayOf("All", "Albums")
        val spinnerArrayAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        spinnerArrayAdapter.setDropDownViewResource(
            android.R.layout
                .simple_spinner_dropdown_item
        )

        binding.spinnerOptions.setAdapter(spinnerArrayAdapter)

        binding.spinnerOptions.setSelection(0)
    }

    private fun loadAll() {
        setDefaultTitle()

        when (type) {
            Type.IMAGES -> {
                loadAllImages()
            }

            Type.BOTH -> {
                loadAllMediaFiles()
            }

            Type.VIDEOS -> {
                loadAllVideos()
            }
        }
    }

    private fun loadAlbums() {
        when (type) {
            Type.IMAGES -> {
                loadImagesAlbums()
            }

            Type.VIDEOS -> {
                loadVideoAlbums()
            }

            Type.BOTH -> {
                loadAllAlbums()
            }
        }
    }

    private fun setListeners() {
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (imagesListAdapter.isSelectedList.contains(true)) {
                    imagesListAdapter.clearSelections()
                } else if (binding.spinnerOptions.selectedItemPosition == 0) {
                    finish()
                } else if (binding.rvImages.visibility == View.VISIBLE) {
                    rvAlbumsVisible()
                    setDefaultTitle()
                } else {
                    finish()
                }
            }
        })

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnSelect.setOnClickListener {
            giveResult(imagesListAdapter.selectedFilesList)
        }

        binding.spinnerOptions.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0 -> loadAll()
                    1 -> loadAlbums()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
    }

    private fun setDefaultTitle() {
        binding.tvTitle.text = getString(R.string.select)
    }

    private fun loadImagesAlbums() {
        progressBarVisible()

        CoroutineScope(Dispatchers.IO).launch {
            val projection = arrayOf(
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED
            )

            val images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val contentResolver = contentResolver
            val selection = MediaStore.Images.Media.MIME_TYPE + "=? OR " +
                    MediaStore.Images.Media.MIME_TYPE + "=? OR " +
                    MediaStore.Images.Media.MIME_TYPE + "=?"
            val selectionArgs = arrayOf("image/jpeg", "image/png", "image/gif")
            val orderBy = MediaStore.Images.Media.DATE_ADDED + " DESC"

            val cursor =
                contentResolver.query(images, projection, selection, selectionArgs, orderBy)

            if (cursor != null) {
                val albums: MutableMap<String, MutableList<Uri>> = HashMap()

                val bucketNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val imageIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

                while (cursor.moveToNext()) {
                    val bucketName = cursor.getString(bucketNameColumn)
                    val imageId = cursor.getLong(imageIdColumn)

                    val imageUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        imageId
                    )

                    bucketName?.let {
                        if (!albums.containsKey(it)) {
                            albums[it] = ArrayList()
                        }

                        albums[it]!!.add(imageUri)
                    }
                }
                cursor.close()

                val albumList = ArrayList<Album>()

                for (item in albums) {
                    val mediaItemsList = ArrayList<MediaItem>()

                    for (new in item.value) {
                        mediaItemsList.add(MediaItem(new, FileType.IMAGE, null))
                    }

                    albumList.add(
                        Album(
                            mediaItemsList,
                            item.key,
                            item.value.size.toString()
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    albumListAdapter.setData(albumList)
                    rvAlbumsVisible()
                }
            }
        }
    }

    private fun loadVideoAlbums() {
        progressBarVisible()

        CoroutineScope(Dispatchers.IO).launch {
            val projection = arrayOf(
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.BUCKET_ID,
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DURATION
            )

            val videos = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val contentResolver = contentResolver
            val selection = MediaStore.Video.Media.MIME_TYPE + "=?"
            val selectionArgs = arrayOf("video/mp4") // You can add more video types if needed
            val orderBy = MediaStore.Video.Media.DATE_ADDED + " DESC"

            val cursor =
                contentResolver.query(videos, projection, selection, selectionArgs, orderBy)

            if (cursor != null) {
                val videoAlbums: MutableMap<String, ArrayList<MediaItem>> = HashMap()

                val bucketNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val videoIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext()) {
                    val bucketName = cursor.getString(bucketNameColumn)
                    val videoId = cursor.getLong(videoIdColumn)

                    val videoUri =
                        ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            videoId
                        )

                    bucketName?.let {
                        if (!videoAlbums.containsKey(bucketName)) {
                            videoAlbums[bucketName] = ArrayList()
                        }

                        val duration = formatDuration(cursor.getLong(durationColumn))
                        videoAlbums[bucketName]!!.add(
                            MediaItem(
                                videoUri,
                                FileType.VIDEO,
                                duration
                            )
                        )
                    }
                }

                cursor.close()

                val albumList = ArrayList<Album>()

                for (item in videoAlbums) {
                    albumList.add(
                        Album(
                            item.value,
                            item.key,
                            item.value.size.toString()
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    albumListAdapter.setData(albumList)
                    rvAlbumsVisible()
                }
            }
        }
    }

    private fun loadAllImages() {
        progressBarVisible()

        CoroutineScope(Dispatchers.IO).launch {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_TAKEN
            )

            val images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val contentResolver = contentResolver
            val orderBy = MediaStore.Images.Media.DATE_TAKEN + " DESC"

            val cursor = contentResolver.query(images, projection, null, null, orderBy)

            if (cursor != null) {
                val imageUris: ArrayList<MediaItem> = ArrayList()

                val imageIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

                while (cursor.moveToNext()) {
                    val imageId = cursor.getLong(imageIdColumn)
                    val imageUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        imageId
                    )
                    imageUris.add(MediaItem(imageUri, FileType.IMAGE, null))
                }
                cursor.close()

                withContext(Dispatchers.Main) {
                    imagesListAdapter.showCameraButton = true
                    imagesListAdapter.showVideoButton = false
                    binding.rvImages.scrollToPosition(0)
                    imagesListAdapter.setData(imageUris) {
                        rvImagesVisible()
                    }
                }
            }
        }
    }

    private fun loadAllVideos() {
        progressBarVisible()

        CoroutineScope(Dispatchers.IO).launch {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATE_TAKEN,
                MediaStore.Video.Media.DURATION
            )

            val images = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val contentResolver = contentResolver
            val orderBy = MediaStore.Video.Media.DATE_TAKEN + " DESC"

            val cursor = contentResolver.query(images, projection, null, null, orderBy)

            if (cursor != null) {
                val imageUris: ArrayList<MediaItem> = ArrayList()

                val imageIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext()) {
                    val imageId = cursor.getLong(imageIdColumn)

                    val imageUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        imageId
                    )

                    val duration = formatDuration(cursor.getLong(durationColumn))
                    imageUris.add(MediaItem(imageUri, FileType.VIDEO, duration))
                }
                cursor.close()

                withContext(Dispatchers.Main) {
                    imagesListAdapter.showCameraButton = false
                    imagesListAdapter.showVideoButton = true

                    binding.rvImages.scrollToPosition(0)

                    imagesListAdapter.setData(imageUris) {
                        rvImagesVisible()
                    }
                }
            }
        }
    }

    private fun loadAllMediaFiles() {
        progressBarVisible()

        CoroutineScope(Dispatchers.IO).launch {
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.DATE_TAKEN,
                MediaStore.Video.Media.DURATION
            )

            val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)

            val sortOrder = "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC"

            val queryUri: Uri = MediaStore.Files.getContentUri("external")
            val contentResolver = contentResolver

            val cursor = contentResolver.query(queryUri, projection, selection, null, sortOrder)

            val mediaItems = ArrayList<MediaItem>()

            cursor?.use {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val typeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val mediaType = cursor.getInt(typeColumn)
                    val uri = ContentUris.withAppendedId(queryUri, id)

                    val mediaItem =
                        if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                            val duration = formatDuration(cursor.getLong(durationColumn))
                            MediaItem(uri, FileType.VIDEO, duration)
                        } else {
                            MediaItem(uri, FileType.IMAGE, null)
                        }

                    mediaItems.add(mediaItem)
                }
            }

            withContext(Dispatchers.Main) {
                imagesListAdapter.showCameraButton = true
                imagesListAdapter.showVideoButton = false

                binding.rvImages.scrollToPosition(0)

                imagesListAdapter.setData(mediaItems) {
                    rvImagesVisible()
                }
            }
        }
    }

    private fun loadAllAlbums() {
        progressBarVisible()

        CoroutineScope(Dispatchers.IO).launch {
            val projection = arrayOf(
                MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Files.FileColumns.BUCKET_ID,
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Video.Media.DURATION // Only valid for videos
            )

            val uri: Uri = MediaStore.Files.getContentUri("external")
            val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)

            val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            val contentResolver = contentResolver

            val cursor = contentResolver.query(uri, projection, selection, null, sortOrder)

            if (cursor != null) {
                val albums: MutableMap<String, ArrayList<MediaItem>> = HashMap()

                val bucketNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
                val mediaIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val mediaTypeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext()) {
                    val bucketName = cursor.getString(bucketNameColumn)
                    val mediaId = cursor.getLong(mediaIdColumn)
                    val mediaType = cursor.getInt(mediaTypeColumn)

                    val contentUri =
                        if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                            ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                mediaId
                            )
                        } else {
                            ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                mediaId
                            )
                        }

                    bucketName?.let {
                        if (!albums.containsKey(bucketName)) {
                            albums[bucketName] = ArrayList()
                        }

                        if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                            val duration = formatDuration(cursor.getLong(durationColumn))
                            albums[bucketName]!!.add(
                                MediaItem(
                                    contentUri,
                                    FileType.VIDEO,
                                    duration
                                )
                            )
                        } else {
                            albums[bucketName]!!.add(
                                MediaItem(
                                    contentUri,
                                    FileType.IMAGE,
                                    null
                                )
                            )
                        }
                    }
                }

                cursor.close()

                val albumList = ArrayList<Album>()

                for (item in albums) {
                    albumList.add(
                        Album(
                            item.value,
                            item.key,
                            item.value.size.toString()
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    albumListAdapter.setData(albumList)
                    rvAlbumsVisible()
                }
            }
        }
    }

    private fun setAlbumsAdapter() {
        albumListAdapter = AlbumListAdapter(this, object : AlbumListAdapter.OnAlbumClickListener {
            override fun onAlbumClick(album: Album) {
                selectedAlbumName = album.name
                updateTitle()
                progressBarVisible()

                imagesListAdapter.showCameraButton = false
                imagesListAdapter.showVideoButton = false

                binding.rvImages.scrollToPosition(0)

                imagesListAdapter.setData(album.files) {
                    rvImagesVisible()
                }
            }
        })

        binding.rvAlbums.apply {
            adapter = albumListAdapter
            layoutManager = GridLayoutManager(this@PhotoPicker, 2)
            addItemDecoration(AdaptiveSpacingItemDecoration(30, true))
            addOnScrollListener(
                RecyclerViewPauseImageLoadOnScrollListener.getPauseScrollListener(
                    this@PhotoPicker,
                    object : RecyclerView.OnScrollListener() {

                    })
            )
        }
    }

    private fun setImagesAdapter() {
        imagesListAdapter = ImagesListAdapter(this, R.layout.row_small_image)

        binding.rvImages.apply {
            adapter = imagesListAdapter
            layoutManager = GridLayoutManager(this@PhotoPicker, 3)
            addItemDecoration(AdaptiveSpacingItemDecoration(20, true))
            addOnScrollListener(
                RecyclerViewPauseImageLoadOnScrollListener.getPauseScrollListener(
                    this@PhotoPicker,
                    object : RecyclerView.OnScrollListener() {

                    })
            )
        }
    }

    fun updateTitle() {
        if (!imagesListAdapter.isSelectedList.contains(true)) {
            binding.tvTitle.text = selectedAlbumName
            binding.btnSelect.visibility = View.GONE
        } else {
            var count = 0

            CoroutineScope(Dispatchers.IO).launch {
                for (isSelected in imagesListAdapter.isSelectedList) {
                    if (isSelected) {
                        count++
                    }
                }

                withContext(Dispatchers.Main) {
//                    if (type == Type.IMAGES) {
//                        binding.tvTitle.text = String.format("%s/%s images", count, maxCount)
//                    } else {
//                        binding.tvTitle.text = String.format("%s/%s videos", count, maxCount)
//                    }

                    binding.tvTitle.text = String.format("%s/%s selected", count, maxCount)
                    binding.btnSelect.visibility = View.VISIBLE
                }
            }
        }
    }

    fun openCamera() {
        checkForCameraPermission()
    }

    private fun cameraPermissionGranted() {
        if (type == Type.VIDEOS) {
            val file = File(filesDir, "recordedVideos")
            recordVideoUri =
                FileProvider.getUriForFile(
                    this,
                    applicationContext.packageName + ".fileprovider",
                    file
                )

            recordVideoResultLauncher.launch(recordVideoUri!!)
        } else {
            val file = File(filesDir, "picFromCamera")
            cameraImageUri =
                FileProvider.getUriForFile(
                    this,
                    applicationContext.packageName + ".fileprovider",
                    file
                )

            takeImageResultLauncher.launch(cameraImageUri!!)
        }
    }

    private fun initResultLaunchers() {
        takeImageResultLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { isSuccess ->
            if (isSuccess) {
                cameraImageUri?.let {
                    val list = ArrayList<MediaItem>()
                    list.add(MediaItem(it, FileType.IMAGE, null))
                    giveResult(list)
                }
            }
        }

        recordVideoResultLauncher = registerForActivityResult(
            ActivityResultContracts.CaptureVideo()
        ) { isSuccess ->
            if (isSuccess) {
                recordVideoUri?.let {
                    val list = ArrayList<MediaItem>()

                    list.add(MediaItem(it, FileType.VIDEO, null))
                    giveResult(list)
                }
            }
        }
    }

    private fun progressBarVisible() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvAlbums.visibility = View.GONE
        binding.rvImages.visibility = View.GONE
    }

    private fun rvAlbumsVisible() {
        binding.progressBar.visibility = View.GONE
        binding.rvAlbums.visibility = View.VISIBLE
        binding.rvImages.visibility = View.GONE
    }

    private fun rvImagesVisible() {
        binding.progressBar.visibility = View.GONE
        binding.rvAlbums.visibility = View.GONE
        binding.rvImages.visibility = View.VISIBLE
    }

    fun giveResult(data: ArrayList<MediaItem>) {
        giveResultInner(data)
    }

    private fun giveResultInner(data: ArrayList<MediaItem>) {
//        for (item in data) {
//            if (item.type == FileType.IMAGE) {
//                println(File(item.uri.path).absolutePath)
//                val image = ImageCompressor.compressImage(this, File(item.uri.path!!))
//                if (image != null) {
//                    item.uri = image.toUri()
//                }
//
////                val image = Compressor.compress(this, File(item.uri.path!!))
//            }
//        }

        val intent = Intent()
        intent.putParcelableArrayListExtra(MEDIA, data)
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    storagePermissionsGranted()
                } else {
                    checkForStoragePermission()
                }
            }

            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraPermissionGranted()
                } else {
                    checkForCameraPermission()
                }
            }
        }
    }

    private fun checkForCameraPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionGranted()
            return
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this@PhotoPicker,
                Manifest.permission.CAMERA
            )
        ) {
            showCameraPermissionFinalDialog()
        } else {
            showCameraPermissionInformationDialog()
        }
    }

    private fun checkForStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                storagePermissionsGranted()
                return
            }

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@PhotoPicker,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) && ActivityCompat.shouldShowRequestPermissionRationale(
                    this@PhotoPicker,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            ) {
                showStoragePermissionFinalDialog()
            } else {
                showStoragePermissionInformationDialog()
            }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                storagePermissionsGranted()
                return
            }

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@PhotoPicker,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                showStoragePermissionFinalDialog()
            } else {
                showStoragePermissionInformationDialog()
            }
        }
    }

    private fun showStoragePermissionInformationDialog() {
        MaterialAlertDialogBuilder(this@PhotoPicker)
            .setMessage(getString(R.string.allow_storage_permission_to_select_photos_and_videos))
            .setPositiveButton(getString(R.string.continue_)) { _: DialogInterface?, _: Int ->
                askForStoragePermissions()
            }.setNegativeButton(
                getString(R.string.not_now)
            ) { _, _ ->
                finish()
            }
            .show()
    }

    private fun showStoragePermissionFinalDialog() {
        MaterialAlertDialogBuilder(this@PhotoPicker)
            .setMessage(getString(R.string.storage_permission_is_not_granted_please_go_to_settings_and_allow_storage_permission_to_select_images_and_videos))
            .setPositiveButton(getString(R.string.continue_)) { _: DialogInterface?, _: Int ->
                openAppSettings(true)
            }.setNegativeButton(
                getString(R.string.not_now)
            ) { _, _ ->
                finish()
            }
            .show()
    }

    private fun askForStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permis =
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)

            ActivityCompat.requestPermissions(
                this@PhotoPicker,
                permis,
                STORAGE_PERMISSION_CODE
            )
        } else {
            val permis = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this@PhotoPicker, permis, STORAGE_PERMISSION_CODE)
        }
    }

    private fun showCameraPermissionInformationDialog() {
        MaterialAlertDialogBuilder(this@PhotoPicker)
            .setMessage(getString(R.string.allow_camera_permission_to_take_photos_or_record_videos))
            .setPositiveButton(getString(R.string.continue_)) { _: DialogInterface?, _: Int ->
                askForCameraPermissions()
            }.setNegativeButton(
                getString(R.string.not_now), null
            )
            .show()
    }

    private fun showCameraPermissionFinalDialog() {
        MaterialAlertDialogBuilder(this@PhotoPicker)
            .setMessage(getString(R.string.camera_permission_is_not_granted_please_go_to_settings_and_allow_camera_permission_to_take_photos_or_record_videos))
            .setPositiveButton(R.string.continue_) { _: DialogInterface?, _: Int ->
                openAppSettings(false)
            }.setNegativeButton(
                R.string.not_now, null
            )
            .show()
    }

    private fun askForCameraPermissions() {
        val permis = arrayOf(Manifest.permission.CAMERA)
        ActivityCompat.requestPermissions(this@PhotoPicker, permis, CAMERA_PERMISSION_CODE)
    }

    private fun openAppSettings(finishAct: Boolean) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.setData(uri)
        startActivity(intent)
        if (finishAct) {
            finish()
        }
    }

    private fun formatDuration(milliseconds: Long): String {
        val seconds: Long = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
        val minutes: Long = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60

        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}