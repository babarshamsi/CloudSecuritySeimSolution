package com.example.cloudsecurity;

import android.Manifest;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static com.example.Utils.GenericUtils.ADMIN;

public class SignInActivity extends BaseActivity {

    private EditText SignInMail, SignInPass;
    private FirebaseAuth auth;
    private Button SignInButton;
    private DatabaseReference mDatabase;
    RadioGroup roles;
    RadioButton admin, user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReferenceFromUrl("https://cloud-security-siem-solution-default-rtdb.firebaseio.com/");
        // set the view now
        setContentView(R.layout.activity_signin);
        SignInMail = (EditText) findViewById(R.id.SignInMail);
        SignInPass = (EditText) findViewById(R.id.SignInPass);
        SignInButton = (Button) findViewById(R.id.SignInButton);
        roles = findViewById(R.id.roles);
        user = findViewById(R.id.user);
        admin = findViewById(R.id.admin);
        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();

        SignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = SignInMail.getText().toString();
                final String password = SignInPass.getText().toString();
                final String userRole = checkForRolesSelection();

                if (!GenericUtils.Companion.isInternetAvailable(SignInActivity.this)) {
                    Toast.makeText(getApplicationContext(), "Check Your Internet First", Toast.LENGTH_LONG).show();
                    return;
                }

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(getApplicationContext(), "Enter your mail address", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password) && password.length() < 8) {
                    Toast.makeText(getApplicationContext(), "At least 8 character required for password", Toast.LENGTH_SHORT).show();
                    return;
                }
                //authenticate user
                showProgress();
                auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(SignInActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (!task.isSuccessful()) {
                                    dismissProgress();
                                    if (task.getException() != null && task.getException().getMessage() != null) {
                                        String errorMessage = task.getException().getMessage();
                                        if (errorMessage.equals("There is no user record corresponding to this identifier. The user may have been deleted.")) {
                                            Toast.makeText(getApplicationContext(), "User not found", Toast.LENGTH_SHORT).show();
                                        } else if (errorMessage.equals("The password is invalid or the user does not have a password.")) {
                                            Toast.makeText(getApplicationContext(), "User Email or Password is Wrong", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                } else {
                                    getUserData(task.getResult().getUser(), task.getResult().getUser().getUid());
                                }
                            }
                        });
            }
        });
    }

    public void NavigateSignUp(View v) {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
        finish();
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


    public void writeNewUser(final User user) {
        if (user!= null && user.getUserID() != null) {
            mDatabase.child("users").child(user.getUserID()).setValue(user)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            checkIfUserIsAdmin(user);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            dismissProgress();
                        }
                    });
        }

    }

    private void checkIfUserIsAdmin( User currentUser) {
        dismissProgress();
        if (currentUser != null && currentUser.getRole() != null && currentUser.getBlocked() != null) {
            if (currentUser.getRole().equals(ADMIN)) {
                Intent intent =
                        new Intent(this, HomeActivityForAdmin.class);
                startActivity(intent);
                finish();
            } else {
                if (currentUser.getBlocked()) {
                    Intent intent =
                            new Intent(this, TemporaryBlockedActivity.class);
                    startActivity(intent);
                    finish();
                }
                else {
                    Intent intent =
                            new Intent(this, HomeActivityForUser.class);
                    startActivity(intent);
                    finish();
                }
            }
        }
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

    private void getUserData(FirebaseUser user, String userID) {
        mDatabase.child("users").child(userID).get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
            @Override
            public void onSuccess(DataSnapshot dataSnapshot) {
                ObjectMapper mapper = new ObjectMapper();
                writeNewUser(mapper.convertValue(dataSnapshot.getValue(), User.class));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Write Failure", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            checkPermissions();
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

//        if (checkIfUserInsideOffice(location.getLatitude(), location.getLongitude())) {
//            return;
//        }
//        Intent intent = new Intent(this, TemporaryBlockedActivity.class);
//        intent.putExtra("TAG", OUTSIDE_OFFICE_PREMISES);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
//        finish();
    }


}