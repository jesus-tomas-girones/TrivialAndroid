package com.trivial.upv.android.widget.treeview;

import android.content.Context;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.trivial.upv.android.R;
import com.trivial.upv.android.fragment.CategorySelectionTreeViewFragment;
import com.trivial.upv.android.helper.singleton.volley.VolleySingleton;
import com.trivial.upv.android.model.json.CategoryJSON;

import me.texy.treeview.TreeNode;
import me.texy.treeview.base.CheckableNodeViewBinder;


/**
 * Created by zxy on 17/4/23.
 */

public class FirstLevelNodeViewBinder extends CheckableNodeViewBinder {
    private final ImageView icon;
    private final TextView textView;
    private final ImageView imageView;
    private final Context mContext;
    private final AppCompatCheckBox mCheckBox;
    private final CategorySelectionTreeViewFragment.OnCheckBoxClickListener mOnCheckBoxListener;

    public FirstLevelNodeViewBinder(View itemView, CategorySelectionTreeViewFragment.OnCheckBoxClickListener onCheckBoxListener) {
        super(itemView);
        textView = (TextView) itemView.findViewById(R.id.node_name_view);
        imageView = (ImageView) itemView.findViewById(R.id.arrow_img);
        icon = (ImageView) itemView.findViewById(R.id.icon);
        mContext = itemView.getContext();
        mCheckBox = (AppCompatCheckBox) itemView.findViewById(R.id.checkBox);
        mOnCheckBoxListener = onCheckBoxListener;
    }


    @Override
    public int getCheckableViewId() {
        return R.id.checkBox;
    }

    @Override
    public int getLayoutId() {
        return R.layout.treeview_item_first_level;
    }

    @Override
    public void bindView(final TreeNode treeNode) {
        final CategoryJSON categoryJSON = (CategoryJSON) treeNode.getValue();

        textView.setText(categoryJSON.getCategory());
        imageView.setRotation(treeNode.isExpanded() ? 90 : 0);

        VolleySingleton.getInstance(mContext).getImageLoader().get(categoryJSON.getImg(), new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                if (response.getBitmap() != null)
                    icon.setImageBitmap(response.getBitmap());
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("VOLLEY", "Error recuperando img: " + categoryJSON.getImg());
            }
        });

        if (treeNode.hasChild()) {
            imageView.setVisibility(View.VISIBLE );
        } else {
            imageView.setVisibility(View.INVISIBLE );
        }
    }

    @Override
    public void onNodeSelectedChanged(TreeNode treeNode, boolean selected) {
        super.onNodeSelectedChanged(treeNode, selected);
        mOnCheckBoxListener.onClick();
    }

    @Override
    public void onNodeToggled(TreeNode treeNode, boolean expand) {
        if (expand) {
            imageView.animate().rotation(90).setDuration(200).start();
        } else {
            imageView.animate().rotation(0).setDuration(200).start();
        }
    }
}
