package com.example.a12300.route;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;
import com.example.a12300.route.api.JsonRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MyActivity extends FragmentActivity
                          implements LocationListener
                                     , GooglePlayServicesClient.ConnectionCallbacks
                                     , GooglePlayServicesClient.OnConnectionFailedListener{

    private static final int CAMERA_BEARING = 0;
    private static final int CAMERA_TILT = 0;
    private static final int CAMERA_ZOOM = 17;

    private static final String LINE_BREAK = "<br>";

    // 位置情報
    private  LatLng start;
    private  LatLng goal;

    // 駅情報
    private  Station stationStart;
    private  Station stationGoal;

    //  ルート情報
    private  Route route1;
    private  Route route2;

    // Fragment
    private MapFragment frgMap1;
    private MapFragment frgMap2;

    // Map
    private GoogleMap map1;
    private GoogleMap map2;

    // WebView
    private WebView webView;

    private FragmentManager fragmentManager;
    private LocationClient locationClient;

    private static final LocationRequest REQUEST = LocationRequest.create().setInterval(30000)
                                                                               .setFastestInterval(16) // 16ms = 60fps
                                                                               .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    // 目的地
    private String destination;
    // 目的地リスト
    private List<Address>addressList;
    private Address address;
    // Volley用Queue
    private RequestQueue mQueue;

    private static final int VOICE_REQUEST_CODE = 0;

    private EditText txtDestination;
    private Button btnVoice;
    private Button btnSearch;
    private Button btnToStation;
    private Button btnRail;
    private Button btnFromStation;

    private boolean isEnableGoogleService = false;
    private boolean isDirect = false;


    @Override
    protected void onResume() {
        Log.d("ROUTE_APP", "onResume (" + "" + ")");
        super.onResume();
        if(webView !=null && webView.getVisibility() != View.GONE) {
            webView.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("ROUTE_APP", "onCreate (" + "" + ")");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        fragmentManager = getFragmentManager();
        frgMap1 = (MapFragment) fragmentManager.findFragmentById(R.id.frg_map1);
        frgMap2 = (MapFragment) fragmentManager.findFragmentById(R.id.frg_map2);

        // Map用Fragment
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.hide(frgMap1);
        fragmentTransaction.hide(frgMap2);
        fragmentTransaction.commit();

        // 電車経路用WebView
        webView = (WebView) findViewById(R.id.webView);
        webView.setVisibility(View.GONE);

        // 検索テキストボックス
        txtDestination = (EditText) findViewById(R.id.txt_destination);

        // 駅までボタン
        btnToStation = (Button) findViewById(R.id.btn_to_station);
        btnToStation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Fragment処理
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.show(frgMap1);
                fragmentTransaction.hide(frgMap2);
                fragmentTransaction.commit();
                // 路線用WebView無効
                webView.setVisibility(View.GONE);
                // 現在地取得ON
                locationClient.requestLocationUpdates(REQUEST, MyActivity.this);

                if(isDirect
                && stationStart != null
                && (stationGoal == null
                || !stationStart.name.equals(stationGoal.name))) {
                    getRoot1();
                }
            }
        });
        btnToStation.setEnabled(false);

        // 電車ボタン
        btnRail = (Button) findViewById(R.id.btn_rail);
        btnRail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.setVisibility(View.VISIBLE);
                String url = "http://maps.google.com/maps";
                url += "?dirflg=t";
                url += "&saddr=" + stationStart.y + "," + stationStart.x + "(" + URLEncoder.encode(stationStart.name) + ")" ;
                url += "&daddr=" + stationGoal.y + "," + stationGoal.x + "(" +URLEncoder.encode(stationGoal.name) + ")";
                webView.loadUrl(url);
            }
        });
        btnRail.setEnabled(false);

        // 駅からボタン
        btnFromStation = (Button) findViewById(R.id.btn_from_station);
        btnFromStation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Fragment処理
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.hide(frgMap1);
                fragmentTransaction.show(frgMap2);
                fragmentTransaction.commit();
                // 路線用WebView無効
                webView.setVisibility(View.GONE);
            }
        });
        btnFromStation.setEnabled(false);

        // 検索ボタン
        btnSearch = (Button) findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // ソフトキーボード消す
                InputMethodManager manager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                manager.hideSoftInputFromWindow(v.getWindowToken(), 0);

                map2.clear();
                destination = ((EditText) findViewById(R.id.txt_destination)).getText().toString();
                addressList = getDestination(destination);
                if(addressList.size() > 0) {
                    address = addressList.get(0);
                    goal = new LatLng(address.getLatitude(), address.getLongitude());
                    CameraPosition.Builder builder = new CameraPosition.Builder().bearing(CAMERA_BEARING)
                            .tilt(CAMERA_TILT)
                            .zoom(CAMERA_ZOOM).target(goal);
                    map2.animateCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
                    MarkerOptions options = new MarkerOptions();
                    options.position(new LatLng(goal.latitude, goal.longitude));
                    options.title(address.getFeatureName());
                    map2.addMarker(options);

                    // Fragment処理
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.hide(frgMap1);
                    fragmentTransaction.show(frgMap2);
                    fragmentTransaction.commit();
                    // 路線用WebView無効
                    webView.setVisibility(View.GONE);

                    // 駅情報取得
                    getStation2();
                }
            }
        });
        btnSearch.setEnabled(false);

        // 音声ボタン
        btnVoice = (Button) findViewById(R.id.btn_voice);
        btnVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // インテント作成
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL
                            ,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

                    // メッセージを設定。
                    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "音声認識を実行中");

                    // インテント発行
                    startActivityForResult(intent, VOICE_REQUEST_CODE);
                } catch (ActivityNotFoundException e) {
                    // このインテントに応答できるアクティビティがインストールされていない場合
                    Toast.makeText(MyActivity.this, "音声認識は使用できません", Toast.LENGTH_LONG).show();
                }
            }
        });
        btnVoice.setEnabled(false);

        locationClient = new LocationClient(getApplicationContext(), this, this); // ConnectionCallbacks, OnConnectionFailedListener
        if (locationClient != null) {
            locationClient.connect();
        }
        mQueue = Volley.newRequestQueue(getApplicationContext());

        // Google Play Servicesを利用可能か
        int iRes = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (iRes == ConnectionResult.SUCCESS) {
            isEnableGoogleService = true;
            initMap();
            btnSearch.setEnabled(true);
            btnVoice.setEnabled(true);
        }
        else if (GooglePlayServicesUtil.isUserRecoverableError(iRes)) {
            // ユーザが対応可能なエラー
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(iRes, this, 0);
            if (dialog != null) {
                GooglePlayServicesUtil.getErrorDialog(iRes, this, 1, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        // TODO Auto-generated method stub
                        finish();
                    }
                }).show();
            }
        }
        else {
            // ユーザでは対応不可
            Toast.makeText(this, "Google Play Services is not available.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initMap() {
        Log.d("ROUTE_APP", "initMap (" + "" + ")");
        map1 = frgMap1.getMap();
        map1.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(MyActivity.this);
                TextView tv = new TextView(getApplicationContext());
                if(stationGoal != null
                        && stationStart.name.equals(stationGoal.name)) {
                    dialog.setTitle("「現在地から" + "目的地" + "」");
                }
                else {
                    dialog.setTitle("「現在地から" + stationStart.name + "駅" + "」");
                }

                tv.setMovementMethod(LinkMovementMethod.getInstance());
                tv.setText(Html.fromHtml(route1.route));
                dialog.setView(tv);
                dialog.setPositiveButton("OK", null);
                dialog.show();
            }
        });

        map2 = frgMap2.getMap();
        map2.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(MyActivity.this);
                TextView tv = new TextView(getApplicationContext());
                dialog.setTitle("「" + stationGoal.name + "駅から" + address.getFeatureName()  + "」");
                tv.setMovementMethod(LinkMovementMethod.getInstance());
                tv.setText(Html.fromHtml(route2.route));
                dialog.setView(tv);
                dialog.setPositiveButton("OK", null);
                dialog.show();
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("ROUTE_APP", "onLocationChanged (" + location.getLatitude() + "," + location.getLongitude() + ")");
        float[] results = new float[1];
        if(start != null) {
            Location.distanceBetween(start.latitude, start.longitude, location.getLatitude(), location.getLongitude(), results);
        }
        if(start == null
        || results[0] > 10) {
            locationChanged(location);
        }
//        // 現在地取得停止
//        locationClient.removeLocationUpdates(MyActivity.this);
//        map1.clear();
//        Log.d("DEBUG", "onLocationChanged (" + location.getLatitude() + "," + location.getLongitude() + ")");
//        start = new LatLng(location.getLatitude(), location.getLongitude());
//        CameraPosition.Builder builder = new CameraPosition.Builder().bearing(CAMERA_BEARING)
//                                                                     .tilt(CAMERA_TILT)
//                                                                     .zoom(CAMERA_ZOOM).target(start);
//
//        map1.animateCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
//
//        MarkerOptions options = new MarkerOptions();
//        options.position(new LatLng(start.latitude, start.longitude));
//        options.title("現在地");
//        map1.addMarker(options);
//
//        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//        fragmentTransaction.show(frgMap1);
//        fragmentTransaction.hide(frgMap2);
//        fragmentTransaction.commit();
//        webView.setVisibility(View.GONE);
//
//        // 現在地取得停止
//        locationClient.removeLocationUpdates(this);
//
//        getStation1();
    }

    private void locationChanged(Location location) {
        Log.d("ROUTE_APP", "locationChanged (" + location.getLatitude() + "," + location.getLongitude() + ")");
        map1.clear();

        start = new LatLng(location.getLatitude(), location.getLongitude());
        CameraPosition.Builder builder = new CameraPosition.Builder().bearing(CAMERA_BEARING)
                .tilt(CAMERA_TILT)
                .zoom(CAMERA_ZOOM).target(start);

        map1.animateCamera(CameraUpdateFactory.newCameraPosition(builder.build()));

        MarkerOptions options = new MarkerOptions();
        options.position(new LatLng(start.latitude, start.longitude));
        options.title("現在地");
        map1.addMarker(options);

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.show(frgMap1);
        fragmentTransaction.hide(frgMap2);
        fragmentTransaction.commit();
        webView.setVisibility(View.GONE);

        getStation1();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("ROUTE_APP", "onConnected (" + "" + ")");
        // 現在地取得開始
        locationClient.requestLocationUpdates(REQUEST, this); // LocationListener
    }

    @Override
    public void onDisconnected() {
        Log.d("ROUTE_APP", "onDisconnected (" + "" + ")");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("ROUTE_APP", "onConnectionFailed (" + "" + ")");
    }

    /**
     * 駅情報クラス
     */
    public class Station {
        public String name;
        public String line;
        public double x;
        public double y;
        public String distance;
    }

    /**
     * ルート情報
     */
    public class Route {
        public String addressFrom;
        public String addressTo;
        public String route;
    }

    /**
     * 駅情報Json → Stationクラス
     * @param response
     * @return
     */
    private List<Station> parseStationInfo(JSONObject response) {
        Log.d("ROUTE_APP", "parseStationInfo (" + "" + ")");
        List<Station> resultList = new ArrayList<Station>();
        try {
            JSONObject feed = response.getJSONObject("response");
            JSONArray jsonArray = feed.getJSONArray("station");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                Station station = new Station();

                String test = json.getString("name");
                station.name = json.getString("name");
                station.line = json.getString("line");
                station.x = json.getDouble("x");
                station.y = json.getDouble("y");
                station.distance = json.getString("distance");
                resultList.add(station);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return resultList;
    }

    private void getStation1() {
        Log.d("ROUTE_APP", "getStation1 (" + start.latitude + ","  + start.longitude + ")");
        String url = "http://express.heartrails.com/api/json?method=getStations&y=" + start.latitude + "&x=" + start.longitude ;
        mQueue.getCache().clear();
        mQueue.add(new JsonRequest(Request.Method.GET, url,  new JSONObject(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("JSON_PARSE", "response : " + response.toString());
                List<Station> stations = parseStationInfo(response);
                if(stations.size() > 0) {
                    stationStart = stations.get(0);

                    MarkerOptions options = new MarkerOptions();
                    options.position(new LatLng(stationStart.y, stationStart.x));
                    options.title(stationStart.name);
                    map1.addMarker(options);

                    if(stationGoal != null
                    && stationStart.name.equals(stationGoal.name)) {
                        getRoot();
                        btnRail.setEnabled(false);
                        btnFromStation.setEnabled(false);
                        btnToStation.setText("目的地まで");
                        isDirect = true;
                    }
                    else {
                        getRoot1();
                        btnToStation.setEnabled(true);
                        btnToStation.setText("駅まで");
                        if(stationGoal != null) {
                            btnRail.setEnabled(true);
                        }
                    }

                }
            }
        }, null));
    }

    private void getStation2() {
        Log.d("ROUTE_APP", "getStation2 (" + goal.latitude + ","  + goal.longitude + ")");
        String url = "http://express.heartrails.com/api/json?method=getStations&y=" + goal.latitude + "&x=" + goal.longitude ;
        mQueue.getCache().clear();
        mQueue.add(new JsonRequest(Request.Method.GET, url,  new JSONObject(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("JSON_PARSE", "response : " + response.toString());
                List<Station> stations = parseStationInfo(response);
                if(stations.size() > 0) {
                    stationGoal = stations.get(0);

                    MarkerOptions options = new MarkerOptions();
                    options.position(new LatLng(stationGoal.y, stationGoal.x));
                    options.title(stationGoal.name);
                    map2.addMarker(options);

                    if(stationStart != null
                    && stationGoal.name.equals(stationStart.name)) {
                        getRoot();
                        btnRail.setEnabled(false);
                        btnFromStation.setEnabled(false);
                        btnToStation.setText("目的地まで");
                        btnToStation.callOnClick();
                        isDirect = true;
                    }
                    else {
                        getRoot2();
                        btnFromStation.setEnabled(true);
                        btnToStation.setText("駅まで");
                        if(stationStart != null) {
                            btnRail.setEnabled(true);
                        }
                    }
                }
            }
        }, null));
    }

    public void getRoot1() {
        Log.d("ROUTE_APP", "getRoot1 ((" + start.latitude + ","  + start.longitude + "),(" + stationStart.y + "," + stationStart.x + "))");
        String url = "https://maps.googleapis.com/maps/api/directions/json?sensor=false"
                + "&origin=" + start.latitude + "," + start.longitude
                + "&destination=" + stationStart.y + "," + stationStart.x
                + "&language=ja"
                + "&mode=walking";

        mQueue.getCache().clear();
        mQueue.add(new JsonRequest(Request.Method.GET, url,  new JSONObject(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                route1 = new Route();
                List<List<HashMap<String,String>>> route = parseRouteInfo(response, route1);
                drawRoute(route, map1);
                {
                    MarkerOptions options = new MarkerOptions();
                    options.position(new LatLng(start.latitude, start.longitude));
                    options.title("現在地");
                    map1.addMarker(options);
                }
                {
                    MarkerOptions options = new MarkerOptions();
                    options.position(new LatLng(stationStart.y, stationStart.x));
                    options.title(stationStart.name);
                    map1.addMarker(options);
                }
            }
        }, null));
    }

    public void getRoot2() {
        Log.d("ROUTE_APP", "getRoot2 ((" + stationGoal.y + ","  + stationGoal.x + "),(" + goal.latitude + "," + goal.longitude + "))");
        String url = "https://maps.googleapis.com/maps/api/directions/json?sensor=false"
                + "&origin=" + stationGoal.y + "," + stationGoal.x
                + "&destination=" + goal.latitude + "," + goal.longitude
                + "&language=ja"
                + "&mode=walking";

        mQueue.getCache().clear();
        mQueue.add(new JsonRequest(Request.Method.GET, url,  new JSONObject(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                route2 = new Route();
                List<List<HashMap<String,String>>> route = parseRouteInfo(response, route2);
                drawRoute(route, map2);
                {
                    MarkerOptions options = new MarkerOptions();
                    options.position(new LatLng(stationGoal.y, stationGoal.x));
                    options.title(stationGoal.name);
                    map2.addMarker(options);
                }
                {
                    MarkerOptions options = new MarkerOptions();
                    options.position(new LatLng(goal.latitude, goal.longitude));
                    options.title(address.getFeatureName());
                    map2.addMarker(options);
                }
            }
        }, null));
    }

    public void getRoot() {
        Log.d("ROUTE_APP", "getRoot ((" + start.latitude + ","  + start.longitude + "),(" + goal.latitude + "," + goal.longitude + "))");
        String url = "https://maps.googleapis.com/maps/api/directions/json?sensor=false"
                + "&origin=" + start.latitude + "," + start.longitude
                + "&destination=" + goal.latitude + "," + goal.longitude
                + "&language=ja"
                + "&mode=walking";

        mQueue.getCache().clear();
        mQueue.add(new JsonRequest(Request.Method.GET, url,  new JSONObject(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                route1 = new Route();
                List<List<HashMap<String,String>>> route = parseRouteInfo(response, route1);
                drawRoute(route, map1);
                {
                    MarkerOptions options = new MarkerOptions();
                    options.position(new LatLng(start.latitude, start.longitude));
                    options.title("現在地");
                    map1.addMarker(options);
                }
                {
                    MarkerOptions options = new MarkerOptions();
                    options.position(new LatLng(goal.latitude, goal.longitude));
                    options.title(address.getFeatureName());
                    map1.addMarker(options);
                }
            }
        }, null));
    }

    private List<List<HashMap<String,String>>> parseRouteInfo(JSONObject response, Route route) {
        Log.d("ROUTE_APP", "parseRouteInfo (" + "" + ")");
        List<List<HashMap<String, String>>> resultList = new ArrayList<List<HashMap<String,String>>>() ;
        JSONArray jsonRoutes = null;
        JSONArray jsonLegs = null;
        JSONArray jsonSteps = null;
        String temp = "";
        try {

            jsonRoutes = response.getJSONArray("routes");

            for(int i=0;i<jsonRoutes.length();i++){
                jsonLegs = ( (JSONObject)jsonRoutes.get(i)).getJSONArray("legs");

                //スタート地点・住所
                route.addressFrom = (String)((JSONObject)(JSONObject)jsonLegs.get(i)).getString("start_address");
                //到着地点・住所
                route.addressTo = (String)((JSONObject)(JSONObject)jsonLegs.get(i)).getString("end_address");

                String distance_txt = (String)((JSONObject)((JSONObject)jsonLegs.get(i)).get("distance")).getString("text");
//                temp += distance_txt + LINE_BREAK +LINE_BREAK;
                String distance_val = (String)((JSONObject)((JSONObject)jsonLegs.get(i)).get("distance")).getString("value");
//                temp += distance_val + LINE_BREAK +LINE_BREAK;
                temp += "距離：" + distance_txt + "(" + distance_val + "m)" + LINE_BREAK +LINE_BREAK;
                List path = new ArrayList<HashMap<String, String>>();

                for(int j=0;j<jsonLegs.length();j++){
                    jsonSteps = ( (JSONObject)jsonLegs.get(j)).getJSONArray("steps");

                    for(int k=0;k<jsonSteps.length();k++){
                        String polyline = "";
                        polyline = (String)((JSONObject)((JSONObject)jsonSteps.get(k)).get("polyline")).get("points");

                        String instructions = (String)((JSONObject)(JSONObject)jsonSteps.get(k)).getString("html_instructions");
                        String duration_value = (String)((JSONObject)((JSONObject)jsonSteps.get(k)).get("duration")).getString("value");
                        String duration_txt = (String)((JSONObject)((JSONObject)jsonSteps.get(k)).get("duration")).getString("text");

                        temp += instructions + "/" + duration_value + " m /" + duration_txt + LINE_BREAK;

                        List<LatLng> list = decodePoly(polyline);

                        for(int l=0;l<list.size();l++){
                            HashMap<String, String> hm = new HashMap<String, String>();
                            hm.put("lat", Double.toString(((LatLng)list.get(l)).latitude) );
                            hm.put("lng", Double.toString(((LatLng)list.get(l)).longitude) );
                            path.add(hm);
                        }
                    }
                    //ルート座標
                    resultList.add(path);
                }
            }
            //ルート情報
            route.route = temp;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return resultList;

    }
    private List<LatLng> decodePoly(String encoded) {
        Log.d("ROUTE_APP", "decodePoly (" + encoded + ")");

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }


    private void drawRoute(List<List<HashMap<String, String>>> route, GoogleMap map) {
        Log.d("ROUTE_APP", "drawRoute (" + "" + ")");
        ArrayList<LatLng> points = null;
        PolylineOptions lineOptions = null;
        MarkerOptions markerOptions = new MarkerOptions();
        map.clear();
        if(route.size() != 0){

            for(int i=0;i<route.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();


                List<HashMap<String, String>> path = route.get(i);


                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                //ポリライン
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(0x550000ff);

            }

            //描画
            map.addPolyline(lineOptions);
        }else{
            map.clear();
            Toast.makeText(this, "ルート情報を取得できませんでした", Toast.LENGTH_LONG).show();
        }
        //progressDialog.hide();

    }

    /**
     * 目的地を検索する
     * @return
     */
    private List<Address> getDestination(String searchWord) {
        Log.d("ROUTE_APP", "getDestination (" + searchWord + ")");
        List<Address> resultList = new ArrayList<Address>();

        Geocoder geocoder = new Geocoder(MyActivity.this , Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocationName(searchWord, 5);
            if(addressList != null && addressList.size()>0){
                resultList = addressList;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultList;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("ROUTE_APP", "onActivityResult (" + requestCode + "," + resultCode + "," + data.toString() + ")");

        // ====================
        // 音声入力結果受取
        // ====================
        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK) {
            String _destination = new String();
            ArrayList<String> strList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//            for (String str: strList) {
//                if(_destination.length() > 0) {
//                    _destination += " ";
//                }
//                _destination += str;
//            }
            if(strList != null && strList.size() > 0) {
                _destination = strList.get(0);
            }

            // 目的地テキストボックスに代入
            txtDestination.setText(_destination);

            // 検索ボタン押下
            btnSearch.callOnClick();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
