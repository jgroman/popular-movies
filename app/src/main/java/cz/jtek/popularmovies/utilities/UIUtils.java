package cz.jtek.popularmovies.utilities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListAdapter;
import android.widget.ListView;

public class UIUtils {

    /**
     * This method returns device display width
     * Uses deprecated API for SDK_INT < 13 only
     *
     * @param context Context
     * @return Display width
     */
    @SuppressLint("ObsoleteSdkInt")
    @SuppressWarnings("deprecation")
    public static int getDisplayWidth(Context context) {

        int width = 0;
        Display display;

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        if (wm != null) {
            display = wm.getDefaultDisplay();

            if (android.os.Build.VERSION.SDK_INT >= 13) {
                Point size = new Point();
                display.getSize(size);
                width = size.x;
            } else {
                width = display.getWidth();  // deprecated API
            }
        }

        return width;
    }

    /**
     * Calculates correct full ListView height and sets layout height to this value
     * Useful for ListViews inside ScrollView
     *
     * @param listView      ListView to process
     * @param viewMaxWidth  Max requested width of this ListView
     */
    public static void showListViewFullHeight(ListView listView, int viewMaxWidth) {
        ListAdapter adapter = listView.getAdapter();

        if (adapter == null) {
            return;
        }
        int totalHeight = 0;

        for (int i = 0; i < adapter.getCount(); i++) {
            View listItem = adapter.getView(i, null, listView);
            listItem.measure(View.MeasureSpec.makeMeasureSpec(viewMaxWidth, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams par = listView.getLayoutParams();
        par.height = totalHeight + (listView.getDividerHeight() * (adapter.getCount() - 1));
        listView.setLayoutParams(par);
        listView.requestLayout();
    }

}
