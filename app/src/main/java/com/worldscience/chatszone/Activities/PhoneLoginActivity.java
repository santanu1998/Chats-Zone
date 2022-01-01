package com.worldscience.chatszone.Activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.worldscience.chatszone.databinding.ActivityPhoneLoginBinding;

import java.util.Objects;

public class PhoneLoginActivity extends AppCompatActivity {

    ActivityPhoneLoginBinding binding;
    FirebaseAuth auth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhoneLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        if(auth.getCurrentUser() != null) {
            Intent intent = new Intent(PhoneLoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        Objects.requireNonNull(getSupportActionBar()).hide();

        binding.phoneBox.requestFocus();

        binding.continueBtn.setOnClickListener(v -> {
            Intent intent = new Intent(PhoneLoginActivity.this, OTPActivity.class);
            intent.putExtra("phoneNumber", binding.phoneBox.getText().toString());
            startActivity(intent);
        });

    }
}