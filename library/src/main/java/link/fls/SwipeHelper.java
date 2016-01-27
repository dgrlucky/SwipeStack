/*
 * Copyright (C) 2016 Frederik Schweiger
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

package link.fls;

import android.animation.Animator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import link.fls.util.AnimationUtils;

public class SwipeHelper implements View.OnTouchListener {

    private final SwipeStack mSwipeStack;
    private View mObservedView;

    private boolean mListenForTouchEvents;
    private float mDownX;
    private float mDownY;
    private int mPointerId;

    private float mRotateDegrees = SwipeStack.DEFAULT_SWIPE_ROTATION;
    private float mOpacityEnd = SwipeStack.DEFAULT_SWIPE_OPACITY;
    private int mAnimationDuration = SwipeStack.DEFAULT_ANIMATION_DURATION;

    public SwipeHelper(SwipeStack swipeStack) {
        mSwipeStack = swipeStack;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!mListenForTouchEvents) return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                v.getParent().requestDisallowInterceptTouchEvent(true);
                mSwipeStack.onSwipeStart();
                mPointerId = event.getPointerId(0);
                mDownX = event.getX(mPointerId);
                mDownY = event.getY(mPointerId);

                return true;

            case MotionEvent.ACTION_MOVE:
                int pointerIndex = event.findPointerIndex(mPointerId);
                if (pointerIndex < 0) return false;

                float dx = event.getX(pointerIndex) - mDownX;
                float dy = event.getY(pointerIndex) - mDownY;

                float newX = mObservedView.getX() + dx;
                float newY = mObservedView.getY() + dy;

                mObservedView.setX(newX);
                mObservedView.setY(newY);

                float dragDistanceX = newX - mSwipeStack.getPaddingLeft();
                float swipeProgress = dragDistanceX / mSwipeStack.getWidth();

                mSwipeStack.onSwipeProgress(swipeProgress);

                if (mRotateDegrees > 0) {
                    float rotation = mRotateDegrees * swipeProgress;
                    mObservedView.setRotation(rotation);
                }

                if (mOpacityEnd < 1f) {
                    float alpha = 1 - Math.min(Math.abs(swipeProgress * 2), 1);
                    mObservedView.setAlpha(alpha);
                }

                return true;

            case MotionEvent.ACTION_UP:
                v.getParent().requestDisallowInterceptTouchEvent(false);
                mSwipeStack.onSwipeEnd();
                checkViewPosition();

                return true;

        }

        return false;
    }

    private void checkViewPosition() {
        float viewCenterHorizontal = mObservedView.getX() + (mObservedView.getWidth() / 2);
        float parentFirstThird = mSwipeStack.getWidth() / 3f;
        float parentLastThird = parentFirstThird * 2;

        if (viewCenterHorizontal < parentFirstThird) {
            swipeViewToLeft(mAnimationDuration / 2);
        } else if (viewCenterHorizontal > parentLastThird) {
            swipeViewToRight(mAnimationDuration / 2);
        } else {
            resetViewPosition();
        }
    }

    private void resetViewPosition() {
        mObservedView.animate()
                .x(mSwipeStack.getPaddingLeft())
                .y(mSwipeStack.getPaddingTop())
                .rotation(0)
                .alpha(1)
                .setDuration(mAnimationDuration)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .setListener(null);
    }

    private void swipeViewToLeft(int duration) {
        if (!mListenForTouchEvents) return;
        mListenForTouchEvents = false;
        mObservedView.animate().cancel();
        mObservedView.animate()
                .x(-mSwipeStack.getWidth())
                .rotation(-mRotateDegrees)
                .setDuration(duration)
                .setListener(new AnimationUtils.AnimationEndListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mSwipeStack.onViewSwipedToLeft();
                    }
                });
    }

    private void swipeViewToRight(int duration) {
        if (!mListenForTouchEvents) return;
        mListenForTouchEvents = false;
        mObservedView.animate().cancel();
        mObservedView.animate()
                .x(mSwipeStack.getWidth())
                .rotation(mRotateDegrees)
                .setDuration(duration)
                .setListener(new AnimationUtils.AnimationEndListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mSwipeStack.onViewSwipedToRight();
                    }
                });
    }

    public void registerObservedView(View view) {
        if (view == null) return;
        mObservedView = view;
        mObservedView.setOnTouchListener(this);
        mListenForTouchEvents = true;
    }

    public void unregisterObservedView() {
        if (mObservedView != null) {
            mObservedView.setOnTouchListener(null);
        }
        mObservedView = null;
        mListenForTouchEvents = false;
    }

    public void setAnimationDuration(int duration) {
        mAnimationDuration = duration;
    }

    public void setRotation(float rotation) {
        mRotateDegrees = rotation;
    }

    public void setOpacityEnd(float alpha) {
        mOpacityEnd = alpha;
    }

    public void swipeViewToLeft() {
        swipeViewToLeft(mAnimationDuration);
    }

    public void swipeViewToRight() {
        swipeViewToRight(mAnimationDuration);
    }

}