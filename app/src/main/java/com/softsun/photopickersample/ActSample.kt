package com.softsun.photopickersample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.softsun.photopicker.activities.PhotoPicker
import com.softsun.photopicker.models.MediaItemModel

class ActSample : AppCompatActivity() {

    private lateinit var btnLaunchPicker: Button
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sample)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnLaunchPicker = findViewById(R.id.btnLaunchPicker)
        initResultLaunchers()

        btnLaunchPicker.setOnClickListener {
            imagePickerLauncher.launch(
                PhotoPicker.getIntent(this, PhotoPicker.Type.BOTH, 10)
            )
        }
    }

    private fun initResultLaunchers() {
        imagePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.data != null) {
                    val items : ArrayList<MediaItemModel> = result.data!!.getParcelableArrayListExtra(
                        PhotoPicker.MEDIA)!!

                    // TODO: Do Anything with result :)
                    Log.d("PickedMediaItems", items.toString())
                }
            }
    }
}