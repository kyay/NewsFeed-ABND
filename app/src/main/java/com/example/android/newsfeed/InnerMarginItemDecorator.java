package com.example.android.newsfeed;

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import android.view.View;

import java.util.Objects;

/**
 * An {@link RecyclerView.ItemDecoration} that adds an inner margin.
 */
class InnerMarginItemDecorator extends RecyclerView.ItemDecoration {
    private final int mInnerSpace;

    /**
     * @param innerSpace the inner margin, measured in pixels
     */
    InnerMarginItemDecorator(int innerSpace) {
        mInnerSpace = innerSpace;
    }

    /**
     * Returns if the {@param child} isn't last or ({@param spanCount} - 1)-to-last
     * in the {@param parent} {@link RecyclerView}
     *
     * @param child     The child to check for being last
     * @param parent    The parent that contains the child
     * @param spanCount The span count of the parent (if any)
     * @return whether the child is last in parent or not
     */
    private static boolean isChildNotLastInList(View child, RecyclerView parent, int spanCount) {
        return (parent.getChildAdapterPosition(child) != (Objects.requireNonNull(parent.getAdapter()).getItemCount() - spanCount))
                || (parent.getChildLayoutPosition(child) != (parent.getChildCount() - 1));
    }

    /**
     * Returns if the {@param child} isn't last or (parent's span count - 1)-to-last
     * in the {@param parent} {@link RecyclerView}
     *
     * @param child  The child to check for being last
     * @param parent The parent that contains the child
     * @return whether the child isn't last in parent or not
     */
    private static boolean isChildNotLastInList(View child, RecyclerView parent) {
        return isChildNotLastInList(child, parent, getSpanCount(parent));
    }

    /**
     * Returns if the {@param child} isn't to the end of the in the {@param parent} {@link RecyclerView}.
     * More technically, it returns whether the child's span index isn't the parent's spanCount - 1
     *
     * @param child  The child to check for being on the end
     * @param parent The parent that contains the child
     * @return whether the child isn't right in parent or not
     */
    private static boolean isChildNotOnEnd(View child, RecyclerView parent) {
        int spanCount = getSpanCount(parent);
        RecyclerView.LayoutParams childLayoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
        if (childLayoutParams instanceof StaggeredGridLayoutManager.LayoutParams)
            return ((StaggeredGridLayoutManager.LayoutParams) childLayoutParams).getSpanIndex() != (spanCount - 1);
        // Will return 0 != 0 if span count is 1.
        return (parent.getChildAdapterPosition(child) % spanCount) != (spanCount - 1);
    }

    /**
     * Return's the {@param recyclerView}'s span count, or 1 if the recyclerView isn't a Grid Recycler View
     *
     * @param recyclerView the recyclerView to get the span count for
     * @return The span count of the recyclerView
     */
    private static int getSpanCount(RecyclerView recyclerView) {
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        return (manager instanceof StaggeredGridLayoutManager) ? ((StaggeredGridLayoutManager) manager).getSpanCount() :
                ((manager instanceof GridLayoutManager) ? ((GridLayoutManager) manager).getSpanCount() :
                        1);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect,
                               @NonNull View view,
                               @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        // If the child isn't the last one
        if (isChildNotLastInList(view, parent))
            // Add a bottom padding
            outRect.bottom = mInnerSpace;
        // If the child is not adjacent to the right side of the screen and the child isn't the last one
        if (isChildNotOnEnd(view, parent) && isChildNotLastInList(view, parent, 1))
            // Add a right padding
            outRect.right = mInnerSpace;
    }
}
