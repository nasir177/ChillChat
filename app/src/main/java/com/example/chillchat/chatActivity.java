package com.example.chillchat;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.chillchat.Adapter.ChatAdapter;
import com.example.chillchat.databinding.ActivityChatBinding;
import com.example.chillchat.models.ChatMessage;
import com.example.chillchat.models.users;
import com.example.chillchat.network.ApiClient;
import com.example.chillchat.network.ApiService;
import com.example.chillchat.utilities.PreferenceManager;
import com.example.chillchat.utilities.constants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class chatActivity extends BaseActivity {

    private ActivityChatBinding binding;
    private users receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversionId = null;
    private Boolean isReceiverAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init(); // Initialize components
        loadReceiverDetails(); // Load receiver data
        setListeners(); // Set listeners for UI actions

        if (receiverUser != null) {
            listenMessages(); // Listen to messages only if receiver data is valid
        } else {
            showError("Receiver user data is missing!");
        }
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        database = FirebaseFirestore.getInstance();

        // Setup RecyclerView with a reversed layout
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.chatRecyclerView.setLayoutManager(layoutManager);
    }

    private void loadReceiverDetails() {
        receiverUser = (users) getIntent().getSerializableExtra(constants.KEY_USER);
        if (receiverUser != null) {
            binding.textName.setText("Chat with " + receiverUser.name);

            // Fetch the receiver's image and set it to the adapter
            Bitmap receiverImage = getBitmapFromEncodedString(receiverUser.image);
            if (receiverImage != null) {
                // Set image to an ImageView (in case you have a profile image display on the UI)
                binding.imageProfile1.setImageBitmap(receiverImage);
            } else {
                // Set a default image if no image is available
               // binding.imageReceiver.setImageResource(R.drawable.default_profile_image);
            }

            chatAdapter = new ChatAdapter(
                    chatMessages,
                    receiverImage,
                    preferenceManager.getString(constants.KEY_USER_ID)
            );
            binding.chatRecyclerView.setAdapter(chatAdapter);
        } else {
            binding.textName.setText("Unknown User");
           // binding.imageReceiver.setImageResource(R.drawable.default_profile_image); // Default image
        }
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> {
            if (!binding.inputMessage.getText().toString().isEmpty()) {
                sendMessage();
            } else {
                showError("Message cannot be empty.");
            }
        });
    }

    private void sendMessage() {
        if (receiverUser == null) {
            showError("Cannot send message. Receiver user is null.");
            return;
        }

        HashMap<String, Object> message = new HashMap<>();
        message.put(constants.KEY_SENDER_ID, preferenceManager.getString(constants.KEY_USER_ID));
        message.put(constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        message.put(constants.KEY_TIMESTAMP, new Date());

        database.collection(constants.KEY_COLLECTION_CHAT).add(message);

        if (conversionId != null) {
            updateConversion(binding.inputMessage.getText().toString());
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(constants.KEY_SENDER_ID, preferenceManager.getString(constants.KEY_USER_ID));
            conversion.put(constants.KEY_SENDER_NAME, preferenceManager.getString(constants.KEY_NAME));
            conversion.put(constants.KEY_SENDER_IMAGE, preferenceManager.getString(constants.KEY_IMAGE));
            conversion.put(constants.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(constants.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(constants.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversion.put(constants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString());
            conversion.put(constants.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
        if (!isReceiverAvailable) {
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.token);

                JSONObject data = new JSONObject();
                data.put(constants.KEY_USER_ID, preferenceManager.getString(constants.KEY_USER_ID));
                data.put(constants.KEY_NAME, preferenceManager.getString(constants.KEY_NAME));
                data.put(constants.KEY_FCM_TOKEN, preferenceManager.getString(constants.KEY_FCM_TOKEN));
                data.put(constants.KEY_MESSAGE, binding.inputMessage.getText().toString());

                JSONObject body = new JSONObject();
                body.put(constants.REMOTE_MSG_REGISTRATION_IDS, tokens);
                body.put(constants.REMOTE_MSG_DATA, data);
                sendNotification(body.toString());

            } catch (Exception exception) {
                showToast(exception.getMessage());
            }
        }
        binding.inputMessage.setText(null);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String messageBody) {
        ApiClient.getClient().create(ApiService.class).sendMessage(
                constants.getRemoteMessageHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    try {
                        if (response.body() != null) {
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results = responseJson.getJSONArray("results");
                            if (responseJson.getInt("failure") == 1) {
                                JSONObject error = (JSONObject) results.get(0);
                                showToast(error.getString("error"));
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showToast("Notification sent successfully");
                } else {
                    // showToast("Error: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                showToast(t.getMessage());
            }
        });
    }

    private void listenMessages() {
        database.collection(constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(constants.KEY_SENDER_ID, preferenceManager.getString(constants.KEY_USER_ID))
                .whereEqualTo(constants.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);

        database.collection(constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(constants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(constants.KEY_RECEIVER_ID, preferenceManager.getString(constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            showError("Failed to listen to messages: " + error.getMessage());
            return;
        }
        if (value != null) {
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(constants.KEY_MESSAGE);
                    chatMessage.dateTime = getRelativeTime(documentChange.getDocument().getDate(constants.KEY_TIMESTAMP), chatMessage.isSeen);
                    chatMessage.dateObject = documentChange.getDocument().getDate(constants.KEY_TIMESTAMP);
                    chatMessage.isSeen = documentChange.getDocument().getBoolean("isSeen") != null && documentChange.getDocument().getBoolean("isSeen");

                    chatMessages.add(chatMessage);
                }
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    chatMessages.sort(Comparator.comparing(chatMessage -> chatMessage.dateObject));
                } else {
                    Collections.sort(chatMessages, (m1, m2) -> m1.dateObject.compareTo(m2.dateObject));
                }
            } catch (Exception e) {
                showError("Error while sorting messages: " + e.getMessage());
            }

            if (count == 0) {
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeInserted(count, chatMessages.size() - count);
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
        }
        binding.progressBar.setVisibility(View.GONE);

        if (conversionId == null) {
            checkForConversion();
        }
    };

    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        if (encodedImage != null && !encodedImage.isEmpty()) {
            byte[] bytes = android.util.Base64.decode(encodedImage, android.util.Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }

    private String getRelativeTime(Date date, boolean isSeen) {
        long currentTime = System.currentTimeMillis();
        long messageTime = date.getTime();
        long difference = currentTime - messageTime;

       // String status = isSeen ? "Seen" : "Sent";

        if (difference < 60 * 1000) {
            return /*status +*/ " " + (difference / 1000) + " sec ago";
        } else if (difference < 60 * 60 * 1000) {
            return /*status +*/  " " + (difference / (60 * 1000)) + " min ago";
        } else if (difference < 24 * 60 * 60 * 1000) {
            return /*status +*/  " " + (difference / (60 * 60 * 1000)) + " hr ago";
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault());
            return /*status + " on " + */ dateFormat.format(date);
        }

    }


    private void showError(String message) {
        System.out.println("Error: " + message);
    }

    private void addConversion(HashMap<String, Object> conversion) {
        database.collection(constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private void updateConversion(String message) {
        DocumentReference documentReference = database.collection(constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
        documentReference.update(
                constants.KEY_LAST_MESSAGE, message,
                constants.KEY_TIMESTAMP, new Date()
        );
    }

    private void checkForConversion() {
        if (!chatMessages.isEmpty()) {
            checkForConversionRemotely(preferenceManager.getString(constants.KEY_USER_ID), receiverUser.id);
            checkForConversionRemotely(receiverUser.id, preferenceManager.getString(constants.KEY_USER_ID));
        }
    }

    private void checkForConversionRemotely(String senderId, String receiverId) {
        database.collection(constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversationCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversationCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversionId = documentSnapshot.getId();
        }
    };
}
