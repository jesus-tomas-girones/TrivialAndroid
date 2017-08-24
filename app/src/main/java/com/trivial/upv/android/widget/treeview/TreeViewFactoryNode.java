package com.trivial.upv.android.widget.treeview;

import android.util.Log;
import android.view.View;

import com.trivial.upv.android.fragment.CategorySelectionTreeViewFragment;

import me.texy.treeview.base.BaseNodeViewBinder;
import me.texy.treeview.base.BaseNodeViewFactory;


/**
 * Created by zxy on 17/4/23.
 */

public class TreeViewFactoryNode extends BaseNodeViewFactory {

    private CategorySelectionTreeViewFragment.OnCheckBoxClickListener mOnCheckBoxListener;

    @Override
    public BaseNodeViewBinder getNodeViewBinder(View view, int level) {
        BaseNodeViewBinder newNode;
        Log.d("TRAZA", "" + level);
        switch (level) {
            case 0:
                newNode = new FirstLevelNodeViewBinder(view, mOnCheckBoxListener);
                break;
            case 1:
                newNode = new SecondLevelNodeViewBinder(view, mOnCheckBoxListener);
                break;
            case 2:
                newNode = new ThirdLevelNodeViewBinder(view, mOnCheckBoxListener);
                break;

            default:
                if ((level+1 % 2) == 0)
                    newNode = new EveryLevelNodeViewBinderEven(view, mOnCheckBoxListener);
                else
                    newNode = new EveryLevelNodeViewBinderOdd(view, mOnCheckBoxListener);
        }

        return newNode;
    }

    public void setOnClickListener(CategorySelectionTreeViewFragment.OnCheckBoxClickListener onCheckBoxListener) {
        this.mOnCheckBoxListener = onCheckBoxListener;
    }
}
