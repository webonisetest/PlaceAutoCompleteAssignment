package jayesh.shah.placesautocomplete.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import jayesh.shah.placesautocomplete.R;

/**
 * Created by Jayesh on 26/05/17.
 * <p>
 * An adapter for AutoComplete Places.
 */

public class AutoCompleteAdapter extends ArrayAdapter<String> implements
        Filterable {

    private static final String TAG = AutoCompleteAdapter.class.getSimpleName();

    // Place suggestions URL constants.
    private static final String PLACES_API_BASE = " https://maps.googleapis.com/maps/api/place/autocomplete/json";
    private static final String KEY = "?key=";
    private static final String INPUT = "&input=";
    private static final String API_KEY = "AIzaSyBoyDc40todngnDnkrGYA2Tr_1bKcocmTc";

    // Member variables.
    private Context mContext;
    private ArrayList<String> mResultList;
    private int mTextViewResourceId;
    private HttpURLConnection mHttpURLConnection;
    private IAutoCompleteAdapterCallback mAutoCompleteAdapterCallback;
    private HashMap<String, String> mPlacesMap, mSearchedPlacesMap;
    private ArrayList<String> mSearchedPlacesList;


    /**
     * @param context            Context provided.
     * @param textViewResourceId view for showing suggestions.
     */
    public AutoCompleteAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        mContext = context;
        mTextViewResourceId = textViewResourceId;
        mPlacesMap = new HashMap<>();
    }

    /**
     * @param autoCompleteAdapterCallback AutoCompleteAdapter callback
     */
    public void setAutoCompleteAdapterCallback(IAutoCompleteAdapterCallback autoCompleteAdapterCallback) {

        mAutoCompleteAdapterCallback = autoCompleteAdapterCallback;
    }

    /**
     * @param searchedPlacesList List of searched places
     */
    public void setSearchedPlacesList(ArrayList<String> searchedPlacesList) {
        mSearchedPlacesList = (ArrayList<String>) searchedPlacesList.clone();

    }

    /**
     *
     * @param searchedPlacesMap searched places map
     */
    public void setSearchedLocationMap(HashMap<String, String> searchedPlacesMap) {
        mSearchedPlacesMap = (HashMap<String, String>) searchedPlacesMap.clone();
    }

    /**
     *
     * Add searched places while user interacts with applications, this avoids multiple file operations like read
     * provides consistent user experience.
     *
     * @param searchedPlace     Searched place
     * @param searchedPlaceID   Searched place ID
     */
    public void addSearchedPlaceToAdapter(String searchedPlace, String searchedPlaceID) {

        if(mSearchedPlacesMap == null || mSearchedPlacesList == null) {
            mSearchedPlacesList = new ArrayList<>();
            mSearchedPlacesMap = new HashMap<>();
        }

        mSearchedPlacesList.add(searchedPlace);
        mSearchedPlacesMap.put(searchedPlace, searchedPlaceID);

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        // Show places suggestion data in City, country and locality format
        String city, country, locality;

        LayoutInflater mInflater = (LayoutInflater) mContext
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(mTextViewResourceId, parent, false);
            holder = new ViewHolder();
            holder.txtArea = (TextView) convertView.findViewById(R.id.areaTV);
            holder.txtDetails = (TextView) convertView
                    .findViewById(R.id.detailsTV);
            holder.divider = convertView.findViewById(R.id.divider);

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (mResultList != null && mResultList.size() > 0) {
            holder.txtDetails.setVisibility(View.VISIBLE);
            if(position < mResultList.size()) {
                String location = mResultList.get(position);

                int countryIndex = location.lastIndexOf(",");

                if (countryIndex > 0) {
                    country = location.substring(countryIndex + 1);
                    locality = location.substring(0, countryIndex);

                    int cityIndex = locality.lastIndexOf(",");

                    if (cityIndex > 0) {

                        city = locality.substring(cityIndex + 1) + ",";
                        locality = locality.substring(0, cityIndex);

                        holder.txtArea.setText(locality.trim());
                        holder.txtDetails.setText(city.trim() + " "
                                + country.trim());

                    } else {

                        holder.txtArea.setText(locality.trim());
                        holder.txtDetails.setText(country.trim());
                    }
                } else {

                    holder.txtArea.setText(location.trim());
                    holder.txtDetails.setVisibility(View.GONE);
                }
            } else {
                return convertView;
            }

        } else {
            holder.txtArea
                    .setText(mContext.getString(R.string.no_results_found));
            holder.txtDetails.setVisibility(View.GONE);
        }

        return convertView;
    }

    @Override
    public int getCount() {
        int count = -1;
        if (mResultList != null) {
            count = mResultList.size();
        }
        return count;
    }

    @Override
    public String getItem(int index) {
        String item = null;
        if (mResultList != null) {
            item = mResultList.get(index);
        }

        return item;

    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {

                    // This will look for earlier places searched list
                    if (constraint.length() < 3) {
                        if (mSearchedPlacesList != null && mSearchedPlacesList.size() > 0) {

                            mResultList = getSearchedPlacesListForCurrentPlace(constraint);

                        }
                    } else {

                        // Retrieve the autocomplete results.
                        mResultList = autocomplete(constraint.toString());
                    }

                    if (mResultList != null) {
                        // Assign the data to the FilterResults
                        filterResults.values = mResultList;

                        filterResults.count = mResultList.size();
                    }
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if(mAutoCompleteAdapterCallback != null) {
                    mAutoCompleteAdapterCallback.setProgressIndicatorVisibility(ProgressBar.INVISIBLE);
                }
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();

                    if (mAutoCompleteAdapterCallback != null) {
                        mAutoCompleteAdapterCallback.suggestionListUpdated(true);
                    }
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return filter;
    }

    /**
     *
     * Check if as per current user input SearchedLocation List as places starting with current user input.
     * @param searchString input for place search
     * @return list of suggestions
     */
    private ArrayList<String> getSearchedPlacesListForCurrentPlace(CharSequence searchString) {

        String tempStr = (String) searchString;
        ArrayList<String> currentSearchPlacesList = null;

        if (mSearchedPlacesList != null && mSearchedPlacesList.size() > 0
                && tempStr != null) {

            int count = mSearchedPlacesList.size();

            currentSearchPlacesList = new ArrayList<>();

            for (int i = 0; i < count; i++) {

                String place = mSearchedPlacesList.get(i);

                if (place.toLowerCase(Locale.getDefault()).startsWith(tempStr.toLowerCase(Locale.getDefault()))) {
                    currentSearchPlacesList.add(place);
                }

            }
        }
        return currentSearchPlacesList;
    }


    /**
     * Get places autocomplete suggestions for provided input
     *
     * @param input Input for places autocomplete
     * @return List of results
     */
    private ArrayList<String> autocomplete(String input) {
        ArrayList<String> resultList = null;
        HashMap<String, String> idMap;

        if (mHttpURLConnection != null) {
            mHttpURLConnection.disconnect();
            mHttpURLConnection = null;
        }

        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE
                    + KEY + API_KEY);
            sb.append(INPUT + URLEncoder.encode(input, "utf8"));

            String placeUrl = sb.toString();

            Log.i(TAG, "Place suggestions URL: " + placeUrl);

            URL url = new URL(placeUrl);

            mHttpURLConnection = (HttpURLConnection) url.openConnection();
            mHttpURLConnection.setConnectTimeout(30000);
            mHttpURLConnection.setReadTimeout(30000);
            InputStreamReader in = new InputStreamReader(mHttpURLConnection.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }

            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.optJSONArray("predictions");

            if(predsJsonArray != null && predsJsonArray.length() > 0) {
                int count = predsJsonArray.length();
                // Extract the Place descriptions from the results
                resultList = new ArrayList<>(predsJsonArray.length());
                idMap = new HashMap<>();
                for (int i = 0; i < count; i++) {
                    resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
                    idMap.put(predsJsonArray.getJSONObject(i).getString(
                            "description"), predsJsonArray.getJSONObject(i).getString(
                            "place_id"));
                }

                mPlacesMap.clear();
                mPlacesMap = (HashMap<String, String>) idMap.clone();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Cannot process JSON results", e);

            if (mAutoCompleteAdapterCallback != null) {
                mAutoCompleteAdapterCallback.showToast(R.string.error_occurred);
            }
        } catch (MalformedURLException e) {
            Log.e(TAG, "Error processing Places API URL", e);

            if (mAutoCompleteAdapterCallback != null) {
                mAutoCompleteAdapterCallback.showToast(R.string.error_occurred);
            }

            return resultList;
        } catch (IOException e) {
            Log.e(TAG, "Error connecting to Places API", e);

            if (mAutoCompleteAdapterCallback != null) {
                mAutoCompleteAdapterCallback.showToast(R.string.service_unavailable);
            }

            return resultList;

        } finally {
            abortPendingOperation();
        }


        return resultList;
    }

    /**
     * Helper view holder class
     */
    private static class ViewHolder {
        TextView txtArea;
        TextView txtDetails;
        View divider;
    }

    /**
     * Abort any pending network communication.
     */
    public void abortPendingOperation() {
        if (mHttpURLConnection != null) {
            mHttpURLConnection.disconnect();
        }
    }

    /**
     *
     * Get place id for place input.
     *
     * @param location place address
     * @return location place id
     */
    public String getPlaceID(final String location) {
        String placeID;

        placeID = mPlacesMap.get(location);

        if (placeID == null) {
            placeID = mSearchedPlacesMap.get(location);
        }

        return placeID;
    }

    /**
     *Callback for AutoComplete Adapter
     */
    public interface IAutoCompleteAdapterCallback {

        void showToast(int message);

        void suggestionListUpdated(boolean isUpdated);

        void setProgressIndicatorVisibility(int visibility);

    }

}


