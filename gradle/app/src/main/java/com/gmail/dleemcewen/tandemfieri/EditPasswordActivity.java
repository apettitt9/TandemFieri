package com.gmail.dleemcewen.tandemfieri;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.gmail.dleemcewen.tandemfieri.Entities.User;
import com.gmail.dleemcewen.tandemfieri.Logging.LogWriter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.logging.Level;

public class EditPasswordActivity extends AppCompatActivity {

    private User currentUser;
    private String type = "";
    private String uid = "";
    private boolean validPswd = false;

    private FirebaseUser fireuser;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseAuth mAuth;

    private EditText password, confirmPswd, oldPassword;
    private Button saveButton, cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_password);

        Bundle bundle = new Bundle();
        bundle = this.getIntent().getExtras();
        currentUser = (User) bundle.getSerializable("User");
        type = this.getIntent().getStringExtra("UserType");

        /*For the code I currently have, the key stored in the User object is not the same as the id in the database.
        Therefore I must use "uid = FirebaseAuth.getInstance().getCurrentUser().getUid()" to access the correct
        user in the database.*/
        mAuth = FirebaseAuth.getInstance();
        fireuser = mAuth.getCurrentUser();
        if (fireuser != null) {
            // User is signed in
            uid = fireuser.getUid();
        } else {
            // No user is signed in
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("Exit me", true);
            startActivity(intent);
            finish();
        }

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    //Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    //Log.d(TAG, "onAuthStateChanged:signed_out");
                    Intent intent = new Intent(EditPasswordActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("Exit me", true);
                    startActivity(intent);
                    finish();
                }
            }
        };

        //Toast.makeText(getApplicationContext(),"The user is " + currentUser.getEmail(), Toast.LENGTH_LONG).show();

        //get handles to the view
        oldPassword = (EditText)findViewById(R.id.old_password);
        password = (EditText)findViewById(R.id.password);
        confirmPswd = (EditText)findViewById(R.id.confirmPassword);
        saveButton = (Button) findViewById(R.id.saveButton);
        cancelButton = (Button)findViewById(R.id.cancelButton);

        //program button listeners
        //cancels the page and returns to previous page
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        //saves information to the database
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                while (!isValidPassword(oldPassword.getText().toString(),
                        password.getText().toString(),
                        confirmPswd.getText().toString())) {

                    if (isValidPassword(oldPassword.getText().toString(),
                            password.getText().toString(),
                            confirmPswd.getText().toString())) {
                        savePassword(password.getText().toString());
                        finish();
                    } else {

                        oldPassword.setText("");
                        password.setText("");
                        confirmPswd.setText("");
                        oldPassword.requestFocus();
                    }
                }//end onClick
            }
        });

    }//end onCreate

    //validates the password
    public boolean isValidPassword(String oldPassword, String newPassword, String confirmPassword){

        boolean result = true;
        String msg = "Password changed.";

       if (!newPassword.equals(confirmPassword)) {
            msg = "Your password confirmation must be the same as your new password.";
            result = false;
        }
        else if (!newPassword.matches(".*\\w.*")) {
            msg = "Please use characters and not only whitespace.";
            result = false;
        }
        else if (newPassword.length() < 6) {
            msg = "Please enter a password with 6 or more characters.";
            result = false;
        }
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        return result;
    }

    //updates password in authentication
    public void savePassword(String newPassword){

        fireuser.updatePassword(newPassword)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            LogWriter.log(getApplicationContext(), Level.INFO, "User password updated.");
                        }
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}//end activity
