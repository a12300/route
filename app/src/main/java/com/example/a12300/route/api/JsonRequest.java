package com.example.a12300.route.api;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

public class JsonRequest extends JsonObjectRequest {

    public JsonRequest(int method
            , String url
            , JSONObject jsonRequest
            , Listener<JSONObject> listener
            , ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
        // TODO Auto-generated constructor stub
    }
}
