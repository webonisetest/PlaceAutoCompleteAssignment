package jayesh.shah.placesautocomplete.helper;

import android.os.Handler;
import android.os.Message;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Created by Jayesh on 28/05/17.
 * Thread to read data from file
 */

public class SearchedPlacesReaderThread implements Runnable {

    private Handler mHandler;
    private File mSearchedPlacesFile;

    public SearchedPlacesReaderThread(Handler handler, File file) {
        mHandler = handler;
        mSearchedPlacesFile = file;
    }

    @Override
    public void run() {

        String searchedPlacesFileData = null;
        HashMap<String, String> searchedPlacesPlaceMap = null;

        try {

            FileInputStream fis = new FileInputStream(mSearchedPlacesFile); // display
            DataInputStream in = new DataInputStream(fis);

            StringBuilder builder = new StringBuilder();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                builder.append(strLine);
            }
            searchedPlacesFileData = builder.toString();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Extract data from SearchedPlacesFileData.
        if (searchedPlacesFileData != null && searchedPlacesFileData.length() > 0) {

            searchedPlacesPlaceMap = new HashMap<>();

            if (searchedPlacesFileData.contains("!")) {

                String[] placesAndIDArray = searchedPlacesFileData.split("!");

                for (String currentPlace : placesAndIDArray) {

                    String place = currentPlace.substring(0, currentPlace.indexOf("@"));
                    String placeID = currentPlace.substring(currentPlace.indexOf("@") + 1);

                    searchedPlacesPlaceMap.put(place, placeID);
                }

            } else {

                String[] placesAndIDArray = searchedPlacesFileData.split("@");
                searchedPlacesPlaceMap.put(placesAndIDArray[0], placesAndIDArray[1]);
            }
        }

        // Send searched places data back to main.
        Message msg = mHandler.obtainMessage();
        msg.obj = searchedPlacesPlaceMap;
        msg.arg1 = 1001;
        mHandler.sendMessage(msg);
    }
}
