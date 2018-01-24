package com.seeker.seeker;

import android.content.Intent;
import android.nfc.Tag;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTextEmail = (AutoCompleteTextView) findViewById(R.id.email);
        mTextPwd = (EditText) findViewById(R.id.pwd);

        mBtnSignIn = (Button) findViewById(R.id.sign_in_btn);
        Log.d(TAG, "onCreate: "+mBtnSignIn);

        mBtnRegister = (Button) findViewById(R.id.register_btn);
        if(mBtnSignIn != null) {

            mBtnSignIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String email = mTextEmail.getText().toString();
                    String pwd = mTextPwd.getText().toString();
                    Toast.makeText(LoginActivity.this, "Sign In clicked"+ email +"   "+ pwd, Toast.LENGTH_SHORT).show();
                    Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(mainIntent);
                }
            });
        }

        if(mBtnRegister != null){
            mBtnRegister.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(LoginActivity.this, "Register clicked", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
