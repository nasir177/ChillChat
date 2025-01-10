package com.example.chillchat;


import android.content.Intent;

import com.example.chillchat.listeners.ConversionListener;
import com.example.chillchat.models.users;
import com.google.firebase.firestore.EventListener;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;


import com.example.chillchat.Adapter.RecentConversationAdapter;
import com.example.chillchat.databinding.ActivityMainBinding;
import com.example.chillchat.models.ChatMessage;
import com.example.chillchat.utilities.PreferenceManager;
import com.example.chillchat.utilities.constants;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity implements ConversionListener {
    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RecentConversationAdapter conversationsAdapter;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize PreferenceManager
        preferenceManager = new PreferenceManager(getApplicationContext());

        // Check if user is already logged in
        if (preferenceManager.getBoolean(constants.KEY_IS_SIGNED_IN)) {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            init();
            listenConversations();

            //loadUserDetails();
            getToken();
            setListeners();
        } else {
            // User is not logged in, redirect to sign-in screen
            startActivity(new Intent(getApplicationContext(), Signin.class));
            finish();
        }
    }

    private void init() {
        conversations = new ArrayList<>();
        conversationsAdapter = new RecentConversationAdapter(conversations,this);
        binding.conversationsRecyclerView.setAdapter(conversationsAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void setListeners() {
        // Sign out button click listener
        binding.imageSignOut.setOnClickListener(v -> signout());
        binding.fabNewChat.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), usersActivity.class)));
        binding.tabStories.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), story.class)));
        binding.fabNewSetting.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), settings.class)));


    }

    /*private void loadUserDetails() {
        // Check if user details are available and handle null cases
        String userName = preferenceManager.getString(constants.KEY_NAME);
        String encodedImage = preferenceManager.getString(constants.KEY_IMAGE);

        // Display the user's name or default if not found
        if (userName != null && !userName.isEmpty()) {
            binding.textName.setText(userName);
        } else {
            binding.textName.setText("Unknown User"); // Fallback to default name
        }

        // Decode and display profile image or set a default if not found
        if (encodedImage != null && !encodedImage.isEmpty()) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            binding.imageProfile.setImageBitmap(bitmap);
        } else {
            // Set a default profile image if no image is found
            binding.imageProfile.setImageResource(R.drawable.background_icon);
        }
    }*/

    private void showToast(String message) {
        // Display toast messages
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void listenConversations() {
        database.collection(constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(constants.KEY_SENDER_ID, preferenceManager.getString(constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(constants.KEY_RECEIVER_ID, preferenceManager.getString(constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            // Log the error for debugging
            return;
        }
        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    // Extract sender and receiver IDs
                    String senderId = documentChange.getDocument().getString(constants.KEY_SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(constants.KEY_RECEIVER_ID);

                    // Create a new ChatMessage object
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = senderId;
                    chatMessage.receiverId = receiverId;

                    if(preferenceManager.getString(constants.KEY_USER_ID).equals(senderId)){
                        chatMessage.conversionImage = documentChange.getDocument().getString(constants.KEY_RECEIVER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(constants.KEY_RECEIVER_NAME);
                        chatMessage.conversionId = documentChange.getDocument().getString(constants.KEY_RECEIVER_ID);
                    }else{
                        chatMessage.conversionImage = documentChange.getDocument().getString(constants.KEY_SENDER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(constants.KEY_SENDER_NAME);
                        chatMessage.conversionId = documentChange.getDocument().getString(constants.KEY_SENDER_ID);
                    }
                    chatMessage.message = documentChange.getDocument().getString(constants.KEY_LAST_MESSAGE);
                   // chatMessage.dateObject = documentChange.getDocument().getString(constants.KEY_TIMESTAMP);
                    // Add chatMessage to conversations list
                    conversations.add(chatMessage);
                } else if (documentChange.getType()== DocumentChange.Type.MODIFIED) {
                    for (int i=0;i<conversations.size();i++){
                        String senderId = documentChange.getDocument().getString(constants.KEY_SENDER_ID);
                        String receiverId = documentChange.getDocument().getString(constants.KEY_RECEIVER_ID);
                        if(conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)){
                            conversations.get(i).message = documentChange.getDocument().getString(constants.KEY_LAST_MESSAGE);
                            conversations.get(i).dateObject = documentChange.getDocument().getDate(constants.KEY_TIMESTAMP);
                            break;
                        }
                    }
                }
            }
            Collections.sort(conversations, (obj1, obj2) -> {
                if (obj1.dateObject == null && obj2.dateObject == null) {
                    return 0; // If both are null, consider them equal
                } else if (obj1.dateObject == null) {
                    return 1; // If obj1's dateObject is null, place it after obj2
                } else if (obj2.dateObject == null) {
                    return -1; // If obj2's dateObject is null, place obj1 before obj2
                } else {
                    return obj2.dateObject.compareTo(obj1.dateObject); // Normal comparison
                }
            });
            conversationsAdapter.notifyDataSetChanged();
            binding.conversationsRecyclerView.smoothScrollToPosition(0);
            binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    };

    private void getToken() {
        // Get Firebase Cloud Messaging token
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String userId = preferenceManager.getString(constants.KEY_USER_ID);

        // Ensure user ID is available before updating token
        if (userId != null) {
            DocumentReference documentReference = database.collection(constants.KEY_COLLECTION_USERS).document(userId);

            documentReference.update(constants.KEY_FCM_TOKEN, token)
                   // .addOnSuccessListener(unused -> showToast("Welcome"))
                    .addOnFailureListener(e -> showToast("Unable to update token"));
        } else {
            showToast("User ID not found. Cannot update token.");
        }
    }

    private void signout() {
        // Display a message indicating the sign-out process
        showToast("Signing out...");

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String userId = preferenceManager.getString(constants.KEY_USER_ID);

        // Check if userId is null before proceeding
        if (userId != null && !userId.isEmpty()) {
            DocumentReference documentReference = database.collection(constants.KEY_COLLECTION_USERS).document(userId);

            // Create a HashMap to update the Firestore document
            HashMap<String, Object> updates = new HashMap<>();
            updates.put(constants.KEY_FCM_TOKEN, FieldValue.delete());

            // Perform the update and handle success or failure
            documentReference.update(updates)
                    .addOnSuccessListener(unused -> {
                        // Clear user preferences and redirect to sign-in screen
                        preferenceManager.clear();

                        // Create an intent for the Signin activity
                        Intent intent = new Intent(getApplicationContext(), Signin.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear the back stack
                        startActivity(intent);

                        // Close the current activity
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        // Display an error message if the Firestore update fails
                        showToast("Unable to sign out. Please try again.");
                    });
        } else {
            // Display an error message if userId is null or empty
            showToast("User ID not found. Cannot sign out.");
        }
    }


    @Override
    public void onConversionClicked(users user) { // Assuming 'users' is the correct model class
        Intent intent = new Intent(getApplicationContext(), chatActivity.class);
        intent.putExtra(constants.KEY_USER, user); // Call putExtra on the intent instance
        startActivity(intent);
    }


}
