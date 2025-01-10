package com.example.chillchat;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chillchat.utilities.PreferenceManager;
import com.example.chillchat.utilities.constants;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class BaseActivity extends AppCompatActivity {
    private DocumentReference documentReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        documentReference = database.collection(constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(constants.KEY_USER_ID));
    }
    @Override
    protected void onPause() {
        super.onPause();
        documentReference.update(constants.KEY_AVAILABILITY, 0);
    }
    @Override
    protected void onResume() {
        super.onResume();
        documentReference.update(constants.KEY_AVAILABILITY, 1);
    }
}
