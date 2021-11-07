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

public interface UploadApis {

    @Multipart
    @POST("file-demo-0.0.1-SNAPSHOT/uploadFile")
    Call<UploadFileResponse> uploadImage(@Part MultipartBody.Part file, @Part("body") RequestBody requestBody);

    @POST("/data/save")
    Call<DataModel> saveData(@Body DataModel requestBody);


    @GET("/data/getalldata")
    Call<ArrayList<DataModel>> getData();
}