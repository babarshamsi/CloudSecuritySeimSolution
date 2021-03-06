package com.example.cloudsecurity

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.Extentions.default
import com.example.Utils.GenericUtils.Companion.ADMIN
import com.example.Utils.GenericUtils.Companion.isInternetAvailable
import com.example.models.User
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.database.FirebaseDatabase


class SplashActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        if (!isInternetAvailable(this)) {
            showAlertDialog("Check Your Internet First", "Okay", "Cancel",
                DialogInterface.OnClickListener { _, i -> finish() },
                DialogInterface.OnClickListener { _, i -> finish() })
            return
        }

        checkPermissions()
        checkGPSEnabledOrNot()

        FirebaseApp.initializeApp(this)
        //Check current user
        val firebaseAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()
            .getReferenceFromUrl("https://cloud-security-siem-solution-default-rtdb.firebaseio.com/")
    }

    override fun onResume() {
        super.onResume()
        getCurrentUserLocation()
        //Now lets connect to the API
        mGoogleApiClient!!.connect()
    }

    override fun onPause() {
        super.onPause()

        //Disconnect from API onPause()
        if (mGoogleApiClient!!.isConnected) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this)
            mGoogleApiClient!!.disconnect()
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
            firebaseAuth.addAuthStateListener(authStateListener)
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
            } catch (e: SendIntentException) {
                // Log the error
                e.printStackTrace()
            }
        } else {
            /*
                 * If no resolution is available, display a dialog to the
                 * user with the error.
                 */
            Toast.makeText(
                this@SplashActivity,
                "Location services connection failed with code " + connectionResult.errorCode,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLatitude = location.getLatitude()
        currentLongitude = location.getLongitude()
    }

    private var authStateListener =
        AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                val intent = Intent(this@SplashActivity, SignUpActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                mDatabase?.child("users")?.child(firebaseUser.uid)?.get()
                    ?.addOnSuccessListener { it ->
                        val mapper = ObjectMapper()
                        val currentUser = mapper.convertValue(it.value, User::class.java)
                        if (currentUser != null) {
                            if (currentUser.blocked.default()) {
                                val intent =
                                    Intent(
                                        this@SplashActivity,
                                        TemporaryBlockedActivity::class.java
                                    )
                                startActivity(intent)
                                finish()
                            } else {
                                checkIfUserIsAdmin(currentUser)
                            }
                        }
                        else {
                            startFromSigUpActivity()
                        }
                        }?.addOnFailureListener { it ->
                        Toast.makeText(this, "Unable to Fetch Value", Toast.LENGTH_SHORT).show()
                    }
            }
        }

    private fun checkIfUserIsAdmin(currentUser: User) {
        if (currentUser.role?.equals(ADMIN).default()) {
            val intent =
                Intent(this@SplashActivity, HomeActivityForAdmin::class.java)
            startActivity(intent)
            finish()
        } else {
            val intent =
                Intent(this@SplashActivity, HomeActivityForUser::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
       firebaseAuth.removeAuthStateListener(authStateListener)
    }

    private fun startFromSigUpActivity() {
        val intent =
            Intent(this@SplashActivity, SignUpActivity::class.java)
        startActivity(intent)
        finish()
    }
}