package com.example.cloudsecurity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.Utils.GenericUtils;
import com.example.models.User;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends BaseActivity {

    EditText SignUpMail, SignUpPass, UserName;
    Button SignUpButton;
    private FirebaseAuth auth;
    private DatabaseReference mDatabase;
    RadioGroup roles;
    RadioButton admin, user;


    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        SignUpMail = findViewById(R.id.SignUpMail);
        SignUpPass = findViewById(R.id.SignUpPass);
        UserName = findViewById(R.id.UserName);
        roles = findViewById(R.id.roles);
        user = findViewById(R.id.user);
        admin = findViewById(R.id.admin);
        auth = FirebaseAuth.getInstance();
        SignUpButton = (Button) findViewById(R.id.SignUpButton);
        mDatabase = FirebaseDatabase.getInstance().getReferenceFromUrl("https://cloud-security-siem-solution-default-rtdb.firebaseio.com/");

        SignUpButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                final String email = SignUpMail.getText().toString();
                String pass = SignUpPass.getText().toString();
                final String userName = UserName.getText().toString();
                checkForRolesSelection();
                final String userRole = checkForRolesSelection();

                if (!GenericUtils.Companion.isInternetAvailable(SignUpActivity.this)) {
                    Toast.makeText(getApplicationContext(), "Check Your Internet First", Toast.LENGTH_LONG).show();
                    return;
                }

                if (TextUtils.isEmpty(userName)) {
                    Toast.makeText(getApplicationContext(), "Please enter your User Name", Toast.LENGTH_LONG).show();
                    return;
                }

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(getApplicationContext(), "Please enter your E-mail address", Toast.LENGTH_LONG).show();
                    return;
                }
                if (TextUtils.isEmpty(pass)) {
                    Toast.makeText(getApplicationContext(), "Please enter your Password", Toast.LENGTH_LONG).show();
                }
                if (pass.length() == 0) {
                    Toast.makeText(getApplicationContext(), "Please enter your Password", Toast.LENGTH_LONG).show();
                }
                if (pass.length() < 8) {
                    Toast.makeText(getApplicationContext(), "Password must be more than 8 digit", Toast.LENGTH_LONG).show();
                } else {
                    showProgress();
                    auth.createUserWithEmailAndPassword(email, pass)
                            .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    dismissProgress();
                                    if (task.getException() != null && task.getException().getMessage() != null) {
                                        if (task.getException().getMessage().equals("The email address is already in use by another account."))
                                            Toast.makeText(SignUpActivity.this, "User already exists", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    if (!task.isSuccessful()) {
                                        Toast.makeText(SignUpActivity.this, "ERROR", Toast.LENGTH_LONG).show();
                                    } else {
                                        writeNewUser(task.getResult().getUser().getUid(), userName, email, false,  userRole);
                                        Toast.makeText(SignUpActivity.this, "Your User has been Created", Toast.LENGTH_LONG).show();
                                        startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
                                        finishAffinity();
                                    }
                                }
                            });
                }
            }
        });
    }

    private String checkForRolesSelection() {
        int selectedId = roles.getCheckedRadioButtonId();

        // find which radioButton is checked by id
        if (selectedId == user.getId()) {
            return user.getText().toString();
        } else {
            return admin.getText().toString();
        }
    }

    public void navigate_sign_in(View v) {
        Intent inent = new Intent(this, SignInActivity.class);
        startActivity(inent);
        finish();
    }

    public void writeNewUser(String userId, String name, String email, Boolean blocked, String userRole) {
        User user = new User(userId, name, email, userRole, blocked, null, "");

        mDatabase.child("users").child(userId).setValue(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getApplicationContext(), "Write Successful", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Write Failure", Toast.LENGTH_SHORT).show();

                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getCurrentUserLocation();
        //Now lets connect to the API
        if (getGoogleClient() != null) {
            getGoogleClient().connect();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        //Disconnect from API onPause()
        if (getGoogleClient() != null) {
            if (getGoogleClient().isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(getGoogleClient(), this);
                getGoogleClient().disconnect();
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            checkPermissions();
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location =
                LocationServices.FusedLocationApi.getLastLocation(getGoogleClient());
        LocationServices.FusedLocationApi.requestLocationUpdates(
                getGoogleClient(),
                getLocationRequest(),
                this  );
        //If everything went fine lets get latitude and longitude
        setCurrentLatitude(location.getLatitude());
        setCurrentLongitude(location.getLongitude());
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
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
                        getCONNECTION_FAILURE_RESOLUTION_REQUEST()
                );
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch ( IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Toast.makeText(this,
                    "Location services connection failed with code " + connectionResult.getErrorCode(), Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        setCurrentLatitude(location.getLatitude());
        setCurrentLongitude(location.getLongitude());
    }
}