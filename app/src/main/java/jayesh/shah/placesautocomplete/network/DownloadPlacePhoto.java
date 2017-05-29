package jayesh.shah.placesautocomplete.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import jayesh.shah.placesautocomplete.R;

/**
 * Created by Jayesh on 27/05/17.
 * Download place photo from server
 */

public class DownloadPlacePhoto extends AsyncTask<String, Void, Bitmap> {

    private static final String TAG = DownloadPlacePhoto.class.getSimpleName();

    private IDownloadPlacePhoto mDownloadPlacePhotoCallback;

    /**
     *
     * @param callback for DownLoadPhoto
     */
    public void setDownloadPlacePhotoCallback(IDownloadPlacePhoto callback) {
        mDownloadPlacePhotoCallback = callback;
    }

    @Override
    protected Bitmap doInBackground(String... params) {

        // URL Constant to get photo from server.
        final String BASE_URL = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=1600&maxheight=1600&photoreference=";
        final String KEY = "&key=AIzaSyBoyDc40todngnDnkrGYA2Tr_1bKcocmTc";

        final String PHOTO_REFERENCE = params[0];

        Bitmap placePhoto = null;

        try {
            HttpURLConnection httpURLConnection;

            StringBuilder urlBuilder = new StringBuilder(BASE_URL);
            urlBuilder.append(PHOTO_REFERENCE);
            urlBuilder.append(KEY);

            String photoDownloadUrl = urlBuilder.toString();

            Log.i(TAG, "Photo Download URL: " + photoDownloadUrl);

            URL url = new URL(photoDownloadUrl);

            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(30000);
            httpURLConnection.setReadTimeout(30000);

//            if ((httpURLConnection.getResponseCode() < 200)
//                    || (httpURLConnection.getResponseCode() > 299)) {
//
//                return placePhoto;
//            }

            InputStream inStream = httpURLConnection.getInputStream();

            if (inStream != null) {

                placePhoto = BitmapFactory.decodeStream(inStream);
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return placePhoto;
        } catch (IOException e) {
            e.printStackTrace();
            return placePhoto;
        }

        return placePhoto;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);

        if(mDownloadPlacePhotoCallback != null) {
            if(bitmap == null) {
             mDownloadPlacePhotoCallback.showToast(R.string.error_occurred);
            } else {
                mDownloadPlacePhotoCallback.savePlacePhoto(bitmap);
            }
        }
    }

    /**
     * Callback for downPhotoTask
     */
    public interface IDownloadPlacePhoto {

        void showToast(int message);

        void savePlacePhoto(Bitmap placePhoto);
    }
}
