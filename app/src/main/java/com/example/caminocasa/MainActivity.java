package com.example.caminocasa;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    MapView map = null;

    private MyLocationNewOverlay mLocationOverlay;
    private RotationGestureOverlay mRotationGestureOverlay;

    private RequestQueue queue;
    private JsonObjectRequest requestMapRequest;
    private FusedLocationProviderClient fusedLocationClient;

    Double myHouseLat=16.435997;
    Double myHouseLon=-95.009321;
    private ArrayList<GeoPoint> puntosDeRuta = new ArrayList<>();
    ArrayList<Polyline> listaPuntos;
    Double myLat;
    Double myLon;
    FloatingActionButton fab;
    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //btn = findViewById(R.id.btn_eliminar);
        fab = findViewById(R.id.fab);
        super.onCreate(savedInstanceState);

        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string

        //inflate and create the map
        setContentView(R.layout.activity_main);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        map.setMultiTouchControls(true);

        IMapController mapController = map.getController();
        mapController.setZoom(19.0);
        GeoPoint startPoint = new GeoPoint(20.139476, -101.150737);
        mapController.setCenter(startPoint);

        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx
        ),map);
        this.mLocationOverlay.enableMyLocation();
        map.getOverlays().add(this.mLocationOverlay);

        mRotationGestureOverlay = new RotationGestureOverlay(ctx, map);
        mRotationGestureOverlay.setEnabled(true);
        map.getOverlays().add(this.mRotationGestureOverlay);

        //your items
        ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
        items.add(new OverlayItem("Title", "Description", new GeoPoint(20.139476d,
                -101.150737d))); // Lat/Lon decimal degrees

        //the overlay
        ItemizedOverlayWithFocus<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        //do something
                        return true;
                    }
                    @Override
                    public boolean onItemLongPress(final int index, final OverlayItem item) {
                        return false;
                    }
                }, ctx);
        mOverlay.setFocusItemsOnTap(true);

        //map.getOverlays().add(mOverlay);

        Marker startMarker = new Marker(map);

        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(startMarker);

        List<GeoPoint> geoPoints = new ArrayList<>();

        geoPoints.add(startPoint);
        geoPoints.add(new GeoPoint(20.141468d, -101.150406d));
//add your points here
        Polyline line = new Polyline();   //see note below!
        line.setPoints(geoPoints);
        line.setOnClickListener(new Polyline.OnClickListener() {
            @Override
            public boolean onClick(Polyline polyline, MapView mapView, GeoPoint eventPos) {
                Toast.makeText(mapView.getContext(), "polyline with " + polyline.getPoints().size() + "pts was tapped", Toast.LENGTH_LONG).show();
                return false;
            }
        });
        //map.getOverlayManager().add(line);
        //ultimaUbicacionCOnocida();

        obtenerRouteFromMapRequest(20.139730, -101.150765);
        /*fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(ctx, "Ruta camino a casa predeterminada", Toast.LENGTH_SHORT).show();
                obtenerRouteFromMapRequest(myLat,myLon);
                //map.getOverlayManager().remove(0);
            }
        });*/


        /*fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("Mi lat1: " + myLat + " Mi 1lonn: " + myLon);
                obtenerRouteFromMapRequest(myLat,myLon);

            }
        });*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up

    }

    private void obtenerRouteFromMapRequest(Double latitud, Double longitud){
        System.out.println("http://www.mapquestapi.com/directions/v2/route?key=EwNXPABPnLR6R250CknmVJPk8ZGaFnqp&from="+latitud+","+longitud+"&to=16.436005, -95.009365");
        puntosDeRuta=new ArrayList<>();
        queue =
                Volley.newRequestQueue(this);
        requestMapRequest =
                new JsonObjectRequest(
                        "http://www.mapquestapi.com/directions/v2/route?key=EwNXPABPnLR6R250CknmVJPk8ZGaFnqp&from="+latitud+","+longitud+"&to=16.436005, -95.009365",
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                Log.d("GIVO", "se ejecuto");
                                try {
                                    JSONArray indicaiones =  response.getJSONObject("route")
                                            .getJSONArray("legs")
                                            .getJSONObject(0).
                                                    getJSONArray("maneuvers");
                                    for( int i =0 ;  i <indicaiones.length(); i++){
                                        JSONObject indi = indicaiones.getJSONObject(i);
                                        GeoPoint punto = new GeoPoint(
                                                Double.parseDouble(indi.getJSONObject("startPoint").get("lat").toString()),
                                                Double.parseDouble(indi.getJSONObject("startPoint").get("lng").toString()));
                                        puntosDeRuta.add(punto);
                                        String strlatlog = indi.getJSONObject("startPoint").get("lat").toString()
                                                + "," +
                                                indi.getJSONObject("startPoint").get("lng").toString();

                                        Log.d("GIVO", "se ejecuto: " +  strlatlog );

                                    }
                                    Polyline line = new Polyline();   //see note below!
                                    line.setPoints(puntosDeRuta);
                                    map.getOverlayManager().add(line);
                                    //listaPuntos.add(line);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("GIVO", "Error "
                                );

                            }
                        }
                );
        queue.add(requestMapRequest);
    }
    private void ultimaUbicacionCOnocida() {
        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(
                this,
                location -> {
                    if(location!=null){
                        String ubica = "Lat: " + location.getLatitude()
                                + ", lon: " + location.getLongitude();
                        myLat = location.getLatitude();
                        myLon = location.getLongitude();
                        System.out.println("Mi lat: " + myLat + "Mi lonn: " + myLon);
                    }else {
                        Log.d ("UBIX", "Location null");
                    }
                }
        );

    }
    @Override
    protected void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }
}