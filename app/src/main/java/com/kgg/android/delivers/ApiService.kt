package com.kgg.android.delivers

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*


data class ResponseDC(var result:String? = null)
interface ApiService {
    @Multipart
    @POST("/upload-images")
    fun postImage(
        @Part image: MultipartBody.Part?,


    ): Call<ResponseBody?>?



    @GET("/getimg/{id}")
    fun getRequest(@Path("id") id: String): Call<ResponseDC>


}