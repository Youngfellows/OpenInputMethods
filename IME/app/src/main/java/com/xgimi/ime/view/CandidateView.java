package com.xgimi.ime.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.xgimi.ime.R;

public class CandidateView extends View {

	private static final int MAX_SUGGESTIONS = 200;
	private int X_GAP = 10;

	private Drawable mSelectionHighlight; // 词库选中的图片.
	private static final List<String> EMPTY_LIST = new ArrayList<String>();
	private int[] mWordWidth = new int[MAX_SUGGESTIONS];
	private int[] mWordX = new int[MAX_SUGGESTIONS];
	private List<String> mSuggestions = EMPTY_LIST;

	private Paint mPaint;
	private float mTextSize;
	private int mDescent;
	private int mColorNormal;
	private int mColorSelect;
	private int mTotalWidth;

	private int mSelectedIndex = 0;
	private boolean isFocus = false;
	private int mTargetScrollX;

	private Context mContext;

	public CandidateView(Context context) {
		this(context, null, 0);
	}

	public CandidateView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CandidateView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mContext = context;
		initPaint();
		/* 初始化一些资源 */
		mSelectionHighlight = getResources().getDrawable(R.drawable.candidate_bg_select2);
		/* 设置一些属性 */
		setHorizontalFadingEdgeEnabled(true);
		setWillNotDraw(false);
		setHorizontalScrollBarEnabled(false);
		setVerticalScrollBarEnabled(false);
	}

	private void initPaint() {
		mColorNormal = mContext.getResources().getColor(R.color.candidate_normal);
		mColorSelect = mContext.getResources().getColor(R.color.candidate_select);
		
		mTextSize = mContext.getResources().getDimension(R.dimen.w_30);
		X_GAP = (int) mContext.getResources().getDimension(R.dimen.w_10);
		//
		mPaint = new Paint();
		mPaint.setColor(mColorNormal);
		mPaint.setAntiAlias(true);
		mPaint.setTextSize(mTextSize);
		mPaint.setStrokeWidth(0);
		mDescent = (int) mPaint.descent();
	}

	public static Integer getMaxSuggest() {
		return MAX_SUGGESTIONS;
	}

	/**
	 * 获取词组.
	 */
	public List<String> getSuggestions() {
		return this.mSuggestions;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (canvas == null || mSuggestions == null) {
			super.onDraw(canvas);
		}
		//
		final int count = mSuggestions.size();
		final Paint paint = mPaint;
		int x = 0;
		final int height = getHeight();
		final int y = (int) (height + mPaint.getTextSize() - mDescent) / 2;
		//
		for (int i = 0; i < count; i++) {
			try {
				String suggestion = mSuggestions.get(i);
				if (TextUtils.isEmpty(suggestion)) {
					continue;
				}
				paint.setColor(mColorNormal);
				/* 字体宽度保存(获取). */
				final int wordWidth;
				if (mWordWidth[i] != 0) {
					wordWidth = mWordWidth[i];
				} else {
					float textWidth = paint.measureText(suggestion, 0, suggestion.length());
					wordWidth = (int) textWidth + X_GAP * 2;
					mWordWidth[i] = wordWidth;
				}
				mWordX[i] = x;
				int draw_x = x - (int) mTargetScrollX;

				paint.setColor(mColorNormal);

				if (isFocus && (i == mSelectedIndex)) {
					paint.setColor(mColorSelect);
					mSelectionHighlight.setBounds(draw_x, 0, draw_x + wordWidth, height);
					mSelectionHighlight.draw(canvas);
				}

				canvas.drawText(suggestion, 0, suggestion.length(), draw_x + X_GAP, y, paint);
				paint.setTypeface(Typeface.DEFAULT);
				x += wordWidth; // 字体绘制的x位置.
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//
		mTotalWidth = x;
		//
		// int availableWidth = getWidth();
		// int neededWidth = mTotalWidth;
		// boolean rightVisible = (mTargetScrollX + availableWidth) <
		// neededWidth;
		// if (rightVisible) {
		// requestLayout();
		// }
	}

	public void setSuggestions(List<String> suggestions) {
		setSuggestions(suggestions, false, false, false);
	}

	/**
	 * 设置候选框显示的词库.
	 */
	public void setSuggestions(List<String> suggestions, boolean completions, boolean typedWordValid,
			boolean haveMinimalSuggestion) {
		clear();
		if (suggestions != null) {
			mSuggestions = suggestions;
			// mSuggestions.addAll(suggestions); // test 翻页
			// mSuggestions.addAll(suggestions); // test 翻页
		}
		mTargetScrollX = 0;
		mTotalWidth = 0;
		invalidate();
		requestLayout();
	}

	public void clear() {
		mSelectedIndex = 0;
		Arrays.fill(mWordWidth, 0);
		Arrays.fill(mWordX, 0);
		invalidate();
		// requestLayout();
	}

	public void scrollNext() {
		int i = 0;
		int targetX = mTargetScrollX;
		final int count = mSuggestions.size();
		int rightEdge = mTargetScrollX + getWidth();
		// 取靠近 width 位置的wordX.
		while (i < count) {
			if (mWordX[i] <= rightEdge && mWordX[i] + mWordWidth[i] >= rightEdge) {
				targetX = mWordX[i];
				break;
			}
			i++;
		}

		if (i > count) {
			targetX = mWordX[i - 1];
		}

		updateScrollPosition(targetX);
	}

	public void scrollPrev() {
		int i = 0;
		final int count = mSuggestions.size();
		int firstItem = 0;

		while (i < count) {
			if (mWordX[i] == mTargetScrollX) {
				firstItem = i;
				break;
			}
			i++;
		}

		int leftEdge = mWordX[firstItem] - getWidth();

		if (leftEdge < 0) {
			leftEdge = 0;
		}

		i = 0;

		while (i < count) {
			if (mWordX[i] >= leftEdge) {
				leftEdge = mWordX[i];
				break;
			}
			i++;
		}
		updateScrollPosition(leftEdge);
	}

	/**
	 * 更新翻页效果.
	 */
	private void updateScrollPosition(int targetX) {
		mTargetScrollX = targetX;
		// setScrollXAnimation(targetX);
		requestLayout();
		invalidate();
	}

	// 候选词库视图--回车按下.
	public int actionForEnterDown() {
		return this.mSelectedIndex;
	}

	// 候选词库视图--回车松开
	public int actionForEnterUp() {
		return this.mSelectedIndex;
	}

	// 候选词库视图--向上--获取焦点.
	public void activeCursorUp() {
		isFocus = true;
		invalidate();
	}

	// 候选词库视图--向下--失去焦点.
	public void activeCursorDown() {
		isFocus = false;
		invalidate();
	}

	// 候选词库视图向右.
	public void activeCursorRight() {
		mSelectedIndex++;
		final int count = mSuggestions.size();
		if (mSelectedIndex >= count) {
			mSelectedIndex = count - 1;
		}

		/* 词组翻页 */
		int targetX = mTargetScrollX;
		int rightEdge = mTargetScrollX + getWidth();

		if (mWordX[mSelectedIndex] <= rightEdge && (mWordX[mSelectedIndex] + mWordWidth[mSelectedIndex]) >= rightEdge) {
			targetX = mWordX[mSelectedIndex];
			updateScrollPosition(targetX);
		} else if (mWordX[mSelectedIndex] > rightEdge) {
			targetX = mWordX[mSelectedIndex - 1];
			updateScrollPosition(targetX);
		}

		invalidate();
	}

	// 候选词库视图向左.
	public void activeCursorLeft() {
		mSelectedIndex--;
		if (mSelectedIndex < 0) {
			mSelectedIndex = 0;
		}

		/* 词组翻页 */
		if (mWordX[mSelectedIndex] == mTargetScrollX) {
			int leftEdge = mWordX[mSelectedIndex] - getWidth();

			if (leftEdge <= 0) {
				leftEdge = 0;
			}

			updateScrollPosition(leftEdge);
		} else if (mWordX[mSelectedIndex] < mTargetScrollX) {
			int leftEdge = mWordX[mSelectedIndex + 1] - getWidth();
			if (leftEdge <= 0) {
				leftEdge = 0;
			}
			updateScrollPosition(leftEdge);
		}
		invalidate();
	}

	/**
	 * 词组翻页动画.
	 */
	private void setScrollXAnimation(int targetX) {
		PropertyValuesHolder valuesXHolder = PropertyValuesHolder.ofInt("scrollX", 0, targetX);
		final ObjectAnimator scrollAnimator = ObjectAnimator.ofPropertyValuesHolder(this, valuesXHolder);
		scrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				mTargetScrollX = (Integer) animation.getAnimatedValue("scrollX");
				invalidate();
			}
		});
		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.playTogether(scrollAnimator);
		animatorSet.setDuration(300);
		animatorSet.start();
	}

	/**
	 * 候选框获取鼠标点击获取词组.
	 */
	public int getCandSuggestion(int touchX) {
		int count = mSuggestions.size();
		for (int i = 0; i < count; i++) {
			int wordX = mWordX[i];
			int wordWidth = mWordWidth[i];
			if (touchX + mTargetScrollX >= wordX && touchX + mTargetScrollX < wordX + wordWidth) {
				return i;
			}
		}
		return -1;
	}

	@Override
	protected int computeHorizontalScrollRange() {
		return mTotalWidth;
	}

	public int getTargetScrollX() {
		return mTargetScrollX;
	}

	public int getContentSize() {
		int mSize;
		if (mSuggestions == null)
			return 0;
		else {
			mSize = mSuggestions.size();
			return mSize;
		}
	}

}
