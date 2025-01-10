package com.example.chillchat;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;


import com.example.chillchat.Adapter.UserAdapter;
import com.example.chillchat.databinding.ActivityUsersBinding;
import com.example.chillchat.listeners.UserListener;
import com.example.chillchat.models.users;
import com.example.chillchat.utilities.PreferenceManager;
import com.example.chillchat.utilities.constants;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.auth.User;

import java.util.ArrayList;
import java.util.List;

public class usersActivity extends BaseActivity implements UserListener {

    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;
    private List<users> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());
        userList = new ArrayList<>(); // Initialize the user list
        setListeners();
        getUsers();
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed()); // Handle back button click
    }

    private void getUsers() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString(constants.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null) {
                        userList.clear(); // Clear the list before adding new data
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (currentUserId != null && currentUserId.equals(queryDocumentSnapshot.getId())) {
                                // Skip the current user from the list
                                continue;
                            }
                            users user = new users();
                            user.name = queryDocumentSnapshot.getString(constants.KEY_NAME);
                            user.image = queryDocumentSnapshot.getString(constants.KEY_IMAGE);
                            user.token = queryDocumentSnapshot.getString(constants.KEY_FCM_TOKEN);
                            user.id = queryDocumentSnapshot.getId();
                            userList.add(user); // Add user to the list
                        }
                        if (!userList.isEmpty()) { // Check if the list is not empty
                            UserAdapter usersAdapter = new UserAdapter(userList, this);
                            binding.usersRecyclerView1.setAdapter(usersAdapter);
                            binding.usersRecyclerView1.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage();
                        }
                    } else {
                        showErrorMessage();
                    }
                })
                .addOnFailureListener(e -> {
                    loading(false);
                    showErrorMessage();
                });
    }

    private void showErrorMessage() {
        binding.textErrorMessage1.setText(String.format("%s", "No user available"));
        binding.textErrorMessage1.setVisibility(View.VISIBLE);
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserClicked(users user) {
        Intent intent = new Intent(getApplicationContext(), chatActivity.class);
        intent.putExtra(constants.KEY_USER, user); // Ensure 'users' class implements Serializable or Parcelable
        startActivity(intent);
        finish(); // Close this activity to avoid going back to it
    }
}