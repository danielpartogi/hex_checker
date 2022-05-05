package com.example.heximagechecker

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isGone
import com.example.heximagechecker.data.AppDatabase
import com.example.heximagechecker.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.File


class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    val database by lazy { AppDatabase.getInstance(this) }

    val PERMISSIONS_MEDIA_STORAGE =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    private val fileChooserContract =
        registerForActivityResult(ActivityResultContracts.GetContent()) { imageUri ->
            if (imageUri != null) {
                // imageUri now contains URI to selected image
                checkHex(imageUri)
            }
        }

    val permissiongStorage: Int = 99

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupView()
        setupData()
    }


    private fun setupView() {
        binding.progressBar.isGone = true
        binding.btnCheckImage.setOnClickListener {
            //Runtime permission request required if Android permission >= Marshmallow
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                checkPermission()
            else
            //read my file in  /sdcard/test.csv
                readTheFiles();
        }
        binding.tvResult.isGone = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun pickImage() {
        fileChooserContract.launch("image/*")
    }

    private fun setupData() {
        AppDatabase.getInstance(this)

    }

    private fun checkHex(imageUri: Uri) = CoroutineScope(Dispatchers.IO).launch {
        val file = FileManager.getPath(this@MainActivity, imageUri)
        val hexList = database.userDao().getAllHex().map { it.hexaDecimal }
        if (File(file.orEmpty()).isFile) {
            val bytes = File(file!!).readBytes()
            runOnUiThread {
                binding.progressBar.isGone= false
                binding.btnCheckImage.isEnabled = false
                binding.btnCheckImage.isFocusable = false
                binding.tvResult.text = "Checking..."
                binding.tvResult.isGone = false
                binding.progressBar.max = bytes.count()
            }

            bytes.forEachIndexed { index, byte ->
                val stringHex = String.format("%02X", byte)
                val isIndicateContains = hexList.count { it.substring(0, 2) == stringHex } > 0

                if (isIndicateContains && isModified(bytes, index, hexList)) {
                    runOnUiThread {
                        binding.tvResult.text = "YOUR IMAGE IS MODIFIED"
                        binding.progressBar.progress = 0
                        binding.progressBar.isGone = true

                        binding.btnCheckImage.isEnabled = true
                        binding.btnCheckImage.isFocusable = true
                    }

                    return@launch
                }
                runOnUiThread {
                   binding.progressBar.progress = index
                }

            }
            runOnUiThread {
                binding.progressBar.progress = 0
                binding.progressBar.isGone = true
                binding.btnCheckImage.isEnabled = true
                binding.btnCheckImage.isFocusable = true
                binding.tvResult.text = "YOUR IMAGE IS ORIGINAL"
            }

        } else {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Something went wrong", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isModified(bytes: ByteArray, index: Int, hexList: List<String>): Boolean {

        val isModified = false

        if (index > bytes.count()) {
            return isModified
        }
        hexList.forEachIndexed { _, s ->

            val indexHex = s.split(" ").count()
            val currentByte =
                bytes.copyOfRange(index, index + indexHex).map { String.format("%02X", it) }
                    .joinToString(" ")
            if (s == currentByte) {
                return true
            }
        }

        return isModified

    }

    private fun readTheFiles() {
        pickImage()
    }

    private fun checkPermission() {

        if (EasyPermissions.hasPermissions(this, *PERMISSIONS_MEDIA_STORAGE)) {
            pickImage()
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, "We need your permission",
                permissiongStorage, *PERMISSIONS_MEDIA_STORAGE);
        }

    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {

        val permissionStorage = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (permissionStorage != PackageManager.PERMISSION_GRANTED) {
            val builder = AppSettingsDialog.Builder(this)
            builder.build().show()
        }

    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

        readTheFiles()
    }

}