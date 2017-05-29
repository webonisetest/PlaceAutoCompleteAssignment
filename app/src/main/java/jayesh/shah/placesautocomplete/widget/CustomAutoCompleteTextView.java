package jayesh.shah.placesautocomplete.widget;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;

/**
 * Created by Jayesh on 26/05/17.
 *
 *  Custom AutoComplete TextView
 */

public class CustomAutoCompleteTextView extends AppCompatAutoCompleteTextView {

    private static final String TAG = CustomAutoCompleteTextView.class.getSimpleName();

    public CustomAutoCompleteTextView(Context context) {
        super(context);
    }

    public CustomAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && isPopupShowing()) {
            InputMethodManager inputManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

            if(inputManager.hideSoftInputFromWindow(findFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS)){
                return true;
            }

        }

        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean enoughToFilter() {
        boolean filter = false;
        if (getText().toString().length() >= 1)
            filter = true;

        return filter;
    }


    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);

        if(focused && getText().toString().length() > 0) {
            if(getAdapter().getCount() > 0)
                showDropDown();
        }
    }
}
