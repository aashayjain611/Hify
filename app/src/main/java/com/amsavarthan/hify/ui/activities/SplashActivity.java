package com.amsavarthan.hify.ui.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.amsavarthan.hify.R;
import com.firebase.ui.auth.AuthUI;
import com.shobhitpuri.custombuttons.GoogleSignInButton;

import java.util.Collections;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private GoogleSignInButton signInButton;
    private static final int RC_SIGN_IN = 200;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        signInButton = findViewById(R.id.signInButtonImpl);

        new Handler().postDelayed(new Runnable() {


            @Override
            public void run() {
                // This method will be executed once the timer is over
//                Intent i = new Intent(SplashActivity.this, MainActivity.class);
//                startActivity(i);
//                finish();

            signInButton.setVisibility(View.VISIBLE);

            }
        }, 3000);

        final List<AuthUI.IdpConfig> providers = Collections.singletonList(new AuthUI.IdpConfig.GoogleBuilder().build());

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInButton.setEnabled(false);

                // Create and launch sign-in intent
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(providers)
                                .build(),
                        RC_SIGN_IN);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        progressDialog=new ProgressDialog(SplashActivity.this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setTitle("Logging in");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();

        if (requestCode == RC_SIGN_IN)
        {
            if (resultCode == RESULT_OK)
            {
                dismissProgressDialog();
                Toast.makeText(this,"Login successful",Toast.LENGTH_LONG).show();
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
            else
            {
                signInButton.setEnabled(true);
                dismissProgressDialog();
                Toast.makeText(this,"Login failed",Toast.LENGTH_LONG).show();
            }
        }
        else
            Toast.makeText(this,"Login failed",Toast.LENGTH_LONG).show();
    }

    private void dismissProgressDialog()
    {
        if (progressDialog!=null && progressDialog.isShowing())
            progressDialog.dismiss();
    }

}
