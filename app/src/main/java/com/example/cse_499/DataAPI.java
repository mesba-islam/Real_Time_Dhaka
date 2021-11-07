package com.example.cse_499;

import java.util.ArrayList;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface DataAPI {


    @POST("foodmood-0.0.1/data/save")
    Call<DataModel> saveData(@Body DataModel requestBody);


    @GET("foodmood-0.0.1/data/getalldata")
    Call<ArrayList<DataModel>> getData();
}