package com.example.chillchat.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import java.util.HashMap;

public interface ApiService {

    @POST("send")
    Call<String> sendMessage(
            @HeaderMap HashMap<String, String> headers,
            @Body String messageBody
    );
    // New method for translation
    @POST("translate")
    Call<String> translateMessage(
            @Body String requestBody
    );

}
