package com.example.cloudsecurity

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.abangfadli.shotwatch.ScreenshotData
import com.abangfadli.shotwatch.ShotWatch
import com.example.Extentions.default
import com.example.Utils.GenericUtils
import com.example.models.User
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_home_user.*
import java.io.File


class HomeActivityForUser : BaseActivity() {

    protected lateinit var user: User
    var mShotWatch: ShotWatch? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_user)
        init()
        observeScreenShots()
        setUserData()

    }

    private fun init() {
        btn_log_out = findViewById(R.id.btn_log_out)
        btn_log_out.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this, SignInActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }
    }


    private fun setUserData() {
        mDatabase?.child("users")?.child(firebaseAuth.currentUser?.uid.default())?.get()
            ?.addOnSuccessListener { it ->
                val mapper = ObjectMapper()
                user = mapper.convertValue(it.value, User::class.java)
                user.apply {
                    UserName.text = "User Name : ${username}"
                    UserEmail.text = "User Email : ${email}"
                    UserRoles.text = "User Role : ${role}"
                }
            }?.addOnFailureListener { it ->
                Log.e(GenericUtils.APP_TAG,it.message.default())
            }

    }



    fun observeScreenShots() {
        val listener = ShotWatch.Listener { screenshotData: ScreenshotData? ->

            val imageUri = Uri.fromFile(File(screenshotData?.path))
            val riversRef =
                imageUri.lastPathSegment?.let { lastPathName ->
                    firebaseAuth.uid?.let { userId ->
                        storageReference.child("users").child(userId).child("screenshot").child(
                            lastPathName
                        )
                    }
                }
            writeNewUser(imageUri.lastPathSegment, firebaseAuth.currentUser?.uid.default(), true)
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



    private fun writeNewUser( imagePath: String? ,userId : String,
        blocked: Boolean
    ) {
        mDatabase?.child("users")?.child(userId)?.child("blocked")?.setValue(blocked)
//            ?.addOnSuccessListener {
//                Toast.makeText(applicationContext, "Write Successful", Toast.LENGTH_SHORT)
//                    .show()
//                startActivity(Intent(this, TemporaryBlockedActivity::class.java))
//                finishAffinity()
//            }?.addOnFailureListener {
//                Toast.makeText(applicationContext, "Write Failure", Toast.LENGTH_SHORT)
//                    .show()
//            }
        mDatabase?.child("users")?.child(userId)?.child("imagePath")?.setValue(imagePath)?.addOnSuccessListener {
            Toast.makeText(applicationContext, "Write Image Path", Toast.LENGTH_SHORT)
                .show()
            startActivity(Intent(this, TemporaryBlockedActivity::class.java))
            finishAffinity()
        }?.addOnFailureListener {
            Toast.makeText(applicationContext, "Write Image Path Failure", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onConnected(bundle: Bundle?) {
        val location: Location? =
            LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
        if (location == null) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                checkPermissions()
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient,
                mLocationRequest,
                this
            )
        } else {
            //If everything went fine lets get latitude and longitude
            currentLatitude = location.getLatitude()
            currentLongitude = location.getLongitude()
            if (checkIfUserInsideOffice(location.latitude, location.longitude)) {
                return
            }
            val intent = Intent(this, TemporaryBlockedActivity::class.java)
            intent.putExtra("TAG", GenericUtils.OUTSIDE_OFFICE_PREMISES)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }


    override fun onConnectionSuspended(i: Int) {
        var j =i
    }


    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        /*
             * Google Play services can resolve some errors it detects.
             * If the error has a resolution, try sending an Intent to
             * start a Google Play services activity that can resolve
             * error.
             */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST
                )
                /*
                     * Thrown if Google Play services canceled the original
                     * PendingIntent
                     */
            } catch (e: IntentSender.SendIntentException) {
                // Log the error
                e.printStackTrace()
            }
        } else {
            /*
                 * If no resolution is available, display a dialog to the
                 * user with the error.
                 */
            Toast.makeText(this@HomeActivityForUser,
                "Location services connection failed with code " + connectionResult.errorCode, Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        mShotWatch!!.register()
        getCurrentUserLocation()
        //Now lets connect to the API
        mGoogleApiClient!!.connect()
    }

    override fun onPause() {
        super.onPause()
        mShotWatch!!.unregister()
        //Disconnect from API onPause()
        if (mGoogleApiClient!!.isConnected) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this)
            mGoogleApiClient!!.disconnect()
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLatitude = location.getLatitude()
        currentLongitude = location.getLongitude()

        if (checkIfUserInsideOffice(location.latitude, location.longitude)) {
            return
        }
        val intent = Intent(this, TemporaryBlockedActivity::class.java)
        intent.putExtra("TAG", GenericUtils.OUTSIDE_OFFICE_PREMISES)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

}