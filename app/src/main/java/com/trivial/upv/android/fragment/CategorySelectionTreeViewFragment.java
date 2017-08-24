
package com.trivial.upv.android.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.trivial.upv.android.R;
import com.trivial.upv.android.activity.QuizActivity;
import com.trivial.upv.android.model.Category;
import com.trivial.upv.android.model.json.CategoryJSON;
import com.trivial.upv.android.model.quiz.Quiz;
import com.trivial.upv.android.persistence.TopekaJSonHelper;
import com.trivial.upv.android.widget.fab.CheckableFab;
import com.trivial.upv.android.widget.treeview.TreeViewFactoryNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import me.texy.treeview.TreeNode;
import me.texy.treeview.TreeView;

public class CategorySelectionTreeViewFragment extends Fragment {
    private static final int NUM_QUIZZES = 10;
    protected Toolbar toolbar;
    private ViewGroup viewGroup;
    private TreeNode root;
    private TreeView treeView;
    private CheckableFab mSubmitAnswer;

    public static CategorySelectionTreeViewFragment newInstance() {
        return new CategorySelectionTreeViewFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Set on menú on this fragment
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_treeview, container, false);
    }

    // Setup Menú
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate( R.menu.treeview_fragment, menu);
    }

    // Setup Actions Menú
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.select_all:
                treeView.selectAll();
                mSubmitAnswer.show();
                break;
            case R.id.deselect_all:
                treeView.deselectAll();
                mSubmitAnswer.hide();
                break;
            case R.id.expand_all:
                treeView.expandAll();
                break;
            case R.id.collapse_all:
                treeView.collapseAll();
                break;
//            case R.id.expand_level:
//                treeView.expandLevel(1);
//                break;
//            case R.id.collapse_level:
//                treeView.collapseLevel(1);
//                break;
//            case R.id.play_game_offline:
//                Toast.makeText(getApplication(), "" + treeView.getSelectedNodes().size(), Toast.LENGTH_LONG).show();
//                playGameOffLine();
//                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setUpQuizGrid(view);
        super.onViewCreated(view, savedInstanceState);
    }

    // JVG.E
    private void setUpQuizGrid(View view) {
        initView(view);

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == REQUEST_CATEGORY && resultCode == R.id.solved) {
//            mAdapter.notifyItemChanged(data.getStringExtra(JsonAttributes.ID));
//        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startQuizActivityWithTransition(Activity activity, View toolbar,
                                                 Category category) {
//
//        final Pair[] pairs = TransitionHelper.createSafeTransitionParticipants(activity, false,
//                new Pair<>(toolbar, activity.getString(R.string.transition_toolbar)));
//        @SuppressWarnings("unchecked")
//        ActivityOptionsCompat sceneTransitionAnimation = ActivityOptionsCompat
//                .makeSceneTransitionAnimation(activity, pairs);
//
//        // Start the activity with the participants, animating from one to the other.
//        final Bundle transitionBundle = sceneTransitionAnimation.toBundle();
//        Intent startIntent = QuizActivity.getStartIntent(activity, category);
//        ActivityCompat.startActivityForResult(activity,
//                startIntent,
//                REQUEST_CATEGORY,
//                transitionBundle);
    }

    private void playGameOffLine() {
        getRandomQuizzesFromSelectedCategories(NUM_QUIZZES);

        animateFloatButton();
    }
    // To hide a previously visible view using this effect:

    public void animateFloatButton() {
        final Intent startIntent = QuizActivity.getStartIntent(getActivity(), TopekaJSonHelper.getInstance(getContext(), false).getCategoryPlayGameOffLine());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            View rootView = getView().findViewById(R.id.coordinator_view);
            final View view = mSubmitAnswer;
            // get the center for the clipping circle

            // get the center for the clipping circle
            int cx = (view.getLeft() + view.getRight()) / 2;
            int cy = (view.getTop() + view.getBottom()) / 2;

            // get the final radius for the clipping circle
            int finalRadius = Math.max(rootView.getWidth(), rootView.getHeight());

            // create the animator for this view (the start radius is zero)
            Animator anim = null;

            anim = ViewAnimationUtils.createCircularReveal(rootView, cx, cy,
                    finalRadius, 0);

            anim.setDuration(750);
            // make the view visible and start the animation
            mSubmitAnswer.hide();

            // start the animation
            anim.addListener(new AnimatorListenerAdapter() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);

                    mSubmitAnswer.show();
                    ActivityCompat.startActivity(getActivity(), startIntent,
                            ActivityOptions.makeSceneTransitionAnimation(getActivity()).toBundle());
                }
            });
            anim.start();
        } else {
            ActivityCompat.startActivity(getActivity(), startIntent,
                    null);
        }
    }

    private List<Quiz> getRandomQuizzesFromSelectedCategories(int numQuizzes) {
        List<TreeNode> selectedNodes = treeView.getSelectedNodes();

        String tmpImg = "";
        String tmpTheme = "";

        ArrayList<Quiz> quizzes = new ArrayList<>();
        for (int i = 0; i < selectedNodes.size(); i++) {

            if (!selectedNodes.get(i).hasChild()) {
                CategoryJSON category = (CategoryJSON) selectedNodes.get(i).getValue();

                if (category.getSubcategories() != null) {
                    for (CategoryJSON subcategory : category.getSubcategories()) {

                        if (subcategory.getQuizzes() != null)
                            quizzes.addAll(subcategory.getQuizzes());
                    }
                }
                if (tmpImg.isEmpty())
                    tmpImg = category.getImg();
                tmpTheme = category.getTheme();
            }
        }


        List<Quiz> tmpQuizzes = getRandomizeQuizzes(quizzes, numQuizzes);

        TopekaJSonHelper.getInstance(getContext(), false).createCategoryPlayGameOffLine(tmpQuizzes, tmpImg, tmpTheme);

        return tmpQuizzes;
    }

    private List<Quiz> getRandomizeQuizzes(ArrayList<Quiz> quizzes, int numQuizzes) {

        List<Quiz> tmpQuizzes = new ArrayList<>();

        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < numQuizzes; i++) {
            list.add(new Integer(i));
        }

        Random r = new Random();
        for (int i = numQuizzes - 1; i >= 0; i--) {
            int t = 0;
            t = r.nextInt(quizzes.size());
            Quiz tmpQuizz = quizzes.remove(t);

            tmpQuizzes.add(tmpQuizz);
        }
        return tmpQuizzes;
    }


    public interface OnCheckBoxClickListener {
        void onClick();
    }

    public void buildTree() {


        root = TreeNode.root();
        buildTreeNodes();

        TreeViewFactoryNode treeViewFactoryNode = new TreeViewFactoryNode();
        treeViewFactoryNode.setOnClickListener(new OnCheckBoxClickListener() {
            @Override
            public void onClick() {
                if (treeView.getSelectedNodes().size() > 0)
                    mSubmitAnswer.show();
                else
                    mSubmitAnswer.hide();
            }
        });
        treeView = new TreeView(root, getActivity(), treeViewFactoryNode);


        View tmpView = treeView.getView();

        tmpView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        viewGroup.addView(tmpView);

    }

    private void buildTreeNodes() {
        if (TopekaJSonHelper.getInstance(getActivity(), false).isLoaded()) {
            List<CategoryJSON> categoriesJSON = TopekaJSonHelper.getInstance(getActivity(), false).getCategoriesJSON();
            if (categoriesJSON != null) {
                int level = 0;
                for (CategoryJSON category : categoriesJSON) {
                    TreeNode treeNode = new TreeNode(category);
                    if (category.getSubcategories() != null)
                        buildTreeSubcategory(treeNode, category.getSubcategories(), level + 1);
                    root.addChild(treeNode);
                }
            }
        }
    }

    private void buildTreeSubcategory(TreeNode parentNode, List<CategoryJSON> categoriesJSON, int level) {
        TreeNode treeNode = null;
        if (categoriesJSON != null) for (CategoryJSON category : categoriesJSON)
            if (category.getSubcategories() != null) {
                treeNode = new TreeNode(category);
                treeNode.setLevel(level);
                buildTreeSubcategory(treeNode, category.getSubcategories(), level + 1);
                parentNode.addChild(treeNode);
            }
    }

    private void initView(View view) {
//        toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        viewGroup = (RelativeLayout) view.findViewById(R.id.container);

        getSubmitButton(view);

        getActivity().supportStartPostponedEnterTransition();


        buildTree();
    }

    private void getSubmitButton(View view) {
        if (null == mSubmitAnswer) {
            mSubmitAnswer = (CheckableFab) view.findViewById(R.id.submitAnswer);
            mSubmitAnswer.hide();
            mSubmitAnswer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playGameOffLine();
//                    mSubmitAnswer.setEnabled(false);
                }
            });
        }

    }


}
