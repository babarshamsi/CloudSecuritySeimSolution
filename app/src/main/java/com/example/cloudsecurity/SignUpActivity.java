package com.example.cloudsecurity;

import android.annotation.SuppressLint;
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
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {

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
                    auth.createUserWithEmailAndPassword(email, pass)
                            .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.getException() != null && task.getException().getMessage() != null) {
                                        if (task.getException().getMessage().equals("The email address is already in use by another account."))
                                            Toast.makeText(SignUpActivity.this, "User already exists", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    if (!task.isSuccessful()) {
                                        Toast.makeText(SignUpActivity.this, "ERROR", Toast.LENGTH_LONG).show();
                                    } else {
                                        writeNewUser(task.getResult().getUser().getUid(), userName, email, userRole);
                                        Toast.makeText(SignUpActivity.this, "Your User has been Created", Toast.LENGTH_LONG).show();
                                        startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
                                        finish();
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
            return user.getText().toString();
        }
    }

    public void navigate_sign_in(View v) {
        Intent inent = new Intent(this, SignInActivity.class);
        startActivity(inent);
        finish();
    }

    public void writeNewUser(String userId, String name, String email, String userRole) {
        User user = new User(name, email, userRole);

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
}