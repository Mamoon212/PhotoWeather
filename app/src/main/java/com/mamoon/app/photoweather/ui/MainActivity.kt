package com.mamoon.app.photoweather.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.mamoon.app.photoweather.R
import com.mamoon.app.photoweather.adapters.PostsRecyclerViewAdapter
import com.mamoon.app.photoweather.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException

const val MY_CAMERA_PERMISSION_CODE: Int = 0
const val MY_STORAGE_PERMISSION_CODE: Int = 1
const val MY_FINE_LOCATION_PERMISSION_CODE: Int = 2
const val MY_CAMERA_CODE: Int = 3

class MainActivity : AppCompatActivity() {
    private lateinit var mainActivityViewModel: MainActivityViewModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var imageUri: Uri
    private lateinit var adapter: PostsRecyclerViewAdapter
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_main
        )
        adapter = PostsRecyclerViewAdapter(PostsRecyclerViewAdapter.OnClickListener {
            mainActivityViewModel.reOpenPhoto(it)
        })
        mainActivityViewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)
        binding.lifecycleOwner = this
        binding.viewModel = mainActivityViewModel
        binding.rvHistory.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        mainActivityViewModel.startPhotoActivity.observe(this, Observer {
            it?.let {
                if (it) {
                    startPhotoActivity()
                    mainActivityViewModel.doneStartingActivity()
                }
            }
        })

        mainActivityViewModel.showErrorMessage.observe(this, Observer {
            it?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                mainActivityViewModel.doneShowingErrorToast()
            }
        })

        mainActivityViewModel.startShareIntent.observe(this, Observer {
            it?.let {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_STREAM, it)
                startActivity(Intent.createChooser(intent, "Share image using:"))
                mainActivityViewModel.doneSharing()
            }
        })

    }

    //check for permissions and start the camera
    private fun startPhotoActivity() {
        if (checkPermissions()) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            imageUri = FileProvider.getUriForFile(
                applicationContext, "$packageName.fileprovider",
                File(
                    this.externalCacheDir.toString(),
                    "fname_" + System.currentTimeMillis().toString() + ".jpg"
                )
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, MY_CAMERA_CODE)
        }
    }

    //checking all required permissions
    private fun checkPermissions(): Boolean {
        return checkCameraPermission() &&
                checkStoragePermission() &&
                checkFineLocationPermission()
    }

    //if photo is taken successfully make an editable copy of it and pass it to the viewModel for processing and editing
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MY_CAMERA_CODE && resultCode == Activity.RESULT_OK) {
            uiScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val imageBitmap =
                            ImageDecoder.decodeBitmap(
                                ImageDecoder.createSource(
                                    contentResolver,
                                    imageUri
                                )
                            )
                        val mutableBitmap: Bitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true)
                        mainActivityViewModel.preparePhotoForDrawing(mutableBitmap)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun checkCameraPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                MY_CAMERA_PERMISSION_CODE
            )
            false
        } else {
            true
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                MY_STORAGE_PERMISSION_CODE
            )
            false
        } else {
            true
        }
    }

    private fun checkFineLocationPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_FINE_LOCATION_PERMISSION_CODE
            )
            false
        } else {
            true
        }
    }

    //if user chooses "don't ask again" instruct them to allow permissions through the settings page
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_CAMERA_PERMISSION_CODE -> {
                if (grantResults.isEmpty()
                    || grantResults[0] != PackageManager.PERMISSION_GRANTED
                ) {
                    if (!this.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        Toast.makeText(
                            this,
                            "The application is missing core permissions, please allow them from settings page.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "The applications needs these permissions to work.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    checkStoragePermission()
                }
            }

            MY_STORAGE_PERMISSION_CODE -> {
                if (grantResults.isEmpty()
                    || grantResults[0] != PackageManager.PERMISSION_GRANTED
                ) {
                    if (!this.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Toast.makeText(
                            this,
                            "The application is missing core permissions, please allow them from settings page.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "The applications needs these permissions to work.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    checkFineLocationPermission()
                }
            }

            MY_FINE_LOCATION_PERMISSION_CODE -> {
                if (grantResults.isEmpty()
                    || grantResults[0] != PackageManager.PERMISSION_GRANTED
                ) {
                    if (!this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        Toast.makeText(
                            this,
                            "The application is missing core permissions, please allow them from settings page.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "The applications needs these permissions to work.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
}
