package jayesh.shah.placesautocomplete.activity;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import jayesh.shah.placesautocomplete.R;
import jayesh.shah.placesautocomplete.adapter.AutoCompleteAdapter;
import jayesh.shah.placesautocomplete.helper.PlacesHelper;
import jayesh.shah.placesautocomplete.helper.SearchedPlacesReaderThread;
import jayesh.shah.placesautocomplete.network.PlacesDetailsTask;

public class MainActivity extends AppCompatActivity implements Handler.Callback {

    private static final String TAG = MainActivity.class.getSimpleName();

    // Constant String.
    private final String FIRST_TIME = "firstTime";

    // Member variables.
    private AutoCompleteTextView mPlacesAutoCompleteTextView;
    private AutoCompleteAdapter mAutoCompleteAdapter;
    private ProgressBar mProgressBar;
    private Drawable mDrawableX, mDrawableSearch;
    private boolean mItemSelected, mFirstTime, mNeedToSave;
    private SharedPreferences mPreferences;
    private HashMap<String, String> mSearchedPlacesMap;
    private Handler mHandler;
    private String mPlaceID, mLocationString;
    private ArrayList<String> mSearchedPlacesList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Getting Google Play availability status and proceed only if Play Service is available.
        if (isGooglePlayServicesAvailable(this)) {

            mPlacesAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.placesAutoCompleteTextView);
            mAutoCompleteAdapter = new AutoCompleteAdapter(this, R.layout.autocomplete_list_item);
            mAutoCompleteAdapter.setAutoCompleteAdapterCallback(autoCompleteAdapterCallback);
            mAutoCompleteAdapter.setNotifyOnChange(true);
            mPlacesAutoCompleteTextView.setAdapter(mAutoCompleteAdapter);
            mPlacesAutoCompleteTextView
                    .setOnItemClickListener(mAutoCompleteAdapterItemClickListener);
            mPlacesAutoCompleteTextView.addTextChangedListener(actvTextWatcher);
            mPlacesAutoCompleteTextView.setOnTouchListener(actvTouchListener);

            mProgressBar = (ProgressBar) findViewById(R.id.mainProgressBar);

            mDrawableX = getResources().getDrawable(R.drawable.ic_clear);
            mDrawableX.setBounds(0, 0, mDrawableX.getMinimumWidth(),
                    mDrawableX.getMinimumHeight());

            mDrawableSearch = getResources().getDrawable(android.R.drawable.ic_menu_search);

            mDrawableSearch.setBounds(0, 0, mDrawableSearch.getMinimumWidth(),
                    mDrawableSearch.getMinimumHeight());

            mHandler = new Handler(this);

            mPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
            mFirstTime = mPreferences.getBoolean(FIRST_TIME, true);

            // If application is launched for first time,
            // file will not have any data, in this case no need to read data from file.
            if (!mFirstTime) {
                readSearchedPlacesFile();
            }
        }
    }

    /**
     * Read user searched places from stored file.
     */
    private void readSearchedPlacesFile() {
        // File data could grow as user uses app, thread would be better place to avoid freezing of ui and keep smooth user interaction.
        Thread readFileThread = new Thread(new SearchedPlacesReaderThread(mHandler, getFile()));
        readFileThread.start();
    }


    /**
     * Check availability of Google Play services on device
     *
     * @param activity check against screen
     * @return availability of Google play services
     */
    public boolean isGooglePlayServicesAvailable(Activity activity) {

        boolean isAvailable = true;
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, 1011).show();
            }
            isAvailable = false;
        }
        return isAvailable;
    }


    @Override
    protected void onRestart() {
        super.onRestart();

        if (mPlacesAutoCompleteTextView.getText().toString().length() >= 1 && !mItemSelected &&
                (mAutoCompleteAdapter != null && mAutoCompleteAdapter.getCount() > 0)) {

            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInputFromWindow(mPlacesAutoCompleteTextView.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
            mPlacesAutoCompleteTextView.requestFocus();
            mPlacesAutoCompleteTextView.showDropDown();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // In case due to some reason network operation is on, need to abort it.
        if (mPlacesAutoCompleteTextView.isPerformingCompletion()) {
            mAutoCompleteAdapter.abortPendingOperation();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mNeedToSave) {

            if (mFirstTime) {
                // Need to ensure same place does not get written for more than once,
                // file gets written on every place search, read happens only on app start to keep performance steady.
                writeToFileForFirstTime();
            } else {
                // Write user place query to file.
                writeToFile();
            }
        }
    }

    /**
     * Write data to file when application is used for the very first time
     *
     * Need to ensure same place does not get written for more than once,
     * file gets written on every place search, read happens only on app start to keep performance steady.
     */
    private void writeToFileForFirstTime() {

        /*
         * For very first time, Searched places map will be null, next onwards when user searches same again
         * same place query should not get stored, since file is read only at startup, during application life time
         * SearchedPlacesMap and SearchedPlacesList works as a cache and provide smoother and faster data and
         * continuous file read operation.
         *
         * Write operation, appends only current place data, that is only minimum required data.
         */
        if (mSearchedPlacesMap == null || (!mSearchedPlacesMap.get(mLocationString).equals(mPlaceID))) {
            File searchedPlacesFile = getFile();

            try {
                StringBuilder builder = new StringBuilder();

                // Delimiters (!, $) are used to extract place and placeID data when file is read.
                // ! is used to distinguish between place and placeID values
                // @ is used to distinguish between different places values.
                if (mFirstTime) {

                    builder.append(mLocationString);

                } else {

                    builder.append("!");
                    builder.append(mLocationString);
                }

                builder.append("@");
                builder.append(mPlaceID);

                String newPlace = builder.toString();
                FileOutputStream fos = new FileOutputStream(searchedPlacesFile, true); // save
                fos.write(newPlace.getBytes());
                fos.close();
                Log.e(TAG, "FILE SAVED");
                mNeedToSave = false;

                if (mFirstTime) {
                    mSearchedPlacesMap = new HashMap<>();
                    mSearchedPlacesList = new ArrayList<>();
                }
                mSearchedPlacesMap.put(mLocationString, mPlaceID);
                mSearchedPlacesList.add(mLocationString);
                // mAutoCompleteAdapter.setSearchedPlacesList(mSearchedPlacesList);
                //mAutoCompleteAdapter.setSearchedLocationMap(mSearchedPlacesMap);

                mAutoCompleteAdapter.addSearchedPlaceToAdapter(mLocationString, mPlaceID);
                mFirstTime = false;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * Write user data to file when application is being used for more than once.
     */
    private void writeToFile() {
        if (((mSearchedPlacesMap != null) &&
                (mSearchedPlacesMap.get(mLocationString) == null || !mSearchedPlacesMap.get(mLocationString).equals(mPlaceID)))) {


            File searchedPlacesFile = getFile();

            try {
                StringBuilder builder = new StringBuilder();

                if (mFirstTime) {

                    builder.append(mLocationString);

                } else {

                    builder.append("!");
                    builder.append(mLocationString);
                }

                builder.append("@");
                builder.append(mPlaceID);

                String newPlace = builder.toString();
                FileOutputStream fos = new FileOutputStream(searchedPlacesFile, true); // save
                fos.write(newPlace.getBytes());
                fos.close();
                Log.e(TAG, "FILE SAVED");
                mNeedToSave = false;

                if (mFirstTime) {
                    mSearchedPlacesMap = new HashMap<>();
                    mSearchedPlacesList = new ArrayList<>();
                }
                mSearchedPlacesMap.put(mLocationString, mPlaceID);
                mSearchedPlacesList.add(mLocationString);
                // mAutoCompleteAdapter.setSearchedPlacesList(mSearchedPlacesList);
                //mAutoCompleteAdapter.setSearchedLocationMap(mSearchedPlacesMap);

                mAutoCompleteAdapter.addSearchedPlaceToAdapter(mLocationString, mPlaceID);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @return File object to used to read or write file operations.
     */
    private File getFile() {
        // File name used to store user searched places queries.
        final String SEARCHED_PLACES_FILE_NAME = "searchedPlaces.txt";

        ContextWrapper contextWrapper = new ContextWrapper(
                getApplicationContext());
        File directory = contextWrapper.getDir(getFilesDir().getName(), Context.MODE_PRIVATE);

        return new File(directory, SEARCHED_PLACES_FILE_NAME);
    }

    /**
     * Text watcher for places autoComplete textView (PATV)
     * When PATV has one or more than character or no character update drawables accordingly
     * also show progress indicator to indicate places suggestions is getting filled.
     */
    private TextWatcher actvTextWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            if (!mPlacesAutoCompleteTextView.getText().toString().isEmpty()) {
                setProgressBarVisibility(ProgressBar.VISIBLE);
                mPlacesAutoCompleteTextView.setCompoundDrawables(mDrawableSearch, null,
                        mDrawableX, null);
            } else {
                setProgressBarVisibility(ProgressBar.INVISIBLE);
                mPlacesAutoCompleteTextView.setCompoundDrawables(mDrawableSearch, null,
                        null, null);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    /**
     * Touch listener for Places AutoCompleteTextView
     *
     * When user clicks on "X" need to clear text and set drawables accordingly.
     */
    private View.OnTouchListener actvTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (((AutoCompleteTextView) v).getCompoundDrawables()[2] == null) {
                return false;
            }
            if (event.getAction() != MotionEvent.ACTION_UP) {
                return false;
            }
            if (event.getX() > v.getWidth() - v.getPaddingRight()
                    - mDrawableX.getIntrinsicWidth()) {
                ((AutoCompleteTextView) v).setText("");
                ((AutoCompleteTextView) v).setCompoundDrawables(mDrawableSearch,
                        null, null, null);
            }
            return false;
        }
    };

    /**
     * Callback for AutoComplete adapter to be able to have communication.
     */
    AutoCompleteAdapter.IAutoCompleteAdapterCallback autoCompleteAdapterCallback = new AutoCompleteAdapter.IAutoCompleteAdapterCallback() {
        @Override
        public void showToast(int messageID) {

            MainActivity.this.showToast(messageID);
        }

        @Override
        public void suggestionListUpdated(boolean isUpdated) {
            mItemSelected = false;
        }

        @Override
        public void setProgressIndicatorVisibility(int visibility) {
            setProgressBarVisibility(visibility);
        }
    };

    /**
     * @param visibility visibility of Progress indicator
     */
    private void setProgressBarVisibility(int visibility) {
        mProgressBar.setVisibility(visibility);
    }


    /**
     *
     *  Adapter/Suggestion list click listener and move to Map Activity when details like latitude, longitude and
     *  photoReference data is fetched from server.
     */
    AdapterView.OnItemClickListener mAutoCompleteAdapterItemClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View view, int arg2,
                                long arg3) {
            mItemSelected = true;
            mLocationString = arg0.getItemAtPosition(arg2).toString();

            mPlaceID = mAutoCompleteAdapter.getPlaceID(mLocationString);//, "nothing");

            if (mPlaceID != null) {
                PlacesDetailsTask placesDetailsTask = new PlacesDetailsTask(PlacesDetailsTask.PLACE_DETAILS_TYPE_ADDRESS);
                placesDetailsTask.setPlacesDetailsCallback(placesDetailsCallback);
                placesDetailsTask.execute(mLocationString, mPlaceID);
                setProgressBarVisibility(ProgressBar.VISIBLE);

            }

            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInputFromWindow(mPlacesAutoCompleteTextView.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
        }
    };

    /**
     * Places details callback to have communication.
     */
    PlacesDetailsTask.IPlacesDetails placesDetailsCallback = new PlacesDetailsTask.IPlacesDetails() {
        @Override
        public void showToast(int messageID) {

            setProgressBarVisibility(ProgressBar.INVISIBLE);
            MainActivity.this.showToast(messageID);
        }

        @Override
        public void placesDetails(PlacesHelper placesHelper) {
            setProgressBarVisibility(ProgressBar.INVISIBLE);
            if (placesHelper == null) {

                MainActivity.this.showToast(R.string.error_occurred);
            } else {

                if (mFirstTime) {
                    SharedPreferences.Editor editor = mPreferences.edit();
                    editor.putBoolean(FIRST_TIME, false);
                    editor.apply();
                }

                mNeedToSave = true;
                Intent mapActivityIntent = new Intent(MainActivity.this, MapsActivity.class);
                mapActivityIntent.putExtra("PLACE_DETAILS", placesHelper);
                startActivity(mapActivityIntent);

            }
        }
    };


    /**
     * @param messageID id of message to be shown
     */
    private void showToast(int messageID) {

        Toast.makeText(MainActivity.this, getString(messageID), Toast.LENGTH_SHORT).show();
    }

    /**
     *
     *
     * @param msg message object sent by Handler.
     * @return result.
     */
    @Override
    public boolean handleMessage(Message msg) {

        if (msg.arg1 == 1001) {

            mSearchedPlacesMap = (HashMap<String, String>) msg.obj;

            if (mSearchedPlacesMap != null && mSearchedPlacesMap.size() > 0) {
                mSearchedPlacesList = new ArrayList<>();

                for (String location : mSearchedPlacesMap.keySet()) {
                    mSearchedPlacesList.add(location);
                }

                mAutoCompleteAdapter.setSearchedPlacesList(mSearchedPlacesList);
                mAutoCompleteAdapter.setSearchedLocationMap(mSearchedPlacesMap);
            }
        }

        return false;
    }
}
