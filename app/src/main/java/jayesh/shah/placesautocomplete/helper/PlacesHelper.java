package jayesh.shah.placesautocomplete.helper;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by Jayesh on 26/05/17.
 *
 * Helper class for Places details.
 */

public class PlacesHelper implements Parcelable {
    String place;
    String placeID;
    double latitude;
    double longitude;
    ArrayList<String> photoReferencesList;

    public PlacesHelper() {}

    private PlacesHelper(Parcel in) {
        place = in.readString();
        placeID = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        photoReferencesList = in.createStringArrayList();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(place);
        dest.writeString(placeID);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeStringList(photoReferencesList);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PlacesHelper> CREATOR = new Creator<PlacesHelper>() {
        @Override
        public PlacesHelper createFromParcel(Parcel in) {
            return new PlacesHelper(in);
        }

        @Override
        public PlacesHelper[] newArray(int size) {
            return new PlacesHelper[size];
        }
    };

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public ArrayList<String> getPhotoReferencesList() {
        return photoReferencesList;
    }

    public void setPhotoReferencesList(ArrayList<String> photoReferencesList) {
        this.photoReferencesList = photoReferencesList;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getPlaceID() {
        return placeID;
    }

    public void setPlaceID(String placeID) {
        this.placeID = placeID;
    }
}
