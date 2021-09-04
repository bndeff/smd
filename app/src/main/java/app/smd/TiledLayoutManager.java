package app.smd;

// based on https://stackoverflow.com/q/26666143

import android.content.Context;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class TiledLayoutManager extends GridLayoutManager
{
    private final int tileWidth;
    private int savedWidth;

    public TiledLayoutManager(Context context, int tileWidth) {
        super(context, 1);
        this.tileWidth = tileWidth;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        int viewWidth = getWidth();
        if(viewWidth != savedWidth) {
            setSpanCount(Math.max((viewWidth - getPaddingStart() - getPaddingEnd()) / tileWidth, 1));
            savedWidth = viewWidth;
        }
        super.onLayoutChildren(recycler, state);
    }

}