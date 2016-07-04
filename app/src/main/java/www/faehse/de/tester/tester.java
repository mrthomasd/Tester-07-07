package www.faehse.de.tester;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.widget.Toast;

import java.lang.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

public class tester extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap map;
    Location loc;
    StringBuilder temp;

    public void onNewLocation() {
        MarkerOptions options = new MarkerOptions();
        LocationManager m = (LocationManager) getSystemService(LOCATION_SERVICE);
        loc = m.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        if (loc != null) {
            options.position(new LatLng(loc.getLatitude(), loc.getLongitude()));
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            options.title("Aktueller Standort");
            map.addMarker(options);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tester);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        new MarkerTask().execute();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        onNewLocation();
        // Sets the map type to be "hybrid"
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);


    }

    //Google json Anfrage String
    final String GOOGLE_KEY = "AIzaSyBOy5KtLlnqgdSvDn7QFlZGJv_NA02GrP8 ";
    /*
    DecimalFormat decimalFormat = new DecimalFormat("#.###");

        final String latitude = decimalFormat.format(Float.valueOf(Location.convert(loc.getLatitude(), Location.FORMAT_DEGREES)));
        final String longitude = decimalFormat.format(Float.valueOf(Location.convert(loc.getLongitude(), Location.FORMAT_DEGREES)));
    */
    final String latitude = "50.06226155";
    final String longitude = "8.21733695";

    public String buildURL(){
        temp = new StringBuilder("https://maps.googleapis.com/maps/api/place/search/json?location=");
        temp.append(latitude+",");
        temp.append(longitude);
        temp.append("&radius=5000");
        temp.append("&types=grocery_or_supermarket");
        //temp.append("&name=rewe");
        temp.append("&sensor=true");
        temp.append("&key=");
        temp.append(GOOGLE_KEY);

        String urlResult = temp.toString();
        return urlResult;

    }

    private class MarkerTask extends AsyncTask<Void, Void, String> {

        private static final String LOG_TAG = "TestApp";
        HttpURLConnection connection = null;
        // Invoked by execute() method of this object

        @Override
        protected String doInBackground(Void... args) {

            //HttpURLConnection conn = null;
            final StringBuilder json = new StringBuilder();
            try {
                // Connect to the web service
                buildURL();
                URL url = new URL(buildURL());

                connection = (HttpURLConnection) url.openConnection();
                InputStreamReader in = new InputStreamReader(connection.getInputStream());

                // Read the JSON data into the StringBuilder
                int read;
                char[] buff = new char[1024];
                while ((read = in.read(buff)) != -1) {
                    json.append(buff, 0, read);
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error connecting to service", e);
                //throw new IOException("Error connecting to service", e); //uncaught
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            return json.toString();
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(String json) {

            try {
                // De-serialize the JSON string into an array geometry objects
                JSONObject mainObj= new JSONObject(json);
                JSONArray jArray = mainObj.optJSONArray("results");
               /*
               * Wenn die json Abfrage erledigt wurde, wird geschaut ob POI(s) von Typ grocery_or_supermarket gefunden
               * wurden. Daraufin wird bei jArray != null eine Testweise Handlungsempfehlung Bananen kaufen ausgegeben.
               *
               */
                if(jArray != null) {
                    Context context = getApplicationContext();
                    CharSequence text = "Bananen kaufen";
                    int duration = Toast.LENGTH_LONG;

                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
                for (int i = 0; i < jArray.length(); i++) {
                    //JSONObject jsonObj = jArray.optJSONObject(i);

                    JSONObject temp = jArray.optJSONObject(i);
                    JSONObject loc = temp.optJSONObject("geometry").optJSONObject("location");
                    double lat = Double.parseDouble(loc.getString("lat"));
                    double lng = Double.parseDouble(loc.getString("lng"));
                    LatLng latLng = new LatLng(lat,lng);

                    //move CameraPosition on first result
                    if (i == 0) {
                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                .target(latLng).zoom(13).build();

                        map.animateCamera(CameraUpdateFactory
                                .newCameraPosition(cameraPosition));
                    }

                    // Create a marker for each city in the JSON data.
                    JSONObject attr = temp.optJSONObject("geometry");
                    map.addMarker(new MarkerOptions()
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                            .title(jArray.getJSONObject(i).optString("name"))
                            .snippet(jArray.getJSONObject(i).optString("vicinity"))
                            .position(latLng));
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error processing JSON", e);
            }

        }
    }


}