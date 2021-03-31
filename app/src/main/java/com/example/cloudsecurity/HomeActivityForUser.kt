package com.example.cloudsecurity

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.abangfadli.shotwatch.ScreenshotData
import com.abangfadli.shotwatch.ShotWatch
import com.example.Extentions.default
import com.example.models.User
import com.github.kayvannj.permission_utils.Func
import com.github.kayvannj.permission_utils.PermissionUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File


class HomeActivityForUser : AppCompatActivity() {

    private var mDatabase: DatabaseReference? = null
    private lateinit var firebaseAuth : FirebaseAuth
    private lateinit var storageReference: StorageReference
    private lateinit var firebaseStorage: FirebaseStorage


    var arrayOfPermissions = arrayOf(
        "Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION," +
                "Manifest.permission.\n" +
                "            ACCESS_FINE_LOCATION"
    )
    var mRequestObject = PermissionUtil.PermissionRequestObject(this, arrayOfPermissions)
    var REQUEST_CODE_STORAGE = 1
    var mShotWatch: ShotWatch? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initializeDB()
        checkPermissions()
        observeScreenShots()


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        mRequestObject?.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun navigateLogOut(view: View) {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }

    fun checkPermissions() {
        mRequestObject =
            PermissionUtil.with(this).request(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
                .onAllGranted(
                    object : Func() {
                        override fun call() {
                            //Happy Path
                            Toast.makeText(this@HomeActivityForUser, "Granted", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }).onAnyDenied(
                    object : Func() {
                        override fun call() {
                            Toast.makeText(this@HomeActivityForUser, "Denied", Toast.LENGTH_SHORT)
                                .show()
                            finish()
                            //Sad Path
                        }
                    })
                .ask(REQUEST_CODE_STORAGE) // REQUEST_CODE_STORAGE is what ever int you want (should be distinct)

    }

    // Register to begin receive event
    override fun onResume() {
        mShotWatch!!.register()
        super.onResume()
    }

    // Don't forget to unregister when apps goes to background
    override fun onPause() {
        mShotWatch!!.unregister()
        super.onPause()
    }

    fun observeScreenShots() {
        val listener = ShotWatch.Listener { screenshotData: ScreenshotData? ->
            writeNewUser(firebaseAuth.currentUser?.uid, true)

            val imageUri = Uri.fromFile(File(screenshotData?.path))
            val riversRef =
                imageUri.lastPathSegment?.let { lastPathName ->
                    firebaseAuth.uid?.let { userId ->
                        storageReference.child("users").child(userId).child("screenshot").child(
                            lastPathName
                        )
                    }
                }
            val uploadTask = riversRef?.putFile(imageUri)

// Register observers to listen for when the download is done or if it fails
            uploadTask?.addOnFailureListener {
                Toast.makeText(
                    applicationContext,
                    "Write Image Failure",
                    Toast.LENGTH_SHORT
                ).show()
            }?.addOnSuccessListener { taskSnapshot ->
                Toast.makeText(
                    applicationContext,
                    "Write Image Successful",
                    Toast.LENGTH_SHORT
                ).show()

            }
        }
        mShotWatch = ShotWatch(getContentResolver(), listener)
    }

    fun initializeDB() {
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()
        storageReference = firebaseStorage.reference

        mDatabase = FirebaseDatabase.getInstance()
            .getReferenceFromUrl("https://cloud-security-siem-solution-default-rtdb.firebaseio.com/")
    }

    fun writeNewUser( userId : String?,
        blocked: Boolean
    ) {
        val user = User(blocked = blocked)
        mDatabase!!.child("users").child(userId.default()).setValue(user)
            .addOnSuccessListener {
                Toast.makeText(applicationContext, "Write Successful", Toast.LENGTH_SHORT)
                    .show()
                startActivity(Intent(this, TemporaryBlockedActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener {
                Toast.makeText(applicationContext, "Write Failure", Toast.LENGTH_SHORT)
                    .show()
            }
    }

}