/*
 * This file is part of the UtilsLib Project: https://github.com/spazedog/utils-lib
 *
 * Copyright (c) 2015 Daniel Bergløv
 *
 * UtilsLib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * UtilsLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with UtilsLib. If not, see <http://www.gnu.org/licenses/>
 */

package com.spazedog.lib.utilsLib.app.widget;


import android.content.Context;
import android.content.res.TypedArray;
import android.database.Observable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import com.spazedog.lib.utilsLib.R;

/**
 * Android's ExpandableListView is a great anough tools, for it might be a bit overkill at some times due to the size of it.
 * It also causes problems if you need to place it within a scroll view amongst other views.
 * This Expandable View is very small and very basic. It does not include any fancy scroll features, or scrolling at all for that matter or
 * other large optimizations for large amount of content.
 *
 * It is a great small view for small amount of content, that means 2-20 groups with about 2-20 small children each.
 * If you need more or larger content than that, you should consider using other views.
 *
 * Including content is done using the attached adapter, which is very similar to the one in
 * ExpandableListView.
 */
public class ExpandableView extends LinearLayout {

    private Handler mHandler = new Handler();
    private List<View> mGroupViews = new ArrayList<View>();
    private List<View> mChildViews = new ArrayList<View>();
    private List<Integer> mViewStateCache = new ArrayList<Integer>();

    private int mExpandedGroup = -1;
    private ExpandableAdapter mAdapter;
    private AdapterDataSetObserver mDataSetObserver;

    private Drawable mDrawable;

    public ExpandableView(Context context) {
        super(context);

        setOrientation(LinearLayout.VERTICAL);
    }

    public ExpandableView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(LinearLayout.VERTICAL);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mViewStateCache.size() > 0) {
            int children = getChildCount();
            View view;

            for (int i = 0; i < children; i++) {
                if (mViewStateCache.get(i) == 0) {
                    mGroupViews.add(getChildAt(i));

                } else {
                    mChildViews.add(getChildAt(i));
                }
            }

            mViewStateCache.clear();
        }

        if (mAdapter != null) {
            mAdapter.registerDataSetObserver(mDataSetObserver);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        int children = getChildCount();
        View view;

        /*
         * We don't keep view references in a List if it is about to be destroyed,
         * and WeakReference might course problems, so for now we simply
         * remove them from the lists and make a note what type each view is.
         * We can use this note to recreate the views if needed.
         */
        for (int i = 0; i < children; i++) {
            view = getChildAt(i);
            mViewStateCache.add( mGroupViews.contains(view) ? 0 : 1 );
        }

        mGroupViews.clear();
        mChildViews.clear();

        /*
         * Do not keep a reference of this view in the adapter,
         * not while the adapter is also references within this view
         */
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }
    }

    public void setAdapter(ExpandableAdapter adapter) {
        if (mDataSetObserver != null) {
            if (mAdapter != null) {
                mAdapter.unregisterDataSetObserver(mDataSetObserver);
            }

        } else {
            mDataSetObserver = new IntAdapterDataSetObserver();
        }

        mAdapter = adapter;
        mAdapter.registerDataSetObserver(mDataSetObserver);
        mAdapter.notifyDataSetChanged();
    }


    /*
     * =================================================
     * INTERNAL LISTENERS
     */

    private OnClickListener mClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mGroupViews.contains(v)) {
                int pos = mGroupViews.indexOf(v);

                if (!mAdapter.onGroupClick(pos, v)) {
                    mExpandedGroup = mExpandedGroup == pos ? -1 : pos;
                    mAdapter.notifyDataSetChanged();
                }

            } else {
                mAdapter.onChildClick(mExpandedGroup, mChildViews.indexOf(v), v);
            }
        }
    };


    /*
     * =================================================
     * INTERNAL OBSERVER
     */

    private class IntAdapterDataSetObserver implements AdapterDataSetObserver {

        @Override
        public void onDataSetChanged() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    int itemSize = mAdapter.getGroupCount();
                    int viewSize = mGroupViews.size();
                    View view = null;

                    for (int i=0; itemSize > 0 || i < viewSize; itemSize--, i++) {
                        if (itemSize > 0 && i < viewSize) {
                            View curView = mGroupViews.get(i);
                            view = mAdapter.getGroupView(i, i == mExpandedGroup, curView, ExpandableView.this);

                            if (view != curView) {
                                int pos = indexOfChild(curView);
                                mGroupViews.set(i, view);

                                curView.setOnClickListener(null);
                                removeView(curView);
                                mAdapter.onRecycleGroupView(curView);
                                addView(view, pos);
                            }

                        } else if (itemSize > 0) {
                            view = mAdapter.getGroupView(i, i == mExpandedGroup, null, ExpandableView.this);
                            mGroupViews.add(view);

                            addView(view);

                        } else {
                            view = mGroupViews.remove(mGroupViews.size()-1);
                            view.setOnClickListener(null);

                            removeView(view);
                            mAdapter.onRecycleGroupView(view);

                            continue;
                        }

                        if (mAdapter.isGroupClickable(i, view)) {
                            view.setOnClickListener(mClickListener);
                            view.setClickable(true);
                            view.setFocusable(true);

                        } else {
                            view.setOnClickListener(null);
                            view.setClickable(false);
                            view.setFocusable(false);
                        }
                    }


                    itemSize = mExpandedGroup >= 0 ? mAdapter.getChildCount(mExpandedGroup) : 0;
                    viewSize = mChildViews.size();
                    View groupView = itemSize > 0 ? mGroupViews.get(mExpandedGroup) : null;

                    for (int i=0, x=1; itemSize > 0 || i < viewSize; itemSize--, i++, x++) {
                        if (itemSize > 0 && i < viewSize) {
                            View curView = mChildViews.get(i);
                            view = mAdapter.getChildView(mExpandedGroup, i, curView, ExpandableView.this);

                            removeView(curView);

                            if (view != curView) {
                                curView.setOnClickListener(null);
                                mAdapter.onRecycleChildView(curView);
                            }

                            addView(view, indexOfChild(groupView) + x);

                        } else if (itemSize > 0) {
                            view = mAdapter.getChildView(mExpandedGroup, i, null, ExpandableView.this);
                            mChildViews.add(view);

                            addView(view, indexOfChild(groupView) + x);

                        } else {
                            view = mChildViews.remove(mChildViews.size()-1);
                            view.setOnClickListener(null);

                            removeView(view);
                            mAdapter.onRecycleChildView(view);
                        }

                        if (mAdapter.isChildClickable(mExpandedGroup, i, view)) {
                            view.setOnClickListener(mClickListener);
                            view.setClickable(true);
                            view.setFocusable(true);

                        } else {
                            view.setOnClickListener(null);
                            view.setClickable(false);
                            view.setFocusable(false);
                        }
                    }

                    requestLayout();
                }
            });
        }
    }


    /*
     * =================================================
     * CUSTOM OBSERVABLE
     */

    public interface AdapterDataSetObserver {
        void onDataSetChanged();
    }

    public static class AdapterDataSetObservable extends Observable<AdapterDataSetObserver> {

        public void notifyDataSetChanged() {
            synchronized(mObservers) {
                for (int i = mObservers.size() - 1; i >= 0; i--) {
                    mObservers.get(i).onDataSetChanged();
                }
            }
        }
    }


    /*
     * =================================================
     * ABSTRACT ADAPTER THAT CAN BE USED ON THE VIEW
     */

    public static abstract class ExpandableAdapter {

        private final AdapterDataSetObservable mDataSetObservable = new AdapterDataSetObservable();

        public void registerDataSetObserver(AdapterDataSetObserver observer) {
            try {
                mDataSetObservable.registerObserver(observer);

            } catch (IllegalStateException e) {
            }
        }

        public void unregisterDataSetObserver(AdapterDataSetObserver observer) {
            try {
                mDataSetObservable.unregisterObserver(observer);

            } catch (IllegalStateException e) {
            }
        }

        public void notifyDataSetChanged() {
            mDataSetObservable.notifyDataSetChanged();
        }

        public int getGroupCount() {
            return 0;
        }

        public int getChildCount(int groupPos) {
            return 0;
        }

        public View getGroupView(int groupPos, boolean isExpanded, View recycledView, ViewGroup parent) {
            return null;
        }

        public View getChildView(int groupPos, int childPos, View recycledView, ViewGroup parent) {
            return null;
        }

        public void onRecycleGroupView(View view) {

        }

        public void onRecycleChildView(View view) {

        }

        public boolean onGroupClick(int groupPos, View view) {
            return false;
        }

        public boolean onChildClick(int groupPos, int childPos, View view) {
            return false;
        }

        public boolean isGroupClickable(int groupPos, View view) {
            return true;
        }

        public boolean isChildClickable(int groupPos, int childPos, View view) {
            return true;
        }
    }
}
