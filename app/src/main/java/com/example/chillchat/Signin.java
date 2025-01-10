package com.example.chillchat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chillchat.databinding.ActivitySignInBinding;
import com.example.chillchat.utilities.PreferenceManager;
import com.example.chillchat.utilities.constants;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Signin extends AppCompatActivity {

   private ActivitySignInBinding binding;
   private PreferenceManager preferenceManager;

   @Override
   protected void onCreate(Bundle saveInstanceState) {
      super.onCreate(saveInstanceState);
      preferenceManager = new PreferenceManager(getApplicationContext());
      if(preferenceManager.getBoolean(constants.KEY_IS_SIGNED_IN)) {
         Intent intent = new Intent(getApplicationContext(), MainActivity.class);
         startActivity(intent);
         finish();  // Initialize the PreferenceManager
      }
      binding = ActivitySignInBinding.inflate(getLayoutInflater());
      setContentView(binding.getRoot());
      setListeners();  // Set up listeners for buttons
   }

   private void setListeners() {
      // When user clicks on "Create New Account"
      binding.createNewAcc.setOnClickListener(v ->
              startActivity(new Intent(getApplicationContext(), Signup.class)));

      // When user clicks the Sign In button
      binding.Signinbutton1.setOnClickListener(v -> {
         if (isValidSignInDetails()) {
            signin();  // Perform the login action if the details are valid
         }
      });
   }

   // Method to handle sign-in logic
   private void signin() {
      FirebaseFirestore database = FirebaseFirestore.getInstance();
      database.collection(constants.KEY_COLLECTION_USERS)
              .whereEqualTo(constants.KEY_EMAIL, binding.editTextTextEmailAddress.getText().toString())
              .whereEqualTo(constants.KEY_PASSWORD, binding.editTextNumberPassword.getText().toString())
              .get()
              .addOnCompleteListener(task -> {
                 if (task.isSuccessful() && task.getResult() != null
                         && !task.getResult().getDocuments().isEmpty()) {
                    DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);

                    // Save user session in SharedPreferences
                    preferenceManager.putBoolean(constants.KEY_IS_SIGNED_IN, true);
                    Log.d("Signin", "Is user signed in: " + preferenceManager.getBoolean(constants.KEY_IS_SIGNED_IN));// Mark user as signed in
                    preferenceManager.putString(constants.KEY_USER_ID, documentSnapshot.getId());  // Save user ID
                    preferenceManager.putString(constants.KEY_NAME, documentSnapshot.getString(constants.KEY_NAME));  // Save user name
                    preferenceManager.putString(constants.KEY_IMAGE, documentSnapshot.getString(constants.KEY_IMAGE));  // Save user image

                    // Log the user in and move to the MainActivity
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);  // Clear the back stack
                    startActivity(intent);
                 } else {
                    showToast("Unable to sign in. Please check your credentials.");
                 }
              })
              .addOnFailureListener(e -> showToast("Unable to sign in: " + e.getMessage()));
   }

   // Helper function to show a Toast message
   private void showToast(String message) {
      Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
   }

   // Validate email and password
   private Boolean isValidSignInDetails() {
      if (binding.editTextTextEmailAddress.getText().toString().trim().isEmpty()) {
         showToast("Enter Email");
         return false;
      } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.editTextTextEmailAddress.getText().toString()).matches()) {
         showToast("Enter a Valid Email");
         return false;
      } else if (binding.editTextNumberPassword.getText().toString().trim().isEmpty()) {
         showToast("Enter Password");
         return false;
      } else {
         return true;
      }
   }
}
