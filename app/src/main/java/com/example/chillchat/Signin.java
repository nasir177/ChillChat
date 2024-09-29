package com.example.chillchat;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chillchat.databinding.ActivitySignInBinding;

public class Signin extends AppCompatActivity {

   private ActivitySignInBinding binding;

   @Override
   protected void onCreate(Bundle saveInstanceState) {
      super.onCreate(saveInstanceState);
      binding = ActivitySignInBinding.inflate(getLayoutInflater());
      setContentView(binding.getRoot());
      setListeners();
   }

   private void setListeners() {
      binding.createNewAcc.setOnClickListener(v ->
              startActivity(new Intent(getApplicationContext() , Signup.class)));
   }
}
