package com.example.cloudsecurity

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.kayvannj.permission_utils.Func
import com.github.kayvannj.permission_utils.PermissionUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlin.math.roundToInt


abstract class BaseActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener,
    LocationListener {

    protected var mDatabase: DatabaseReference? = null
    protected lateinit var firebaseAuth : FirebaseAuth
    protected lateinit var storageReference: StorageReference
    protected lateinit var firebaseStorage: FirebaseStorage
    protected lateinit var btn_log_out: TextView
     var progressDialog : ProgressDialog? = null

    protected val CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000
    protected var mGoogleApiClient: GoogleApiClient? = null
    protected var mLocationRequest: LocationRequest? = null
    protected var currentLatitude = 0.0
    protected var currentLongitude = 0.0

    protected val IBA_LATITUDE = 24.8673  // -> IBA
//    protected val IBA_LATITUDE = 24.8577 // -> Ceaseor tower
    protected val IBA_LONGITUDE = 67.0248 // -> IBA
//    protected val IBA_LONGITUDE = 67.0456 // -> Ceaseor tower


    var arrayOfPermissions = arrayOf(
        "Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION," +
                "Manifest.permission.\n" +
                "            ACCESS_FINE_LOCATION"
    )
    var mRequestObject = PermissionUtil.PermissionRequestObject(this, arrayOfPermissions)
    var REQUEST_CODE_STORAGE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeDB()
    }

    fun initializeDB() {
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()
        storageReference = firebaseStorage.reference

        mDatabase = FirebaseDatabase.getInstance()
            .getReferenceFromUrl("https://cloud-security-siem-solution-default-rtdb.firebaseio.com/")
    }

    fun showAlertDialog(message: String , positiveMessage : String, negativeMessage : String, positive : DialogInterface.OnClickListener , negative : DialogInterface.OnClickListener) {
        val builder1: AlertDialog.Builder = AlertDialog.Builder(this)
        builder1.setMessage(message)
        builder1.setCancelable(true)

        builder1.setPositiveButton(
            positiveMessage,
           positive)

        builder1.setNegativeButton(
            negativeMessage,
            negative)

        val alert11: AlertDialog = builder1.create()
        alert11.show()
    }

    protected fun showProgress() {
        progressDialog = ProgressDialog(this)
        progressDialog?.setMessage("loading");
        progressDialog?.setCancelable(false)
        progressDialog?.show();
    }

    protected fun dismissProgress() {
        progressDialog?.dismiss()
    }

    fun checkGPSEnabledOrNot() {
        val lm: LocationManager =
            this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gps_enabled = false
        var network_enabled = false

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: Exception) {
        }

        if (!gps_enabled && !network_enabled) {
            // notify user
            android.app.AlertDialog.Builder(this)
                .setMessage(R.string.gps_network_not_enabled)
                .setPositiveButton(
                    R.string.open_location_settings
                ) { _, _ ->
                    this.startActivity(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    )
                }
                .setNegativeButton(R.string.Cancel){ _, _ ->
                    finish()}
                .show()
        }
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
                        }
                    }).onAnyDenied(
                    object : Func() {
                        override fun call() {
                            showAlertDialog("Permission Denied","Ask Again", "Cancel",
                                DialogInterface.OnClickListener { _, i -> checkPermissions() } ,
                                DialogInterface.OnClickListener { _, i -> finish() })
                        }
                    })
                .ask(REQUEST_CODE_STORAGE) // REQUEST_CODE_STORAGE is what ever int you want (should be distinct)
    }

    fun getCurrentUserLocation() {
        mGoogleApiClient =
            GoogleApiClient.Builder(this) // The next two lines tell the new client that “this” current class will handle connection stuff
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this) //fourth line adds the LocationServices API endpoint from GooglePlayServices
                .addApi(LocationServices.API)
                .build()

        // Create the LocationRequest object

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(10 * 1000.toLong()) // 10 seconds, in milliseconds
            .setFastestInterval(1 * 1000.toLong()) // 1 second, in milliseconds

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        mRequestObject?.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun getGoogleClient() : GoogleApiClient?{
        return mGoogleApiClient;
    }

    fun getLocationRequest() : LocationRequest?{
        return mLocationRequest;
    }

    fun checkIfUserInsideOffice(latitude: Double, longitude: Double) : Boolean {
        val startPoint = Location("locationA")
        startPoint.latitude = latitude
        startPoint.longitude = longitude

        val endPoint = Location("locationA")
        endPoint.latitude = IBA_LATITUDE
        endPoint.longitude = IBA_LONGITUDE


        val distance: Double = startPoint.distanceTo(endPoint).toDouble()
        Toast.makeText(this, distance.roundToInt().toString() + " meters far from office", Toast.LENGTH_SHORT).show()
        if (distance.roundToInt() > 100) {
            return false
        }
        return true
    }

}