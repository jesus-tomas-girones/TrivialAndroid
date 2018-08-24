/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.trivial.upv.android.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.trivial.upv.android.R;
import com.trivial.upv.android.activity.CategorySelectionActivity;
import com.trivial.upv.android.activity.QuizActivity;
import com.trivial.upv.android.adapter.CategoryAdapterJSON;
import com.trivial.upv.android.helper.TransitionHelper;
import com.trivial.upv.android.model.Category;
import com.trivial.upv.android.model.JsonAttributes;
import com.trivial.upv.android.persistence.TrivialJSonHelper;
import com.trivial.upv.android.widget.OffsetDecoration;

public class CategorySelectionFragment extends Fragment {

    //JVG.S
    //private CategoryAdapter mAdapter;
    private CategoryAdapterJSON mAdapter;
    //JVG.E
    private static final int REQUEST_CATEGORY = 0x2300;

    public static CategorySelectionFragment newInstance() {
        return new CategorySelectionFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_categories, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpQuizGrid(view);
    }

    // JVG.S
    public CategoryAdapterJSON getAdapter() {
        return mAdapter;
    }

    private RecyclerView categoriesView;

    // JVG.E
    private void setUpQuizGrid(View view) {

        categoriesView = (RecyclerView) view.findViewById(R.id.categories);

        final int spacing = getContext().getResources()
                .getDimensionPixelSize(R.dimen.spacing_nano);
        categoriesView.addItemDecoration(new OffsetDecoration(spacing));
        //JVG.S
        //mAdapter = new CategoryAdapter(getActivity());
        mAdapter = new CategoryAdapterJSON(getActivity());
        //JVG.E

        //JVG.S
        mAdapter.setOnItemClickListener(
                new CategoryAdapterJSON.OnItemClickListener() {
                    @Override
                    public void onClick(View v, int position) {
                        if (TrivialJSonHelper.getInstance(getContext(), false).getCategoriesCurrent().get(position).getSubcategories() == null) {
                            // Mostrar Quizzes
                            Activity activity = getActivity();
                            startQuizActivityWithTransition(activity,
                                    v.findViewById(R.id.category_title),
                                    mAdapter.getItem(position));
                        } else {
                            // Mostrar Subcategorias
                            TrivialJSonHelper.getInstance(getContext(), false).navigateNextCategory(position);
                            animateTransitionSubcategories(v);

                            TextView textViewSubcategory = (TextView) getActivity().findViewById(R.id.sub_category_title);
                            textViewSubcategory.setText(mAdapter.getItem(position).getId());
                            ((CategorySelectionActivity) getActivity()).animateToolbarNavigateToSubcategories(true);
                        }
                        // JVG.S
                        /*
                        Activity activity = getActivity();
                        startQuizActivityWithTransition(activity,
                                v.findViewById(R.id.category_title),
                                mAdapter.getItem(position));*/
                        // JVG.E
                    }
                });
        mAdapter.setOnLongItemClickListener(new CategoryAdapterJSON.OnLongItemClickListener() {
            @Override
            public void onClick(View v, final int position) {
                ((CategorySelectionActivity) getActivity()).showDeleteProgressConfirmation(position);
            }
        });


        //JVG.E
        categoriesView.setAdapter(mAdapter);
        //JVG.S
        categoriesView.getViewTreeObserver()               .

                        addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                            @Override
                            public boolean onPreDraw() {
                                categoriesView.getViewTreeObserver().removeOnPreDrawListener(this);
                                //        JVG.S
//                                getActivity().supportStartPostponedEnterTransition();
                                if (TrivialJSonHelper.getInstance(getActivity().getApplicationContext(), false).isLoaded()) {
//                                    Log.d("TRAZA", "" + mAdapter.getItemCount());

                                    animateTransitionSubcategories(null);
                                    ((CategorySelectionActivity) getActivity()).showToolbarSubcategories();
                                }
                                //        JVG.E
                                return true;
                            }
                        });

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CATEGORY && resultCode == R.id.solved) {

            /// Actualizar el estado de los test
            /// mAdapter.notifyItemChanged(data.getStringExtra(JsonAttributes.ID));
            mAdapter.notifyItemChanged(data.getStringExtra(JsonAttributes.ID));
            //JVG.E
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startQuizActivityWithTransition(Activity activity, View toolbar,
                                                 Category category) {

        final Pair[] pairs = TransitionHelper.createSafeTransitionParticipants(activity, false,
                new Pair<>(toolbar, activity.getString(R.string.transition_toolbar)));
        @SuppressWarnings("unchecked")
        ActivityOptionsCompat sceneTransitionAnimation = ActivityOptionsCompat
                .makeSceneTransitionAnimation(activity, pairs);

        // Start the activity with the participantsTurnBased, animating from one to the other.
        final Bundle transitionBundle = sceneTransitionAnimation.toBundle();
        Intent startIntent = QuizActivity.getStartIntent(activity, category);
        startActivityForResult(
                startIntent,
                REQUEST_CATEGORY,
                transitionBundle);
    }

    //JVG.S
    // To reveal a previously invisible view using this effect:
    private void showRecyclerView() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            View view = categoriesView;
            // get the center for the clipping circle
            int cx = (view.getLeft() + view.getRight()) / 2;
            int cy = (view.getTop() + view.getBottom()) / 2;

            // get the final radius for the clipping circle
            int finalRadius = Math.max(view.getWidth(), view.getHeight());

            // create the animator for this view (the start radius is zero)
            Animator anim = null;

            anim = ViewAnimationUtils.createCircularReveal(view, cx, cy,
                    0, finalRadius);

            anim.setDuration(500);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    ((CategorySelectionActivity) getActivity()).setInitBlockAnimation(false);
                }
            });

            // make the view visible and start the animation
            view.setVisibility(View.VISIBLE);
            anim.start();
        }
    }

    // To hide a previously visible view using this effect:
    public void animateTransitionSubcategories(final View viewSelectedRecyclerView) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && !((CategorySelectionActivity) getActivity()).getInitBlockAnimation()) {
            ((CategorySelectionActivity) getActivity()).setInitBlockAnimation(true);
            final View view = categoriesView;
            // get the center for the clipping circle

            int cx;
            int cy;

            if (viewSelectedRecyclerView == null) {
                cx = (view.getLeft() + view.getRight()) / 2;
                cy = (view.getTop() + view.getBottom()) / 2;
            } else {
                cx = (viewSelectedRecyclerView.getLeft() + viewSelectedRecyclerView.getRight()) / 2;
                cy = (viewSelectedRecyclerView.getTop() + viewSelectedRecyclerView.getBottom()) / 2;
            }

            // get the initial radius for the clipping circle
            int initialRadius = view.getWidth();

            // create the animation (the final radius is zero)
            Animator anim = ViewAnimationUtils.createCircularReveal(view, cx, cy,
                    initialRadius, 0);
            anim.setDuration(500);

            // make the view invisible when the animation is done
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    view.setVisibility(View.INVISIBLE);
                    mAdapter.updateCategories();
                    mAdapter.notifyDataSetChanged();
                    showRecyclerView();
                }
            });

            // start the animation
            anim.start();
        }
    }


}
