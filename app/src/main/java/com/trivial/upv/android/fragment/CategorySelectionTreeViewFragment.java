
package com.trivial.upv.android.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.trivial.upv.android.R;
import com.trivial.upv.android.activity.CategorySelectionActivity;
import com.trivial.upv.android.activity.QuizActivity;
import com.trivial.upv.android.helper.singleton.SharedPreferencesStorage;
import com.trivial.upv.android.model.gpg.Game;
import com.trivial.upv.android.model.json.CategoryJSON;
import com.trivial.upv.android.model.quiz.Quiz;
import com.trivial.upv.android.persistence.TrivialJSonHelper;
import com.trivial.upv.android.widget.fab.CheckableFab;
import com.trivial.upv.android.widget.treeview.TreeViewFactoryNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import me.texy.treeview.TreeNode;
import me.texy.treeview.TreeView;

import static com.trivial.upv.android.activity.QuizActivity.ARG_ONE_PLAYER;
import static com.trivial.upv.android.activity.QuizActivity.ARG_REAL_TIME_ONLINE;

public class CategorySelectionTreeViewFragment extends Fragment {
    private static final String MODE = "mode";
    private static final String TAG = "TreeViewFragment";
    private static final int MAX_LEVEL = 1;
    protected Toolbar toolbar;
    private ViewGroup viewGroup;
    private TreeNode root;
    private TreeView treeView;
    private CheckableFab mSubmitAnswer;
    private String mode = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = getArguments().getString(MODE);
        Game.mode = mode;
    }

    public static CategorySelectionTreeViewFragment newInstance(String mode) {
        CategorySelectionTreeViewFragment fragment = new CategorySelectionTreeViewFragment();
        Bundle args = new Bundle();
        args.putString(MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Set on menú on this fragment
        setHasOptionsMenu(true);

        changeTitleActionBar(mode);

        return inflater.inflate(R.layout.fragment_treeview, container, false);
    }

    // Setup Menú
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.treeview_fragment, menu);

//        inviteItem = menu.findItem(R.id.invite);
//        inviteItem.setVisible(false);

    }

//    private MenuItem inviteItem = null;

    // Setup Actions Menú
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.select_all:
                treeView.selectAll();
                mSubmitAnswer.show();
//                if (inviteItem != null)
//                    inviteItem.setVisible(true);
                break;
            case R.id.deselect_all:
                treeView.deselectAll();
                mSubmitAnswer.hide();
//                if (inviteItem != null)
//                    inviteItem.setVisible(false);
                break;
            case R.id.expand_all:
                treeView.expandAll();
                break;
            case R.id.collapse_all:
                treeView.collapseAll();
                break;
//            case R.id.invite:
//                inviteClick();
//                break;
//            case R.id.expand_level:
//                treeView.expandLevel(1);
//                break;
//            case R.id.collapse_level:
//                treeView.collapseLevel(1);
//                break;
//            case R.id.play_game_offline:
//                Toast.makeText(getApplication(), "" + treeView.getSelectedNodes().size(), Toast.LENGTH_LONG).show();
//                playGame();
//                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setupQuizGrid(view);
        super.onViewCreated(view, savedInstanceState);
    }

    // JVG.E
    private void setupQuizGrid(View view) {
        initView(view);

    }


    private void playGame() {
        if (mode.equals(ARG_ONE_PLAYER)) {
            playGameOnePlayer();
        } else if (mode.equals(ARG_REAL_TIME_ONLINE)) {
            Game.resetGameVars();
            Game.minAutoMatchPlayers = Game.tmpNumPlayers - 1;
            Game.maxAutoMatchPlayers = Game.tmpNumPlayers - 1;
            playGameOnline();
        }

    }

    private void playGameOnline() {
        if (getRandomQuizzesOnlineFromSelectedCategories(Game.tmpNumQuizzes)) {

//            Game.numQuizzes = Game.tmpNumQuizzes;
//            Game.numPlayers = Game.tmpNumPlayers;
//            Game.totalTime = Game.tmpTotalTime;
//
//            Game.category = TrivialJSonHelper.getInstance(getContext(), false).getCategoryPlayGameOffLine();
            animateFloatButton();
        }
    }

    private void playGameOnePlayer() {
        SharedPreferencesStorage sps = SharedPreferencesStorage.getInstance(getContext());
        Game.tmpNumQuizzes = sps.readIntPreference(SharedPreferencesStorage.PREF_URL_MODE_ONE_PLAYER_NUM_QUIZZES, 10);
        Game.tmpNumPlayers = 1;
        Game.tmpTotalTime = sps.readIntPreference(SharedPreferencesStorage.PREF_URL_MODE_ONE_PLAYER_TOTAL_TIME, 250);

        if (getRandomQuizzesFromSelectedCategories(Game.tmpNumQuizzes)) {
            Game.category = TrivialJSonHelper.getInstance(getContext(), false).getCategoryPlayGameOffLine();
            Game.numQuizzes = Game.tmpNumQuizzes;
            Game.numPlayers = Game.tmpNumPlayers;
            Game.totalTime = Game.tmpTotalTime;

            animateFloatButton();
        }
    }
    // To hide a previously visible view using this effect:

    public void animateFloatButton() {
        final Intent startIntent;
        if (mode.equals(ARG_ONE_PLAYER)) {
            startIntent = QuizActivity.getStartIntent(getActivity(), TrivialJSonHelper.getInstance(getContext(), false).getCategoryPlayGameOffLine());
        } else if (mode.equals(ARG_REAL_TIME_ONLINE)) {
            //startIntent = QuizActivity.getStartIntent(getActivity(), TrivialJSonHelper.getInstance(getContext(), false).getCategoryPlayGameOffLine());

            startIntent = null;
        } else {
            startIntent = null;
            Log.d(TAG, "AnimateFloatButton option incorrect");
            return;
        }

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
                    if (mode.equals(ARG_ONE_PLAYER)) {
                        ActivityCompat.startActivity(getActivity(), startIntent,
                                ActivityOptions.makeSceneTransitionAnimation(getActivity()).toBundle());
                        mSubmitAnswer.show();
                    } else if (mode.equals(ARG_REAL_TIME_ONLINE)) {
                        getActivity().onBackPressed();
                    }

//                    removeFragmentIfMultiplayerGame();
                }
            });
            anim.start();
        } else {
            if (mode.equals(ARG_ONE_PLAYER)) {
                ActivityCompat.startActivity(getActivity(), startIntent,
                        null);
            } else if (mode.equals(ARG_REAL_TIME_ONLINE)) {
                getActivity().onBackPressed();
            }
//            removeFragmentIfMultiplayerGame();
        }
    }

    private void removeFragmentIfMultiplayerGame() {
//        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
//        fragmentTransaction.remove(this);
//        fragmentTransaction.commit();

        FragmentManager fm = getActivity().getSupportFragmentManager();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }


    private boolean getRandomQuizzesOnlineFromSelectedCategories(int numQuizzes) {
        List<TreeNode> selectedNodes = treeView.getSelectedNodes();

        String tmpImg = "";
        String tmpTheme = "";

        ArrayList<Quiz> quizzes = new ArrayList<>();
        Game.listCategories.clear();
        long level = 0;

        TrivialJSonHelper instanceJSON = TrivialJSonHelper.getInstance(getContext(), false);
        for (int i = 0; i < selectedNodes.size(); i++) {
            CategoryJSON category = (CategoryJSON) selectedNodes.get(i).getValue();
            Game.listCategories.add(category.getCategory());

            level += Math.pow(2, instanceJSON.findPosCategory(category.getCategory()));

            getRandomQuizzesOnlineFromSelectedSubCategories(quizzes, category);
        }


        if (quizzes.size() < numQuizzes) {
            ((CategorySelectionActivity) getActivity()).showSnackbarMessage("You need select more categories. At least you need " + numQuizzes + " Quizzes.", "GO ON", false, null);
            return false;
        } else if (Game.listCategories.size() > 31) {
            ((CategorySelectionActivity) getActivity()).showSnackbarMessage("You need select less categories. Less than 31", "GO ON", false, null);
            return false;
        }
        List<Quiz> tmpQuizzes = getRandomizeQuizzes(quizzes, numQuizzes);

        String moreInfo = null;
        String description = null;
        String video = null;

        TrivialJSonHelper.getInstance(getContext(), false).createCategoryPlayGameOffLine(tmpQuizzes, tmpImg, tmpTheme, mode, moreInfo, description, video);
        Game.level = level;
//        Log.d("LEVEL", "Level:" + Game.level);
        return true;
    }

    private void getRandomQuizzesOnlineFromSelectedSubCategories(ArrayList<Quiz> quizzes, CategoryJSON category) {

        if (category.getSubcategories() != null) {
            for (CategoryJSON subcategory : category.getSubcategories()) {
                if (subcategory.getQuizzes() != null) {
                    quizzes.addAll(subcategory.getQuizzes());
                } else {
                    getRandomQuizzesOnlineFromSelectedSubCategories(quizzes, subcategory);
                }
            }
        }
    }

    private boolean getRandomQuizzesFromSelectedCategories(int numQuizzes) {
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

                if (tmpImg.isEmpty()) {
                    TreeNode parent = selectedNodes.get(i).getParent();
                    if (parent != null && parent.getValue() != null) {
                        tmpImg = ((CategoryJSON) parent.getValue()).getImg();
                        tmpTheme = ((CategoryJSON) parent.getValue()).getTheme();
                    } else {
                        tmpImg = ((CategoryJSON) selectedNodes.get(i).getValue()).getImg();
                        tmpTheme = ((CategoryJSON) selectedNodes.get(i).getValue()).getTheme();
                    }
                }
            }
        }


        if (quizzes.size() < numQuizzes) {
            ((CategorySelectionActivity) getActivity()).showSnackbarMessage("You need select more categories. At least you need " + numQuizzes + " Quizzes.", "GO ON", false, null);
            return false;
        }
        List<Quiz> tmpQuizzes = getRandomizeQuizzes(quizzes, numQuizzes);

        String moreInfo = null;
        String description = null;
        String video = null;

        TrivialJSonHelper.getInstance(getContext(), false).createCategoryPlayGameOffLine(tmpQuizzes, tmpImg, tmpTheme, mode, moreInfo, description, video);

        return true;
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

    // Change the fragment mode: ONE_PLAYER, REALTIME_MULTIPLAYER
    public void changeMode(String newMode) {
        if (mode == null || !mode.equals(newMode))
            switch (mode) {
                case QuizActivity.ARG_ONE_PLAYER:

                case QuizActivity.ARG_REAL_TIME_ONLINE:
                    changeTitleActionBar(newMode);
                    mode = newMode;
                    break;
                default:
                    Log.d(TAG, "Mode not supported " + mode);
            }
    }

    private void changeTitleActionBar(String mode) {
        //
        switch (mode) {
            case ARG_ONE_PLAYER:

                ((CategorySelectionActivity) getActivity()).setToolbarTitle(getString(R.string.choose_category));
                break;
            case ARG_REAL_TIME_ONLINE:
                ((CategorySelectionActivity) getActivity()).setToolbarTitle(getString(R.string.choose_category_online));
                ((CategorySelectionActivity) getActivity()).animateToolbarNavigateToSubcategories(false);
                break;
            default:
                break;
        }
    }

    public String getMode() {
        return mode;
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
                if (treeView.getSelectedNodes().size() > 0) {
//                    if (mode.equals(ARG_REAL_TIME_ONLINE)) {
//                        if (inviteItem != null)
//                            inviteItem.setVisible(true);
//                    }
                    mSubmitAnswer.show();
                } else {
                    mSubmitAnswer.hide();
//                    inviteItem.setVisible(false);
                }
            }
        });
        treeView = new TreeView(root, getActivity(), treeViewFactoryNode);

        View tmpView = treeView.getView();
        tmpView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        viewGroup.addView(tmpView);
    }

    private void buildTreeNodes() {
        if (TrivialJSonHelper.getInstance(getActivity(), false).isLoaded()) {
            List<CategoryJSON> categoriesJSON = TrivialJSonHelper.getInstance(getActivity(), false).getCategoriesJSON();
            if (categoriesJSON != null) {
                int level = 0;
                for (CategoryJSON category : categoriesJSON) {
                    TreeNode treeNode = new TreeNode(category);
                    treeNode.setLevel(0);
                    if (category.getSubcategories() != null)
                        buildTreeSubcategory(treeNode, category.getSubcategories(), level + 1);
                    root.addChild(treeNode);
                }
            }
        }
    }

    private void buildTreeSubcategory(TreeNode parentNode, List<CategoryJSON> categoriesJSON, int level) {
        TreeNode treeNode = null;

        if (mode.equals(ARG_REAL_TIME_ONLINE) && level >= MAX_LEVEL) {
            return;
        }
        if (categoriesJSON != null) {
            for (CategoryJSON category : categoriesJSON) {
                if (category.getSubcategories() != null) {
                    treeNode = new TreeNode(category);
                    treeNode.setLevel(level);
                    buildTreeSubcategory(treeNode, category.getSubcategories(), level + 1);
                    parentNode.addChild(treeNode);
                }
            }
        }

    }

    private void initView(View view) {
//        toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
        CategorySelectionActivity.animateViewFullScaleXY(((CategorySelectionActivity) getActivity()).getSubcategory_title(), 100, 300);
        viewGroup = (RelativeLayout) view.findViewById(R.id.container);
        getSubmitButton(view);
//        getActivity().supportStartPostponedEnterTransition();
        buildTree();
    }

    private void getSubmitButton(View view) {
        if (null == mSubmitAnswer) {
            mSubmitAnswer = (CheckableFab) view.findViewById(R.id.submitAnswer);
            mSubmitAnswer.hide();
            mSubmitAnswer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playGame();
//                    mSubmitAnswer.setEnabled(false);
                }
            });
        }

    }
}
