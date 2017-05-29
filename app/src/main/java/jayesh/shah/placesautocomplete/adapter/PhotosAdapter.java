package jayesh.shah.placesautocomplete.adapter;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import jayesh.shah.placesautocomplete.R;

/**
 * Created by Jayesh on 27/05/17.
 *
 * Adapter for recycler view
 */

public class PhotosAdapter extends RecyclerView.Adapter<PhotosAdapter.ViewHolder> {

    private static final String TAG = PhotosAdapter.class.getSimpleName();

    private Context mContext;
    private ArrayList<String> mPhotosUrlList;
    private IPhotosAdapter photosAdapterCallback;
    private boolean mProgressIndicatorIsVisible;

    /**
     *
     * @param context context for adapter
     */
    public PhotosAdapter(Context context) {
        mContext = context;
    }

    /**
     *
     * @param callback callback for adapter
     */
    public void setPhotosAdapterCallback(IPhotosAdapter callback) {
       photosAdapterCallback = callback;
    }

    @Override
    public PhotosAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.photos_item, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(PhotosAdapter.ViewHolder holder, int position) {

        final String GET_PHOTO_BASE_URL = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&maxheight=250&photoreference=";
        final String KEY = "&key=AIzaSyBoyDc40todngnDnkrGYA2Tr_1bKcocmTc";

        StringBuilder builder = new StringBuilder(GET_PHOTO_BASE_URL);
        builder.append(mPhotosUrlList.get(position));
        builder.append(KEY);

        String url = builder.toString();

        Log.i(TAG, "Get Photo URL: " + url);

        Picasso.with(mContext).load(url).into(holder.imgThumbnail);
    }

    @Override
    public int getItemCount() {
        int count = -1;

        if (mPhotosUrlList != null) {
            count = mPhotosUrlList.size();
        }

        return count;
    }

    /**
     *
     * @param photosUrlList Photo url List to be fetched from server
     */
    public void setPhotosUrlList(ArrayList<String> photosUrlList) {
        if(mPhotosUrlList != null) {
            mPhotosUrlList.clear();
            mPhotosUrlList = null;
        }

        mPhotosUrlList = photosUrlList;
        notifyDataSetChanged();
    }

    /**
     *
     * @param isVisible set progress bar visibility
     */
    public void setProgressIndicatorIsVisible(boolean isVisible) {
        mProgressIndicatorIsVisible = isVisible;
    }

    /**
     *
     *
     * @param v show popp menu wit respect to image view
     */
    private void showPopUpMenu(final View v, final int position) {

        if(! mProgressIndicatorIsVisible) {
            PopupMenu popup = new PopupMenu(mContext, v);
            //Inflating the Popup using xml file
            popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

            //registering popup with OnMenuItemClickListener
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {

                    // Below can be used to save thumbnail size photo instead of fetch it from server with larger size.
                    /*ImageView imageView = (ImageView) v;
                    imageView.buildDrawingCache();
                    Bitmap bm = imageView.getDrawingCache();*/

                    if (photosAdapterCallback != null) {
                        photosAdapterCallback.downloadPlacePhoto(mPhotosUrlList.get(position));
                    }
                    return true;
                }
            });

            popup.show();

        } else {
            if (photosAdapterCallback != null) {
                photosAdapterCallback.showToast(R.string.please_wait);
            }
        }
    }


    /**
     * View holder class for adapter view.
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {

        public ImageView imgThumbnail;

        public ViewHolder(View itemView) {
            super(itemView);
            imgThumbnail = (ImageView) itemView.findViewById(R.id.img_thumbnail);
            imgThumbnail.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View v) {
            showPopUpMenu(v, getAdapterPosition());
            return false;
        }
    }

    /**
     * callback for communication.
     */
    public interface IPhotosAdapter {

        void downloadPlacePhoto(String photoReference);
        void showToast(int messageID);
    }
}
