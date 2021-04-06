package com.example.cloudsecurity

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import com.example.Extentions.default
import com.example.Utils.GenericUtils.Companion.USER
import com.example.models.User
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import kotlinx.android.synthetic.main.acticity_home_admin.*


class HomeActivityForAdmin : BaseActivity() {

    var arrayList = ArrayList<User>()
    var userName = ArrayList<String>()
    lateinit var postListener : ValueEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acticity_home_admin)
        init()


    }



     fun getAllUsers() {
         arrayList.clear()
         userName.clear()
         mDatabase?.child("users")
         postListener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    it.children.forEach {
                        val mapper = ObjectMapper()
                        val user = mapper.convertValue(
                            it.value,
                            User::class.java
                        )
                        if (user.role.equals(USER)) {
                            arrayList.add(user)
                            userName.add(user.username.default())
                        }
                    }

                }

                val arrayAdapter = ArrayAdapter(
                    this@HomeActivityForAdmin,
                    android.R.layout.simple_list_item_1,
                    userName
                )
                adminList.adapter = arrayAdapter
                adminList.setOnItemClickListener(AdapterView.OnItemClickListener { adapterView, view, i, l ->
                    val intent = Intent(this@HomeActivityForAdmin, DetailScreenActivity::class.java)
                    val gson = Gson()
                    val myJson = gson.toJson(arrayList.get(i))
                    intent.putExtra("identifier", myJson)
                    startActivity(intent)

                })
            }
        }
         mDatabase?.addValueEventListener(postListener)
     }

    override fun onStop() {
        super.onStop()
        mDatabase?.removeEventListener(postListener)
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

    override fun onResume() {
        super.onResume()
        getAllUsers()
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