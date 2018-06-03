package com.nextdots.retargetly.api;

import android.content.BroadcastReceiver;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nextdots.retargetly.data.listeners.CustomEventListener;
import com.nextdots.retargetly.data.models.Event;
import com.nextdots.retargetly.receivers.NetworkBroadCastReceiver;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.nextdots.retargetly.api.ApiConstanst.TAG;

public class ApiController {

    private ApiService service;
    /*
    Creamos e inicializamos las variables con sus valores por default
     */
    public int motionFrequency= 300;
    public int staticFrequency= 1800;
    public int motionTreshold= 300;
    public int motionDetectionFrequency = 20;
    public static String ip = "";

    public ApiController(){

        OkHttpClient defaultHttpClient = new OkHttpClient.Builder()
                .addInterceptor(
                        new Interceptor() {
                            @Override
                            public okhttp3.Response intercept(Chain chain) throws IOException {
                                Request request = chain.request().newBuilder()
                                        .addHeader("Accept", "Application/JSON").build();
                                return chain.proceed(request);
                            }
                        }).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.retargetly.com/")
                .client(defaultHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(ApiService.class);
    }

    public void callCustomEvent(Event event){
        event.ip = ip;
        event.nwifi = NetworkBroadCastReceiver.nWifi;
        callEvent(event,null);
    }

    public void callCustomEvent(Event event,final CustomEventListener customEventListener){
        callEvent(event,customEventListener);
    }
    public void callIp(){
        service.callDynamic("http://www.ip-api.com/json").enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                if(response.body()!=null){
                    final JsonObject json = response.body().getAsJsonObject();
                    if(json!=null){
                        ip = json.get("query").getAsString();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }
    public void callInitData(String api){
        service.callInit(api).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                if(response.body()!=null){
                    final JsonObject json = response.body().getAsJsonObject();
                    if(json!=null){
                        final JsonObject jsonResponse = json.getAsJsonObject("response");
                        if(jsonResponse!=null){
                            motionDetectionFrequency = jsonResponse
                                    .get("motionDetectionFrequency").getAsInt();
                            motionFrequency = jsonResponse
                                    .get("motionFrequency").getAsInt();
                            motionTreshold = jsonResponse
                                    .get("motionTreshold").getAsInt();
                            staticFrequency = jsonResponse
                                    .get("staticFrequency").getAsInt();
                        }
                    }
                }

            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                t.printStackTrace();
            }


        });

    }
    private void callEvent(final Event event,final CustomEventListener customEventListener){
        service.callEvent(event).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if(event.getEt() != ApiConstanst.EVENT_OPEN)
                    Log.d(TAG,"Event : "+event.getEt() + ", " +
                            "value:" + event.getValue() + ", " +
                            "status: " + response.code()+", "+
                            "IP: "+event.ip+", nWifi "+event.nwifi);
                else
                    Log.d(TAG,"Event : "+event.getEt() + ", status: " + response.code()+", "+
                            "IP: "+event.ip+", nWifi "+event.nwifi);

                if(customEventListener != null)
                    customEventListener.customEventSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG,t.getMessage());
                if(customEventListener != null)
                    customEventListener.customEventFailure(t.getMessage());
            }
        });
    }

}
