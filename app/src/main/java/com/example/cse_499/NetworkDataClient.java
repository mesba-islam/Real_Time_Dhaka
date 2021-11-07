package com.example.cse_499;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkDataClient {
   //private static final String BASE_URL = "http://35.247.139.226/nsu-0.0.1/";
   private static final String BASE_URL = "http://35.186.154.9:8080/";

    private static Retrofit retrofit;
    static OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(300, TimeUnit.SECONDS)
            .connectTimeout(300, TimeUnit.SECONDS)
            .build();
    public static Retrofit getRetrofitClient(Context context) {
        if (retrofit == null) {

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

}