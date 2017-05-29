package jayesh.shah.placesautocomplete.network;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import jayesh.shah.placesautocomplete.R;
import jayesh.shah.placesautocomplete.helper.JSONParserHelper;
import jayesh.shah.placesautocomplete.helper.PlacesHelper;

/**
 * Created by Jayesh on 26/05/17.
 *
 * Get places details task
 */

public class PlacesDetailsTask extends AsyncTask<String, Void, PlacesHelper> {

    private static final String TAG = PlacesDetailsTask.class.getSimpleName();

    // Constant for type of place details, either from address or from latLng
    public static final String PLACE_DETAILS_TYPE_ADDRESS = "address";
    public static final String PLACE_DETAILS_TYPE_LAT_LNG = "latLng";

    // Member variables
    private IPlacesDetails mPlacesDetailsCallback;
    private String mType;
    private double mLatitude, mLongitude;

    /**
     *  Set type of place input either address or latLng
     * @param detailsType details type
     */
    public PlacesDetailsTask(String detailsType) {
        mType = detailsType;
    }

    /**
     *
     * Callback for Places details task
     * @param callback calllBack for place details
     */
    public void setPlacesDetailsCallback(IPlacesDetails callback) {
        mPlacesDetailsCallback = callback;
    }

    /**
     *
     *
     * @param latitude Latitude value
     * @param longitude Longitude value
     */
    public void setLatLngForDetails(double latitude, double longitude) {

        mLatitude = latitude;
        mLongitude = longitude;
    }

    @Override
    protected PlacesHelper doInBackground(String... params) {

        String place = null;
        String placeID = null;

        // Place details urls
        final String GET_PLACES_DETAILS_LOCATION_BASE_URL = "https://maps.googleapis.com/maps/api/place/details/json?placeid=";
        final String GET_PLACES_DETAILS_LAT_LNG_BASE_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=";
        final String RADIUS = "&radius=10";

        final String KEY = "&key=";
        final String API_KEY = "AIzaSyBoyDc40todngnDnkrGYA2Tr_1bKcocmTc";

        PlacesHelper placesHelper = null;
        HttpURLConnection httpURLConnection = null;

        try {

            StringBuilder urlBuilder = new StringBuilder();

            if (mType.equals(PlacesDetailsTask.PLACE_DETAILS_TYPE_ADDRESS)) {

                urlBuilder.append(GET_PLACES_DETAILS_LOCATION_BASE_URL);
                place = params[0];
                placeID = params[1];
                urlBuilder.append(placeID);

            } else {

                urlBuilder.append(GET_PLACES_DETAILS_LAT_LNG_BASE_URL);
                urlBuilder.append(mLatitude);
                urlBuilder.append(",");
                urlBuilder.append(mLongitude);
                urlBuilder.append(RADIUS);
            }

            urlBuilder.append(KEY);
            urlBuilder.append(API_KEY);

            String placeDetailsUrl = urlBuilder.toString();

            Log.i(TAG, "PLACES DETAILS URL: " + placeDetailsUrl);

            URL url = new URL(placeDetailsUrl);
            StringBuilder jsonResults = new StringBuilder();

            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(30000);
            httpURLConnection.setReadTimeout(30000);

            InputStreamReader in = new InputStreamReader(httpURLConnection.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }

            if ((httpURLConnection.getResponseCode() < 200)
                    || (httpURLConnection.getResponseCode() > 299)) {

                return placesHelper;
            }

            JSONParserHelper jsonParserHelper = new JSONParserHelper();

            if (mType.equals(PLACE_DETAILS_TYPE_ADDRESS)) {

                placesHelper = jsonParserHelper.parseLocationAddressDetailsResponse(jsonResults.toString(), place, placeID);

            } else {

                placesHelper = jsonParserHelper.parseLatLngDetailsResponse(jsonResults.toString());
            }

        } catch (MalformedURLException e) {
            Log.e(TAG, "Error processing Places API URL", e);
            return placesHelper;
        } catch (IOException e) {
            Log.e(TAG, "Error connecting to Places API", e);
            return placesHelper;
        } catch (JSONException e) {
            Log.e(TAG, "Cannot process JSON results", e);

            return placesHelper;
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }


        return placesHelper;
    }

    @Override
    protected void onPostExecute(PlacesHelper placesHelper) {
        super.onPostExecute(placesHelper);

        if (placesHelper == null) {
            mPlacesDetailsCallback.showToast(R.string.error_occurred);
        } else {
            mPlacesDetailsCallback.placesDetails(placesHelper);
        }
    }

    /**
     *
     *  Callback for places details.
     */
    public interface IPlacesDetails {

        void showToast(int messageID);

        void placesDetails(PlacesHelper placesHelper);
    }
}
