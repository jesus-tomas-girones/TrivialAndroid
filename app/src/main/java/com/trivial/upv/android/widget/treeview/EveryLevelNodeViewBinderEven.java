package com.trivial.upv.android.widget.treeview;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.trivial.upv.android.R;
import com.trivial.upv.android.fragment.CategorySelectionTreeViewFragment;
import com.trivial.upv.android.model.json.CategoryJSON;

import me.texy.treeview.TreeNode;
import me.texy.treeview.base.CheckableNodeViewBinder;

/**
 * Created by jvg63 on 17/08/2017.
 */

class EveryLevelNodeViewBinderEven extends CheckableNodeViewBinder {
    private final ImageView imageView;
    private final TextView textView;
    private final Context mContext;
    private final CategorySelectionTreeViewFragment.OnCheckBoxClickListener mOnCheckBoxListener;

    public EveryLevelNodeViewBinderEven(View itemView, CategorySelectionTreeViewFragment.OnCheckBoxClickListener onCheckBoxListener) {
        super(itemView);
        textView = (TextView) itemView.findViewById(R.id.node_name_view);
        imageView = (ImageView) itemView.findViewById(R.id.arrow_img);
        mContext = itemView.getContext();
        mOnCheckBoxListener = onCheckBoxListener;
    }

    @Override
    public void onNodeSelectedChanged(TreeNode treeNode, boolean selected) {
        super.onNodeSelectedChanged(treeNode, selected);
        mOnCheckBoxListener.onClick();
    }

    @Override
    public int getCheckableViewId() {
        return R.id.checkBox;
    }

    @Override
    public int getLayoutId() {
        return R.layout.treeview_item_even_level;
    }

    @Override
    public void bindView(TreeNode treeNode) {
        final CategoryJSON categoryJSON = (CategoryJSON) treeNode.getValue();
        textView.setText(categoryJSON.getCategory());

        if (treeNode.hasChild()) {
            imageView.setVisibility(View.VISIBLE );
        } else {
            imageView.setVisibility(View.INVISIBLE );
        }
    }
}
