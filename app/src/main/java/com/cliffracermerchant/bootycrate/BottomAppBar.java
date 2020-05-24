/* Below is a copy of the BottomAppBar.java file from the Android Open Source
 | Project with the following modifications:
 | - The comments have been removed for brevity
 | - The fab alignment mode FAB_ALIGNMENT_MODE_CENTER has been renamed to
 |   FAB_ALIGNMENT_MODE_CENTER_WITH_OFFSET
 | - The function getFabTranslationX(@FabAlignmentMode int fabAlignmentMode)
 |   has been modified to take into account the translationX property of the
 |   floating action button when in FAB_ALIGNMENT_MODE_CENTER_WITH_OFFSET. This
 |   allows for a horizontal offset of the FAB and its cradle.
 | - The shadow compatibility mode of the member materialShapeDrawable is set
 |   to SHADOW_COMPAT_MODE_NEVER instead of SHADOW_COMPAT_MODE_ALWAYS due to
 |   visual artifacts caused by the drawing of the shadow when the FAB vertical
 |   offset is less than 0.*/

/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cliffracermerchant.bootycrate;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout.AttachedBehavior;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewCompat.NestedScrollType;
import androidx.core.view.ViewCompat.ScrollAxis;
import androidx.core.view.WindowInsetsCompat;
import androidx.customview.view.AbsSavedState;

import com.google.android.material.R;
import com.google.android.material.animation.TransformationCallback;
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton.OnVisibilityChangedListener;
import com.google.android.material.internal.ThemeEnforcement;
import com.google.android.material.internal.ViewUtils;
import com.google.android.material.internal.ViewUtils.RelativePadding;
import com.google.android.material.resources.MaterialResources;
import com.google.android.material.shape.EdgeTreatment;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.MaterialShapeUtils;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.google.android.material.internal.ThemeEnforcement.createThemedContext;
import static com.google.android.material.shape.MaterialShapeDrawable.SHADOW_COMPAT_MODE_NEVER;

public class BottomAppBar extends Toolbar implements AttachedBehavior {

  private static final int DEF_STYLE_RES = R.style.Widget_MaterialComponents_BottomAppBar;

  private static final long ANIMATION_DURATION = 300;

  public static final int FAB_ALIGNMENT_MODE_CENTER_WITH_OFFSET = 0;
  public static final int FAB_ALIGNMENT_MODE_END = 1;

  @IntDef({FAB_ALIGNMENT_MODE_CENTER_WITH_OFFSET, FAB_ALIGNMENT_MODE_END})
  @Retention(RetentionPolicy.SOURCE)
  public @interface FabAlignmentMode {}

  public static final int FAB_ANIMATION_MODE_SCALE = 0;
  public static final int FAB_ANIMATION_MODE_SLIDE = 1;

  @IntDef({FAB_ANIMATION_MODE_SCALE, FAB_ANIMATION_MODE_SLIDE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface FabAnimationMode {}

  private final int fabOffsetEndMode;
  private final MaterialShapeDrawable materialShapeDrawable = new MaterialShapeDrawable();

  @Nullable private Animator modeAnimator;
  @Nullable private Animator menuAnimator;
  @FabAlignmentMode private int fabAlignmentMode;
  @FabAnimationMode private int fabAnimationMode;
  private boolean hideOnScroll;

  private int animatingModeChangeCounter = 0;
  private ArrayList<AnimationListener> animationListeners;

  interface AnimationListener {
    void onAnimationStart(BottomAppBar bar);
    void onAnimationEnd(BottomAppBar bar);
  }

  private boolean fabAttached = true;

  private Behavior behavior;

  private int bottomInset;

  @NonNull
  AnimatorListenerAdapter fabAnimationListener =
      new AnimatorListenerAdapter() {
        @Override
        public void onAnimationStart(Animator animation) {
          maybeAnimateMenuView(fabAlignmentMode, fabAttached);
        }
      };

  @SuppressLint("RestrictedApi")
  @NonNull
  TransformationCallback<FloatingActionButton> fabTransformationCallback =
      new TransformationCallback<FloatingActionButton>() {
        @Override
        public void onScaleChanged(@NonNull FloatingActionButton fab) {
          materialShapeDrawable.setInterpolation(
              fab.getVisibility() == View.VISIBLE ? fab.getScaleY() : 0);
        }

        @Override
        public void onTranslationChanged(@NonNull FloatingActionButton fab) {
          float horizontalOffset = fab.getTranslationX();
          if (getTopEdgeTreatment().getHorizontalOffset() != horizontalOffset) {
            getTopEdgeTreatment().setHorizontalOffset(horizontalOffset);
            materialShapeDrawable.invalidateSelf();
          }
          float verticalOffset = -fab.getTranslationY();
          if (getTopEdgeTreatment().getCradleVerticalOffset() != verticalOffset) {
            getTopEdgeTreatment().setCradleVerticalOffset(verticalOffset);
            materialShapeDrawable.invalidateSelf();
          }
          materialShapeDrawable.setInterpolation(
              fab.getVisibility() == View.VISIBLE ? fab.getScaleY() : 0);
        }
      };

  public BottomAppBar(@NonNull Context context) {
    this(context, null, 0);
  }

  public BottomAppBar(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, R.attr.bottomAppBarStyle);
  }
  @SuppressLint("RestrictedApi")
  public BottomAppBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(createThemedContext(context, attrs, defStyleAttr, DEF_STYLE_RES), attrs, defStyleAttr);
    // Ensure we are using the correctly themed context rather than the context that was passed in.
    context = getContext();

    TypedArray a =
        ThemeEnforcement.obtainStyledAttributes(
            context, attrs, R.styleable.BottomAppBar, defStyleAttr, DEF_STYLE_RES);

    ColorStateList backgroundTint =
        MaterialResources.getColorStateList(context, a, R.styleable.BottomAppBar_backgroundTint);

    int elevation = a.getDimensionPixelSize(R.styleable.BottomAppBar_elevation, 0);
    float fabCradleMargin = a.getDimensionPixelOffset(R.styleable.BottomAppBar_fabCradleMargin, 0);
    float fabCornerRadius =
        a.getDimensionPixelOffset(R.styleable.BottomAppBar_fabCradleRoundedCornerRadius, 0);
    float fabVerticalOffset =
        a.getDimensionPixelOffset(R.styleable.BottomAppBar_fabCradleVerticalOffset, 0);
    fabAlignmentMode =
        a.getInt(R.styleable.BottomAppBar_fabAlignmentMode, FAB_ALIGNMENT_MODE_CENTER_WITH_OFFSET);
    fabAnimationMode =
        a.getInt(R.styleable.BottomAppBar_fabAnimationMode, FAB_ANIMATION_MODE_SCALE);
    hideOnScroll = a.getBoolean(R.styleable.BottomAppBar_hideOnScroll, false);

    a.recycle();

    fabOffsetEndMode =
        getResources().getDimensionPixelOffset(R.dimen.mtrl_bottomappbar_fabOffsetEndMode);

    EdgeTreatment topEdgeTreatment =
        new BottomAppBarTopEdgeTreatment(fabCradleMargin, fabCornerRadius, fabVerticalOffset);
    ShapeAppearanceModel shapeAppearanceModel =
        ShapeAppearanceModel.builder().setTopEdge(topEdgeTreatment).build();
    materialShapeDrawable.setShapeAppearanceModel(shapeAppearanceModel);
    materialShapeDrawable.setShadowCompatibilityMode(SHADOW_COMPAT_MODE_NEVER);
    materialShapeDrawable.setPaintStyle(Style.FILL);
    materialShapeDrawable.initializeElevationOverlay(context);
    setElevation(elevation);
    DrawableCompat.setTintList(materialShapeDrawable, backgroundTint);
    ViewCompat.setBackground(this, materialShapeDrawable);

    ViewUtils.doOnApplyWindowInsets(
        this,
        new ViewUtils.OnApplyWindowInsetsListener() {
          @NonNull
          @Override
          public WindowInsetsCompat onApplyWindowInsets(
              View view,
              @NonNull WindowInsetsCompat insets,
              @NonNull RelativePadding initialPadding) {
            bottomInset = insets.getSystemWindowInsetBottom();
            initialPadding.bottom += insets.getSystemWindowInsetBottom();
            initialPadding.applyToView(view);
            return insets;
          }
        });
  }

  @FabAlignmentMode
  public int getFabAlignmentMode() {
    return fabAlignmentMode;
  }

  public void setFabAlignmentMode(@FabAlignmentMode int fabAlignmentMode) {
    maybeAnimateModeChange(fabAlignmentMode);
    maybeAnimateMenuView(fabAlignmentMode, fabAttached);
    this.fabAlignmentMode = fabAlignmentMode;
  }

  @FabAnimationMode
  public int getFabAnimationMode() {
    return fabAnimationMode;
  }

  public void setFabAnimationMode(@FabAnimationMode int fabAnimationMode) {
    this.fabAnimationMode = fabAnimationMode;
  }

  public void setBackgroundTint(@Nullable ColorStateList backgroundTint) {
    DrawableCompat.setTintList(materialShapeDrawable, backgroundTint);
  }

  @Nullable
  public ColorStateList getBackgroundTint() {
    return materialShapeDrawable.getTintList();
  }

  public float getFabCradleMargin() {
    return getTopEdgeTreatment().getFabCradleMargin();
  }

  public void setFabCradleMargin(@Dimension float cradleMargin) {
    if (cradleMargin != getFabCradleMargin()) {
      getTopEdgeTreatment().setFabCradleMargin(cradleMargin);
      materialShapeDrawable.invalidateSelf();
    }
  }

  @Dimension
  public float getFabCradleRoundedCornerRadius() {
    return getTopEdgeTreatment().getFabCradleRoundedCornerRadius();
  }

  public void setFabCradleRoundedCornerRadius(@Dimension float roundedCornerRadius) {
    if (roundedCornerRadius != getFabCradleRoundedCornerRadius()) {
      getTopEdgeTreatment().setFabCradleRoundedCornerRadius(roundedCornerRadius);
      materialShapeDrawable.invalidateSelf();
    }
  }

  @Dimension
  public float getCradleVerticalOffset() {
    return getTopEdgeTreatment().getCradleVerticalOffset();
  }

  public void setCradleVerticalOffset(@Dimension float verticalOffset) {
    if (verticalOffset != getCradleVerticalOffset()) {
      getTopEdgeTreatment().setCradleVerticalOffset(verticalOffset);
      materialShapeDrawable.invalidateSelf();
      setCutoutState();
    }
  }

  public boolean getHideOnScroll() {
    return hideOnScroll;
  }

  public void setHideOnScroll(boolean hide) {
    hideOnScroll = hide;
  }

  public void performHide() {
    getBehavior().slideDown(this);
  }

  public void performShow() {
    getBehavior().slideUp(this);
  }

  @Override
  public void setElevation(float elevation) {
    materialShapeDrawable.setElevation(elevation);
    // Make sure the shadow isn't shown if this view slides down with hideOnScroll.
    int topShadowHeight =
        materialShapeDrawable.getShadowRadius() - materialShapeDrawable.getShadowOffsetY();
    getBehavior().setAdditionalHiddenOffsetY(this, topShadowHeight);
  }

  public void replaceMenu(@MenuRes int newMenu) {
    getMenu().clear();
    inflateMenu(newMenu);
  }

  void addAnimationListener(@NonNull AnimationListener listener) {
    if (animationListeners == null) animationListeners = new ArrayList<>();
    animationListeners.add(listener);
  }

  void removeAnimationListener(@NonNull AnimationListener listener) {
    if (animationListeners == null) return;
    animationListeners.remove(listener);
  }

  private void dispatchAnimationStart() {
    if (animatingModeChangeCounter++ == 0 && animationListeners != null) {
      // Only dispatch the starting event if there are 0 running animations before this one starts.
      for (AnimationListener listener : animationListeners) {
        listener.onAnimationStart(this);
      }
    }
  }

  private void dispatchAnimationEnd() {
    if (--animatingModeChangeCounter == 0 && animationListeners != null) {
      // Only dispatch the ending event if there are 0 running animations after this one ends.
      for (AnimationListener listener : animationListeners)
        listener.onAnimationEnd(this);
    }
  }

  boolean setFabDiameter(@Px int diameter) {
    if (diameter != getTopEdgeTreatment().getFabDiameter()) {
      getTopEdgeTreatment().setFabDiameter(diameter);
      materialShapeDrawable.invalidateSelf();
      return true;
    }

    return false;
  }

  private void maybeAnimateModeChange(@FabAlignmentMode int targetMode) {
    if (fabAlignmentMode == targetMode || !ViewCompat.isLaidOut(this)) return;

    if (modeAnimator != null) modeAnimator.cancel();

    List<Animator> animators = new ArrayList<>();

    if (fabAnimationMode == FAB_ANIMATION_MODE_SLIDE)
      createFabTranslationXAnimation(targetMode, animators);
    else createFabDefaultXAnimation(targetMode, animators);

    AnimatorSet set = new AnimatorSet();
    set.playTogether(animators);
    modeAnimator = set;
    modeAnimator.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationStart(Animator animation) {
            dispatchAnimationStart();
          }

          @Override
          public void onAnimationEnd(Animator animation) {
            dispatchAnimationEnd();
          }
        });
    modeAnimator.start();
  }

  @Nullable
  private FloatingActionButton findDependentFab() {
    View view = findDependentView();
    return view instanceof FloatingActionButton ? (FloatingActionButton) view : null;
  }

  @Nullable
  private View findDependentView() {
    if (!(getParent() instanceof CoordinatorLayout)) {
      // If we aren't in a CoordinatorLayout we won't have a dependent FAB.
      return null;
    }

    List<View> dependents = ((CoordinatorLayout) getParent()).getDependents(this);
    for (View v : dependents) {
      if (v instanceof FloatingActionButton || v instanceof ExtendedFloatingActionButton) {
        return v;
      }
    }

    return null;
  }

  private boolean isFabVisibleOrWillBeShown() {
    FloatingActionButton fab = findDependentFab();
    return fab != null && fab.isOrWillBeShown();
  }

  protected void createFabDefaultXAnimation(
      final @FabAlignmentMode int targetMode, List<Animator> animators) {
    final FloatingActionButton fab = findDependentFab();

    if (fab == null || fab.isOrWillBeHidden()) {
      return;
    }

    dispatchAnimationStart();

    fab.hide(
        new OnVisibilityChangedListener() {
          @Override
          public void onHidden(@NonNull FloatingActionButton fab) {
            fab.setTranslationX(getFabTranslationX(targetMode));
            fab.show(
                new OnVisibilityChangedListener() {
                  @Override
                  public void onShown(FloatingActionButton fab) {
                    dispatchAnimationEnd();
                  }
                });
          }
        });
  }

  private void createFabTranslationXAnimation(
      @FabAlignmentMode int targetMode, @NonNull List<Animator> animators) {
    ObjectAnimator animator =
        ObjectAnimator.ofFloat(findDependentFab(), "translationX", getFabTranslationX(targetMode));
    animator.setDuration(ANIMATION_DURATION);
    animators.add(animator);
  }

  private void maybeAnimateMenuView(@FabAlignmentMode int targetMode, boolean newFabAttached) {
    if (!ViewCompat.isLaidOut(this)) {
      return;
    }

    if (menuAnimator != null) {
      menuAnimator.cancel();
    }

    List<Animator> animators = new ArrayList<>();

    // If there's no visible FAB, treat the animation like the FAB is going away.
    if (!isFabVisibleOrWillBeShown()) {
      targetMode = FAB_ALIGNMENT_MODE_CENTER_WITH_OFFSET;
      newFabAttached = false;
    }

    createMenuViewTranslationAnimation(targetMode, newFabAttached, animators);

    AnimatorSet set = new AnimatorSet();
    set.playTogether(animators);
    menuAnimator = set;
    menuAnimator.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationStart(Animator animation) {
            dispatchAnimationStart();
          }

          @Override
          public void onAnimationEnd(Animator animation) {
            dispatchAnimationEnd();
            menuAnimator = null;
          }
        });
    menuAnimator.start();
  }

  private void createMenuViewTranslationAnimation(
      @FabAlignmentMode final int targetMode,
      final boolean targetAttached,
      @NonNull List<Animator> animators) {

    final ActionMenuView actionMenuView = getActionMenuView();

    // Stop if there is no action menu view to animate
    if (actionMenuView == null) {
      return;
    }

    Animator fadeIn = ObjectAnimator.ofFloat(actionMenuView, "alpha", 1);

    float translationXDifference =
        actionMenuView.getTranslationX()
            - getActionMenuViewTranslationX(actionMenuView, targetMode, targetAttached);

    // If the MenuView has moved at least a pixel we will need to animate it.
    if (Math.abs(translationXDifference) > 1) {
      // We need to fade the MenuView out and in because it's position is changing
      Animator fadeOut = ObjectAnimator.ofFloat(actionMenuView, "alpha", 0);

      fadeOut.addListener(
          new AnimatorListenerAdapter() {
            public boolean cancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
              cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
              if (!cancelled) {
                translateActionMenuView(actionMenuView, targetMode, targetAttached);
              }
            }
          });

      AnimatorSet set = new AnimatorSet();
      set.setDuration(ANIMATION_DURATION / 2);
      set.playSequentially(fadeOut, fadeIn);
      animators.add(set);
    } else if (actionMenuView.getAlpha() < 1) {
      // If the previous animation was cancelled in the middle and now we're deciding we don't need
      // fade the MenuView away and back in, we need to ensure the MenuView is visible
      animators.add(fadeIn);
    }
  }

  private float getFabTranslationY() {
    return -getTopEdgeTreatment().getCradleVerticalOffset();
  }

  private float getFabTranslationX(@FabAlignmentMode int fabAlignmentMode) {
    boolean isRtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
    if (fabAlignmentMode == FAB_ALIGNMENT_MODE_END)
        return (getMeasuredWidth() / 2 - fabOffsetEndMode) * (isRtl ? -1 : 1);
    else {
      final FloatingActionButton fab = findDependentFab();
      return (fab != null) ? fab.getTranslationX() : 0;
    }
  }

  private float getFabTranslationX() {
    return getFabTranslationX(fabAlignmentMode);
  }

  @Nullable
  private ActionMenuView getActionMenuView() {
    for (int i = 0; i < getChildCount(); i++) {
      View view = getChildAt(i);
      if (view instanceof ActionMenuView) {
        return (ActionMenuView) view;
      }
    }

    return null;
  }

  private void translateActionMenuView(
      @NonNull ActionMenuView actionMenuView,
      @FabAlignmentMode int fabAlignmentMode,
      boolean fabAttached) {
    actionMenuView.setTranslationX(
        getActionMenuViewTranslationX(actionMenuView, fabAlignmentMode, fabAttached));
  }

  protected int getActionMenuViewTranslationX(
      @NonNull ActionMenuView actionMenuView,
      @FabAlignmentMode int fabAlignmentMode,
      boolean fabAttached) {
    boolean isRtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
    int toolbarLeftContentEnd = isRtl ? getMeasuredWidth() : 0;

    // Calculate the inner side of the Toolbar's Gravity.START contents.
    for (int i = 0; i < getChildCount(); i++) {
      View view = getChildAt(i);
      boolean isAlignedToStart =
          view.getLayoutParams() instanceof LayoutParams
              && (((LayoutParams) view.getLayoutParams()).gravity
                      & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)
                  == Gravity.START;
      if (isAlignedToStart) {
        toolbarLeftContentEnd =
            isRtl
                ? Math.min(toolbarLeftContentEnd, view.getLeft())
                : Math.max(toolbarLeftContentEnd, view.getRight());
      }
    }

    int end = isRtl ? actionMenuView.getRight() : actionMenuView.getLeft();
    int offset = toolbarLeftContentEnd - end;

    return fabAlignmentMode == FAB_ALIGNMENT_MODE_END && fabAttached ? offset : 0;
  }

  private void cancelAnimations() {
    if (menuAnimator != null) {
      menuAnimator.cancel();
    }
    if (modeAnimator != null) {
      modeAnimator.cancel();
    }
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);

    // If the layout hasn't changed this means the position and size hasn't changed so we don't need
    // to update the position of the cutout and we can continue any running animations. Otherwise,
    // we should stop any animations that might be trying to move things around and reset the
    // position of the cutout.
    if (changed) {
      cancelAnimations();

      setCutoutState();
    }

    // Always ensure the MenuView is in the correct position after a layout.
    setActionMenuViewPosition();
  }

  @NonNull
  private BottomAppBarTopEdgeTreatment getTopEdgeTreatment() {
    return (BottomAppBarTopEdgeTreatment)
        materialShapeDrawable.getShapeAppearanceModel().getTopEdge();
  }

  private void setCutoutState() {
    // Layout all elements related to the positioning of the fab.
    getTopEdgeTreatment().setHorizontalOffset(getFabTranslationX());
    View fab = findDependentView();
    materialShapeDrawable.setInterpolation(fabAttached && isFabVisibleOrWillBeShown() ? 1 : 0);
    if (fab != null) {
      fab.setTranslationY(getFabTranslationY());
      fab.setTranslationX(getFabTranslationX());
    }
  }

  private void setActionMenuViewPosition() {
    ActionMenuView actionMenuView = getActionMenuView();
    if (actionMenuView != null) {
      actionMenuView.setAlpha(1.0f);
      if (!isFabVisibleOrWillBeShown()) {
        translateActionMenuView(actionMenuView, FAB_ALIGNMENT_MODE_CENTER_WITH_OFFSET, false);
      } else {
        translateActionMenuView(actionMenuView, fabAlignmentMode, fabAttached);
      }
    }
  }

  private void addFabAnimationListeners(@NonNull FloatingActionButton fab) {
    fab.addOnHideAnimationListener(fabAnimationListener);
    fab.addOnShowAnimationListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationStart(Animator animation) {
            fabAnimationListener.onAnimationStart(animation);

            // Any time the fab is being shown make sure it is in the correct position.
            FloatingActionButton fab = findDependentFab();
            if (fab != null) {
              fab.setTranslationX(getFabTranslationX());
            }
          }
        });
    fab.addTransformationCallback(fabTransformationCallback);
  }

  private int getBottomInset() {
    return bottomInset;
  }

  @Override
  public void setTitle(CharSequence title) {
    // Don't do anything. BottomAppBar can't have a title.
  }

  @Override
  public void setSubtitle(CharSequence subtitle) {
    // Don't do anything. BottomAppBar can't have a subtitle.
  }

  @NonNull
  @Override
  public Behavior getBehavior() {
    if (behavior == null) {
      behavior = new Behavior();
    }
    return behavior;
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    MaterialShapeUtils.setParentAbsoluteElevation(this, materialShapeDrawable);

    // Automatically don't clip children for the parent view of BottomAppBar. This allows the shadow
    // to be drawn outside the bounds.
    if (getParent() instanceof ViewGroup) {
      ((ViewGroup) getParent()).setClipChildren(false);
    }
  }

  public static class Behavior extends HideBottomViewOnScrollBehavior<BottomAppBar> {

    @NonNull private final Rect fabContentRect;

    private WeakReference<BottomAppBar> viewRef;

    private int originalBottomMargin;

    private final OnLayoutChangeListener fabLayoutListener =
        new OnLayoutChangeListener() {
          @Override
          public void onLayoutChange(
              View v,
              int left,
              int top,
              int right,
              int bottom,
              int oldLeft,
              int oldTop,
              int oldRight,
              int oldBottom) {
            BottomAppBar child = viewRef.get();

            // If the child BAB no longer exists, remove the listener.
            if (child == null || !(v instanceof FloatingActionButton)) {
              v.removeOnLayoutChangeListener(this);
              return;
            }

            FloatingActionButton fab = ((FloatingActionButton) v);

            fab.getMeasuredContentRect(fabContentRect);
            int height = fabContentRect.height();

            // Set the cutout diameter based on the height of the fab.
            child.setFabDiameter(height);

            CoordinatorLayout.LayoutParams fabLayoutParams =
                (CoordinatorLayout.LayoutParams) v.getLayoutParams();

            // Manage the bottomMargin of the fab if it wasn't explicitly set to something. This
            // adds space below the fab if the BottomAppBar is hidden.
            if (originalBottomMargin == 0) {
              // Extra padding is added for the fake shadow on API < 21. Ensure we don't add too
              // much space by removing that extra padding.
              int bottomShadowPadding = (fab.getMeasuredHeight() - height) / 2;
              int bottomMargin =
                  child
                      .getResources()
                      .getDimensionPixelOffset(R.dimen.mtrl_bottomappbar_fab_bottom_margin);
              // Should be moved above the bottom insets with space ignoring any shadow padding.
              int minBottomMargin = bottomMargin - bottomShadowPadding;
              fabLayoutParams.bottomMargin = child.getBottomInset() + minBottomMargin;
            }
          }
        };

    public Behavior() {
      fabContentRect = new Rect();
    }

    public Behavior(Context context, AttributeSet attrs) {
      super(context, attrs);
      fabContentRect = new Rect();
    }

    @Override
    public boolean onLayoutChild(
            @NonNull CoordinatorLayout parent, @NonNull BottomAppBar child, int layoutDirection) {
      viewRef = new WeakReference<>(child);

      View dependentView = child.findDependentView();
      if (dependentView != null && !ViewCompat.isLaidOut(dependentView)) {
        // Set the initial position of the FloatingActionButton with the BottomAppBar vertical
        // offset.
        CoordinatorLayout.LayoutParams fabLayoutParams =
            (CoordinatorLayout.LayoutParams) dependentView.getLayoutParams();
        fabLayoutParams.anchorGravity = Gravity.CENTER | Gravity.TOP;

        // Keep track of the original bottom margin for the fab. We will manage the margin if
        // nothing was set.
        originalBottomMargin = fabLayoutParams.bottomMargin;

        if (dependentView instanceof FloatingActionButton) {
          FloatingActionButton fab = ((FloatingActionButton) dependentView);

          // Always update the BAB if the fab is laid out.
          fab.addOnLayoutChangeListener(fabLayoutListener);

          // Ensure the FAB is correctly linked to this BAB so the animations can run correctly
          child.addFabAnimationListeners(fab);
        }

        // Move the fab to the correct position
        child.setCutoutState();
      }

      // Now let the CoordinatorLayout lay out the BAB
      parent.onLayoutChild(child, layoutDirection);
      return super.onLayoutChild(parent, child, layoutDirection);
    }

    @Override
    public boolean onStartNestedScroll(
        @NonNull CoordinatorLayout coordinatorLayout,
        @NonNull BottomAppBar child,
        @NonNull View directTargetChild,
        @NonNull View target,
        @ScrollAxis int axes,
        @NestedScrollType int type) {
      // We will ask to start on nested scroll if the BottomAppBar is set to hide.
      return child.getHideOnScroll()
          && super.onStartNestedScroll(
              coordinatorLayout, child, directTargetChild, target, axes, type);
    }
  }

  @NonNull
  @Override
  protected Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();
    SavedState savedState = new SavedState(superState);
    savedState.fabAlignmentMode = fabAlignmentMode;
    savedState.fabAttached = fabAttached;
    return savedState;
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    if (!(state instanceof SavedState)) {
      super.onRestoreInstanceState(state);
      return;
    }
    SavedState savedState = (SavedState) state;
    super.onRestoreInstanceState(savedState.getSuperState());
    fabAlignmentMode = savedState.fabAlignmentMode;
    fabAttached = savedState.fabAttached;
  }

  static class SavedState extends AbsSavedState {
    @FabAlignmentMode int fabAlignmentMode;
    boolean fabAttached;

    public SavedState(Parcelable superState) {
      super(superState);
    }

    public SavedState(@NonNull Parcel in, ClassLoader loader) {
      super(in, loader);
      fabAlignmentMode = in.readInt();
      fabAttached = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
      super.writeToParcel(out, flags);
      out.writeInt(fabAlignmentMode);
      out.writeInt(fabAttached ? 1 : 0);
    }

    public static final Creator<SavedState> CREATOR =
        new ClassLoaderCreator<SavedState>() {
          @NonNull
          @Override
          public SavedState createFromParcel(@NonNull Parcel in, ClassLoader loader) {
            return new SavedState(in, loader);
          }

          @Nullable
          @Override
          public SavedState createFromParcel(@NonNull Parcel in) {
            return new SavedState(in, null);
          }

          @NonNull
          @Override
          public SavedState[] newArray(int size) {
            return new SavedState[size];
          }
        };
  }
}
