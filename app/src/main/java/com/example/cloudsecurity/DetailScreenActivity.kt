package com.example.cloudsecurity

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.example.Extentions.default
import com.example.models.User
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnFailureListener
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_detail_screen.*
import java.io.File
import java.io.IOException


class DetailScreenActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_screen)

        val gson = Gson()
        val user = gson.fromJson<User>(intent.getStringExtra("identifier"), User::class.java)

        checkUserStatus(user)
        setImage(user)

    }

    private fun setImage(user: User) {
        if (user.imagePath.default() != "") {
            try {
                val localFile = File.createTempFile("lastPathName", "jpg")
                storageReference.child("users").child(user.userID.default()).child("screenshot")
                    .child(user.imagePath.default())
                    .getFile(localFile)
                    .addOnSuccessListener {
                        Glide.with(getApplicationContext())
                            .load(localFile)
                            .into(screenshot)
                    }.addOnFailureListener(OnFailureListener {
                        if (it.message.equals("Object does not exist at location.")) {
                            imageNotFound.visibility = VISIBLE
                        }
                    })
            } catch (e: IOException) {
                e.toString()
            }
        }
        else {
            imageNotFound.visibility = VISIBLE
        }
    }


    private fun checkUserStatus(user: User) {
        if (user.blocked.default()) {
            status.text = "Blocked"
            status.setTextColor(resources.getColor(R.color.red))
            unblock.isEnabled = true
        }
        else {
            status.text = "Un Blocked"
            status.setTextColor(resources.getColor(R.color.green))
            unblock.isEnabled = false
        }

        unblock.setOnClickListener(View.OnClickListener {
            if (user.blocked.default()) {
                mDatabase?.child("users")?.child(user.userID.default())?.child("blocked")?.setValue(false)?.addOnSuccessListener {
                    Toast.makeText(applicationContext, "Unblocked Successful", Toast.LENGTH_SHORT)
                        .show()
                   finish()
                }?.addOnFailureListener {
                }
            }
        })
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
        }
    }


    override fun onConnectionSuspended(i: Int) {}

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
//            Toast.makeText(this@HomeActivityForAdmin,
//                "Location services connection failed with code " + connectionResult.errorCode, Toast.LENGTH_SHORT
//            ).show()
        }
    }

    /**
     * If locationChanges change lat and long
     *
     *
     * @param location
     */
    override fun onLocationChanged(location: Location) {
        currentLatitude = location.getLatitude()
        currentLongitude = location.getLongitude()
    }

}