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


public class tester extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap map;
    Location loc;
    LocationManager m;
    StringBuilder temp;

    public void onLocationRequest(){
        m = (LocationManager) getSystemService(LOCATION_SERVICE);
        loc = m.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
    }
    public void onNewLocation() {
        onLocationRequest();
        MarkerOptions options = new MarkerOptions();
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
        // Ändern der GoogleMaps
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        // Deaktiviren der Google Buttons für GoogleMaps und Google Routing Funktionen
        //map.getUiSettings().setMapToolbarEnabled(false);;


    }

    public String buildURL(){
        /* Hier wird über einen StringBuilder die http Adresse zur aktuellen POI Abfrage über eine json-Datei realisert.
        * Die http-Suchanfrage hat die Form: https://maps.googleapis.com/maps/api/place/nearbysearch/json
        * ?location=STRING LATITUDE, STRING LONGITUDE&radius=1000&name=rewe&key=YOUR_GOOGLE_KEY
        * Feste Latitude und Longitude Werte um String Builder zu testen.
        * final String latitude = "50.06226155";
        * final String longitude = "8.21733695";
        */
        // Todo Noch zu klären wie es möglich ist, ohne wetere Location und LocationManager Instanz hier fortzufahren.
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location location = manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        final String latitude = ""+location.getLatitude();
        final String longitude = ""+location.getLongitude();
        final String GOOGLE_KEY = "AIzaSyBOy5KtLlnqgdSvDn7QFlZGJv_NA02GrP8 ";

        temp = new StringBuilder("https://maps.googleapis.com/maps/api/place/search/json?location=");
        temp.append(latitude+",");
        temp.append(longitude);
        temp.append("&radius=500");
        //temp.append("&types=grocery_or_supermarket");
        temp.append("&name=rewe");
        temp.append("&sensor=true");
        temp.append("&key=");
        temp.append(GOOGLE_KEY);

        String urlResult = temp.toString();
        //Log.d("------------>: ", urlResult);
        return urlResult;

    }

    private class MarkerTask extends AsyncTask<Void, Void, String> {

        private static final String LOG_TAG = "TestApp";
        HttpURLConnection connection = null;
        // Invoked by execute() method of this object

        @Override
        protected String doInBackground(Void... args) {

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

        // Wird nach der kompletten Ausführung der Methode protected String doInBackground(Void... args) ausgeführt
        @Override
        protected void onPostExecute(String json) {

            try {
                // De - Serialisierung des JSON-String in ein Array mit Geometrie-Objekten.
                JSONObject mainObj= new JSONObject(json);
                // Das Array in dem die GEometrie-Objekte enthalten sind, heisst results.
                JSONArray jArray = mainObj.optJSONArray("results");
                /*
                 *
                 * Wenn die json Abfrage erledigt wurde, wird geschaut ob POI(s) von Typ grocery_or_supermarket gefunden
                 * wurden. Daraufin wird bei jArray != null eine Testweise Handlungsempfehlung Bananen kaufen ausgegeben.
                 *
                */
                for (int i = 0; i < jArray.length(); i++) {
                    JSONObject temp = jArray.optJSONObject(i);
                    JSONObject loc = temp.optJSONObject("geometry").optJSONObject("location");

                    String stillOpen = jArray.getJSONObject(i).getJSONObject("opening_hours").getString("open_now");
                    double lat = Double.parseDouble(loc.getString("lat"));
                    double lng = Double.parseDouble(loc.getString("lng"));
                    LatLng latLng = new LatLng(lat,lng);
                    String resultFilter = jArray.getJSONObject(i).optString("name");

                    /*
                     * Wenn die json Abfrage erledigt wurde, wird geschaut ob POI(s) mit dem Namen Rewe oder Aldi Süd vorhanden sind
                     * Daraufin wird durch den Aufruf von toast.show eine Handlungsempfehlung ausgegeben.
                     *
                    */
                    if (resultFilter.equalsIgnoreCase("rewe") | resultFilter.equalsIgnoreCase("aldi süd") && stillOpen.equals(false)){
                        // Todo: Bei einem Treffer die Farbe der dazugehörigen POI-Marker ändern.
                        Context context = getApplicationContext();
                        String geschaeft = jArray.getJSONObject(i).optString("name");
                        String adresse = jArray.getJSONObject(i).optString("vicinity");
                        CharSequence text = "Eine eingetragene To-do kann hier "+":"+ geschaeft +", " + adresse+ " "+"erledigt werden." ;
                        int duration = Toast.LENGTH_LONG;

                        Toast toast = Toast.makeText(context, text, duration);
                        // Ausgabe der Handlungsempfehlung
                        toast.show();
                    }
                    //Kamera Bewegung zum ersten angezeigten POI.
                    if (i == 0) {
                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                .target(latLng).zoom(12).build();

                        map.animateCamera(CameraUpdateFactory
                                .newCameraPosition(cameraPosition));
                    }

                    // Für jeden POI wird auf der Karte ein Marker erzeugt.
                    JSONObject attr = temp.optJSONObject("geometry");
                    map.addMarker(new MarkerOptions()
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                            .title(jArray.getJSONObject(i).optString("name"))
                            // Todo Eventuell noch anzeigen, ob Geschaeft noch offen aus String stillOpen
                            .snippet(jArray.getJSONObject(i).optString("vicinity"))// +"Geschäft geöffnet?: "+stillOpen))
                            .position(latLng));
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error processing JSON", e);
            }

        }
    }


}