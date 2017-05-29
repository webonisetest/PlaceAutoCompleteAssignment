package jayesh.shah.placesautocomplete.helper;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * Created by Jayesh on 27/05/17.
 *
 * Helper class to parse JSON data
 */

public class JSONParserHelper {

    private static final String TAG = JSONParserHelper.class.getSimpleName();

    /**
     *
     * Parse JSON Data
     * @param response JSON input from server
     * @param place searched place
     * @param placeID searched placeID
     * @return PlacesHelper
     * @throws JSONException exception thrown my method
     */
    public PlacesHelper parseLocationAddressDetailsResponse(String response, String place, String placeID) throws JSONException {

        PlacesHelper placesHelper = null;

        if (!response.isEmpty() && response.length() > 0) {
            ArrayList<String> photoReferencesList = null;
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(response);

            //Below if structure is necessary to ensure all objects are present in response,
            // it may happen due to max query reached or service failure to have different response.
            JSONObject resultObject = jsonObj.optJSONObject("result");

            if (resultObject != null) {

                JSONObject geometryObject = resultObject.optJSONObject("geometry");

                if (geometryObject != null) {
                    placesHelper = new PlacesHelper();
                    JSONObject location = geometryObject.optJSONObject("location");
                    placesHelper.setLatitude(Double.valueOf(location.getString("lat")));
                    placesHelper.setLongitude(Double.valueOf(location.getString("lng")));
                    placesHelper.setPlaceID(placeID);
                    placesHelper.setPlace(place);
                }

                JSONArray photoRefsArray = resultObject.optJSONArray("photos");
                if (photoRefsArray != null) {
                    int count = photoRefsArray.length();

                    if (count > 0) {
                        photoReferencesList = new ArrayList<>();

                        for (int i = 0; i < count; i++) {
                            photoReferencesList.add(photoRefsArray.getJSONObject(i).getString("photo_reference"));
                        }
                    }

                    placesHelper.setPhotoReferencesList(photoReferencesList);
                }
            }
        }

        return placesHelper;
    }

    /**
     * Parse JOSN Data received against place details for place latitude and longitude
     * @param response JSON input data
     * @return PlaceHelper
     */
    public PlacesHelper parseLatLngDetailsResponse(String response) throws JSONException {

        PlacesHelper placesHelper = null;

        if (!response.isEmpty() && response.length() > 0) {
            ArrayList<String> photoReferencesList = null;
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(response);


            //Below if structure is necessary to ensure all objects are present in response,
            // it may happen due to max query reached or service failure to have different response.
            JSONArray resultsArray = jsonObj.optJSONArray("results");
            if (resultsArray != null && resultsArray.length() > 0) {

                JSONObject object = resultsArray.optJSONObject(0);

                if (object != null) {

                    placesHelper = new PlacesHelper();

                    placesHelper.setPlace(object.getString("name"));
                    Log.e(TAG, "PLACE: " + placesHelper.getPlace());
                    placesHelper.setPlaceID(object.getString("place_id"));

                    JSONArray photoRefsArray = object.optJSONArray("photos");
                    if (photoRefsArray != null) {
                        int count = photoRefsArray.length();

                        if (count > 0) {
                            photoReferencesList = new ArrayList<>();

                            for (int i = 0; i < count; i++) {
//                                Log.e(TAG, "REF: " + i + " : " + photoRefsArray.getJSONObject(i).getString("photo_reference"));
                                photoReferencesList.add(photoRefsArray.getJSONObject(i).getString("photo_reference"));
                            }
                        }

                        placesHelper.setPhotoReferencesList(photoReferencesList);
                    }
                }
            }
        }

        return placesHelper;
    }
}
