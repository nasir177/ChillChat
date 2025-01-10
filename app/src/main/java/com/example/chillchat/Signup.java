package com.example.chillchat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chillchat.databinding.ActivitySignUpBinding;
import com.example.chillchat.utilities.constants;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

import com.example.chillchat.utilities.PreferenceManager;

public class Signup extends AppCompatActivity {

    private static final String TAG = "SignupActivity"; // Log tag for debugging
    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    private String profilepic; // Updated profilepic to String for encoded image

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
    }

    private void setListeners() {
        binding.signin.setOnClickListener(v -> finish()); // Use finish() instead of onBackPressed()
        binding.Signupbutton.setOnClickListener(this::onClick);
        binding.profilepic.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    private void registerUser() { // Register user method
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(constants.KEY_NAME, binding.name.getText().toString());
        user.put(constants.KEY_EMAIL, binding.editTextTextEmailAddress.getText().toString());
        user.put(constants.KEY_PASSWORD, binding.password.getText().toString());
        user.put(constants.KEY_IMAGE, profilepic); // Saving encoded image as profilepic

        database.collection(constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    loading(false);
                    preferenceManager.putBoolean(constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(constants.KEY_NAME, binding.name.getText().toString());
                    preferenceManager.putString(constants.KEY_IMAGE, profilepic);
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(exception -> {
                    loading(false);
                    showToast(exception.getMessage());
                });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show(); // Implemented to show message
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT); // Encodes the image to Base64
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) { // Check for null imageUri
                            try {
                                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                binding.profilepic.setImageBitmap(bitmap);
                                binding.txtaddimg.setVisibility(View.GONE);
                                profilepic = encodeImage(bitmap); // Save encoded image to profilepic
                            } catch (FileNotFoundException e) {
                                Log.e(TAG, "Error loading image", e); // Better logging
                                showToast("Error loading image");
                            }
                        } else {
                            showToast("Image URI is null");
                        }
                    }
                }
            }
    );

    private Boolean isValidSignupDetails() {
        if (profilepic == null) {
            showToast("Select profile image");
            return false;
        } else if (binding.name.getText().toString().trim().isEmpty()) {
            showToast("Enter name");
            return false;
        } else if (binding.editTextTextEmailAddress.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.editTextTextEmailAddress.getText().toString().trim()).matches()) {
            showToast("Enter a valid email");
            return false;
        } else if (binding.password.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        } else if (binding.confirmpassword.getText().toString().trim().isEmpty()) {
            showToast("Enter confirm password");
            return false;
        } else if (!binding.password.getText().toString().trim().equals(binding.confirmpassword.getText().toString().trim())) {
            showToast("Password & confirm password must be same");
            return false;
        }
        return true;
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.Signupbutton.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.Signupbutton.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void onClick(View v) {
        if (isValidSignupDetails()) {
            registerUser(); // Call to register user
        }
    }
}
