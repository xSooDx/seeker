package com.seeker.seeker;

import android.accounts.Account;
import android.content.Intent;
import android.content.res.Resources;
import android.nfc.Tag;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import static com.seeker.seeker.R.id.email;
import static com.seeker.seeker.R.id.pwd;
import static com.seeker.seeker.R.id.register_btn;
import static com.seeker.seeker.R.id.sign_in_btn;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity" ;
    private AutoCompleteTextView mTextEmail;
    private EditText mTextPwd;
    private Button mBtnSignIn;
    private Button mBtnRegister;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user!=null){
           // Toast.makeText(this, user.getEmail()+" already signed in", Toast.LENGTH_SHORT).show();
            Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(mainIntent);
            finish();
        }
        else {

            mTextEmail = (AutoCompleteTextView) findViewById(R.id.email);
            mTextPwd = (EditText) findViewById(R.id.pwd);

            mBtnSignIn = (Button) findViewById(R.id.sign_in_btn);
            Log.d(TAG, "onCreate: " + mBtnSignIn);

            mBtnRegister = (Button) findViewById(R.id.register_btn);

            if (mBtnSignIn != null) {
                mBtnSignIn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String email = mTextEmail.getText().toString();
                        String pwd = mTextPwd.getText().toString();
                        if (mTextEmail.getText().toString().equals("")) {
                            Toast.makeText(LoginActivity.this, "ENTER EMAIL", Toast.LENGTH_SHORT).show();
                        } else{
                            if (mTextPwd.getText().toString().equals("")) {
                                Toast.makeText(LoginActivity.this, "ENTER PASSWORD", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LoginActivity.this, "Signing in, Please wait..", Toast.LENGTH_LONG).show();
                                goSignIn();
                            }
                        }
                    }
                });
            }

            if (mBtnRegister != null) {
                mBtnRegister.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String email = mTextEmail.getText().toString();
                        String pwd = mTextPwd.getText().toString();
                        if (mTextEmail.getText().toString().equals("")) {
                            Toast.makeText(LoginActivity.this, "ENTER EMAIL", Toast.LENGTH_SHORT).show();

                        } else{
                                if (mTextPwd.getText().toString().equals("")) {
                                    Toast.makeText(LoginActivity.this, "ENTER PASSWORD", Toast.LENGTH_SHORT).show();
                                } else {
                                    goRegister();
                                }
                        }
                    }
                });
            }
        }
    }

    public void goSignIn() {
        mAuth.signInWithEmailAndPassword(mTextEmail.getText().toString(), mTextPwd.getText().toString())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(mainIntent);
                            finish();

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void goRegister(){
        mAuth.createUserWithEmailAndPassword(mTextEmail.getText().toString(), mTextPwd.getText().toString())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(LoginActivity.this, "Register success; Login with the same details", Toast.LENGTH_SHORT).show();

                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(null, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
