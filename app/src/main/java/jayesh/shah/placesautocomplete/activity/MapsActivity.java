package jayesh.shah.placesautocomplete.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import jayesh.shah.placesautocomplete.R;
import jayesh.shah.placesautocomplete.adapter.PhotosAdapter;
import jayesh.shah.placesautocomplete.helper.PlacesHelper;
import jayesh.shah.placesautocomplete.network.DownloadPlacePhoto;
import jayesh.shah.placesautocomplete.network.PlacesDetailsTask;

/**
 * Map activity that shows map and photos of place selected by user
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private static final String TAG = MapsActivity.class.getSimpleName();

    // Permission request code constants for Location and External storage write
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int EXTERNAL_STORAGE_WRITE_PERMISSION_REQUEST_CODE = 2;

    // Member variables.
    private GoogleMap mMap;
    private PlacesHelper mPlacesHelper;
    private LocationManager mLocationManager;
    private boolean mRequestedForEnableLocation;
    private ProgressBar mProgressBar;
    private PhotosAdapter mPhotosAdapter;
    private Bitmap mPlaceBitmap;
    private String mPlacePhotoReference;
    private Marker mCurrentMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mPlacesHelper = getIntent().getParcelableExtra("PLACE_DETAILS");
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mProgressBar = (ProgressBar) findViewById(R.id.mapsProgressBar);

        ImageButton myLocationButton = (ImageButton) findViewById(R.id.myLocationBtn);
        myLocationButton.setOnClickListener(this);

        RecyclerView photosRecyclerView = (RecyclerView) findViewById(R.id.photos_recycler_view);
        photosRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        photosRecyclerView.setLayoutManager(layoutManager);

        mPhotosAdapter = new PhotosAdapter(this);
        mPhotosAdapter.setPhotosAdapterCallback(photosAdapterCallback);
        mPhotosAdapter.setPhotosUrlList(mPlacesHelper.getPhotoReferencesList());
        photosRecyclerView.setAdapter(mPhotosAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // mRequestedForEnableLocation to ensure user is came back to screen and user has requested for location usage
        // isLocationEnabled to check if location service is turned on
        if (mRequestedForEnableLocation && isLocationEnabled()) {
            mRequestedForEnableLocation = false;
            requestLocationUpdates();
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setMapToolbarEnabled(false);

        // Add a marker in Sydney and move the camera
        LatLng location = new LatLng(mPlacesHelper.getLatitude(), mPlacesHelper.getLongitude());

        mMap.clear();

        if(mCurrentMarker != null) {
            mCurrentMarker.remove();
            mCurrentMarker = null;
        }
        mMap.addMarker(new MarkerOptions().position(location).title(mPlacesHelper.getPlace()).
                icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15.0f));

    }

    /**
     * @return criteria for getting user location
     */
    private Criteria getLocationCriteria() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(true);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setVerticalAccuracy(Criteria.ACCURACY_LOW);
        return criteria;
    }

    /**
     * Register to get location updates
     *
     */
    private void requestLocationUpdates() {

        Log.d(TAG, "Requesting location Updates");
        Log.d(TAG,
                "Best location provider: "
                        + mLocationManager.getBestProvider(
                        getLocationCriteria(), true));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            if (mLocationManager.getAllProviders().contains(
                    LocationManager.NETWORK_PROVIDER)) {

                mLocationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener);
            }

            Log.d(TAG, "Subscribed for gps location updates");
            if (mLocationManager.getAllProviders().contains(
                    LocationManager.GPS_PROVIDER)) {
                setProgressVisible(ProgressBar.VISIBLE);
                mLocationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 0L, 0f, locationListener);
            }

        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(MapsActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }

    }

    /**
     *
     *  Location update listener.
     */
    LocationListener locationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onLocationChanged(Location location) {

            mLocationManager.removeUpdates(locationListener);

            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.clear();

            if(mCurrentMarker != null) {
                mCurrentMarker.remove();
                mCurrentMarker = null;
            }
            mCurrentMarker =  mMap.addMarker(new MarkerOptions().position(currentLocation).
                    icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker)));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15.0f));

            getDetailsForCurrentLocation(currentLocation);
        }
    };

    /**
     *
     * @param currentLatLng Get place details for user's current location latitude and longitude
     */
    private void getDetailsForCurrentLocation(LatLng currentLatLng) {

        PlacesDetailsTask placesDetailsTask = new PlacesDetailsTask(PlacesDetailsTask.PLACE_DETAILS_TYPE_LAT_LNG);
        placesDetailsTask.setPlacesDetailsCallback(placesDetailsCallback);
        placesDetailsTask.setLatLngForDetails(currentLatLng.latitude, currentLatLng.longitude);
        placesDetailsTask.execute();
        setProgressVisible(ProgressBar.VISIBLE);

    }

    /**
     *
     * Set visibility of Progress indicator.
     *
     * @param visible value for progress indicator.
     */
    private void setProgressVisible(int visible) {

        mProgressBar.setVisibility(visible);

        if(visible == ProgressBar.VISIBLE) {
            mPhotosAdapter.setProgressIndicatorIsVisible(true);
        } else {
            mPhotosAdapter.setProgressIndicatorIsVisible(false);
        }
    }

    /**
     *
     * Click listener for view
     *
     * @param v view clicked
     */
    @Override
    public void onClick(View v) {

        int id = v.getId();

        if (id == R.id.myLocationBtn) {

            if (isLocationEnabled()) {

                requestLocationUpdates();

            } else {
                mRequestedForEnableLocation = true;
                Intent viewIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(viewIntent);

            }

        }
    }

    /**
     *
     * Check if location service is enabled.
     *
     * @return location enabled value
     */
    private boolean isLocationEnabled() {

        return mLocationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     *
     *
     * @param requestCode Permission request code.
     * @param permissions requested permissions array.
     * @param grantResults number of granted permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        int permissionsCounter = 0;
        int count = grantResults.length;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {

                    for (int value : grantResults) {
                        if (value == 0) {
                            permissionsCounter++;
                        }
                    }

                    if(permissionsCounter == count) {
                        requestLocationUpdates();
                    }

                }

                break;
            }

            case EXTERNAL_STORAGE_WRITE_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {

                    for (int value : grantResults) {
                        if (value == 0) {
                            permissionsCounter++;
                        }
                    }

                    if (permissionsCounter == count) {
                        downloadAndSavePlacePhotoFromServer(mPlacePhotoReference);
                    }

                }

                break;
            }

            default: {
                Log.i(TAG, "Different permission request code received");
            }
        }
    }

    /**
     * @param messageID Id of message to be shown
     */
    private void showToast(int messageID) {

        Toast.makeText(MapsActivity.this, getString(messageID), Toast.LENGTH_SHORT).show();
    }

    /**
     * Callback for places details to have communication.
     */
    PlacesDetailsTask.IPlacesDetails placesDetailsCallback = new PlacesDetailsTask.IPlacesDetails() {
        @Override
        public void showToast(int messageID) {

            setProgressVisible(ProgressBar.INVISIBLE);
            MapsActivity.this.showToast(messageID);
        }

        @Override
        public void placesDetails(PlacesHelper placesHelper) {
            setProgressVisible(ProgressBar.INVISIBLE);
            if (placesHelper == null) {

                MapsActivity.this.showToast(R.string.error_occurred);
            } else {
                if(mCurrentMarker != null && placesHelper.getPlace() != null) {
                    mCurrentMarker.setTitle(placesHelper.getPlace());
                }
                mPhotosAdapter.setPhotosUrlList(placesHelper.getPhotoReferencesList());

            }
        }
    };

    /**
     * Callback for photos adapter to have communication.
     */
    PhotosAdapter.IPhotosAdapter photosAdapterCallback = new PhotosAdapter.IPhotosAdapter() {
        @Override
        public void downloadPlacePhoto(String photoReference) {

            MapsActivity.this.downloadPhoto(photoReference);
        }

        @Override
        public void showToast(int messageID) {

            MapsActivity.this.showToast(messageID);
        }
    };

    /**
     *  Download photo if SD Card is available.
     *
     * @param photoReference photo reference string
     */
    private void downloadPhoto(String photoReference) {

        //Below can be used to checked for external storage availability, downside it will always be true for devices having only internal storage supported
        boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);

        //Below will work only if SD Card is present, it will return false for devices that doesn't support SD Card.
        //boolean isSDPresent = ContextCompat.getExternalFilesDirs(this, null).length >= 2;

        if (isSDPresent) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED) {

                downloadAndSavePlacePhotoFromServer(photoReference);

                //savePlacePhoto(placePhoto);
            } else {
                mPlacePhotoReference = photoReference;
                //placeBitmap = placePhoto;
                ActivityCompat.requestPermissions(MapsActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        2);
            }

        } else {
            MapsActivity.this.showToast(R.string.no_sd_card_can_not_download);
        }
    }


    /**
     *
     * Save photo to external storage
     *
     * @param placePhoto photo of a place
     */
    private void savePlacePhoto(Bitmap placePhoto) {
        try {
            OutputStream fOut;

            File root = new File(Environment.getExternalStorageDirectory()
                    + File.separator + getString(R.string.app_name) + File.separator);
            root.mkdirs();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date now = new Date();
            String fileName = sdf.format(now);

            File sdImageMainDirectory = new File(root, fileName + ".png");
            Uri.fromFile(sdImageMainDirectory);

            fOut = new FileOutputStream(sdImageMainDirectory);
            placePhoto.compress(Bitmap.CompressFormat.PNG, 100, fOut);

            fOut.flush();
            fOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (mPlaceBitmap != null) {
                mPlaceBitmap.recycle();
                mPlaceBitmap = null;
            }
            setProgressVisible(ProgressBar.INVISIBLE);
        }
    }

    /**
     *
     * Download photo from server
     * @param mPlacePhotoReference photo reference string
     */
    private void downloadAndSavePlacePhotoFromServer(String mPlacePhotoReference) {

        DownloadPlacePhoto downloadPlacePhoto = new DownloadPlacePhoto();
        downloadPlacePhoto.setDownloadPlacePhotoCallback(downloadPlacePhotoCallback);
        downloadPlacePhoto.execute(mPlacePhotoReference);
        setProgressVisible(ProgressBar.VISIBLE);
    }

    /**
     *
     *  Callback for download photo task to have communication.
     */
    DownloadPlacePhoto.IDownloadPlacePhoto downloadPlacePhotoCallback = new DownloadPlacePhoto.IDownloadPlacePhoto() {
        @Override
        public void showToast(int message) {
            MapsActivity.this.setProgressVisible(ProgressBar.INVISIBLE);
            MapsActivity.this.showToast(message);
        }

        @Override
        public void savePlacePhoto(Bitmap placePhoto) {
            MapsActivity.this.savePlacePhoto(placePhoto);
        }
    };
}
