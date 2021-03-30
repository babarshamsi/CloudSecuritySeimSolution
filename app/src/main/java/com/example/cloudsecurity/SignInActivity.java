package com.example.cloudsecurity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.Utils.GenericUtils;
import com.example.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignInActivity extends AppCompatActivity {

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
                if (TextUtils.isEmpty(password) && password.length() < 8 ) {
                    Toast.makeText(getApplicationContext(), "At least 8 character required for password", Toast.LENGTH_SHORT).show();
                    return;
                }
                //authenticate user
                auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(SignInActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (!task.isSuccessful()) {
                                    if (task.getException() != null && task.getException().getMessage() != null) {
                                        String errorMessage = task.getException().getMessage();
                                        if (errorMessage.equals("There is no user record corresponding to this identifier. The user may have been deleted.")) {
                                            Toast.makeText(getApplicationContext(), "User not found", Toast.LENGTH_SHORT).show();
                                        } else if (errorMessage.equals("The password is invalid or the user does not have a password.")) {
                                            Toast.makeText(getApplicationContext(), "User Email or Password is Wrong", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                } else {
                                    writeNewUser(task.getResult().getUser().getUid(), email, userRole);
                                    Intent intent = new Intent(SignInActivity.this, HomeActivityForUser.class);
                                    startActivity(intent);
                                    finish();
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


    public void writeNewUser(String userId, String email, String userRole) {
        User user = new User(null, email, userRole);
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

//    public void getUserData(String userId, String name, String email) {
//        mDatabase.child("users").child(userId).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<DataSnapshot> task) {
//                if (!task.isSuccessful()) {
//                    Log.e("firebase", "Error getting data", task.getException());
//                }
//                else {
//                    Log.d("firebase", String.valueOf(task.getResult().getValue()));
//                }
//            }
//        });
//    }
}