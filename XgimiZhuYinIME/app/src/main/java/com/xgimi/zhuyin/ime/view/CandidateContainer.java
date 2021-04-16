package com.xgimi.zhuyin.ime.view;

import com.xgimi.xgimizhuyinime.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

public class CandidateContainer extends LinearLayout implements View.OnTouchListener {

	private View mButtonLeft;
	private View mButtonRight;
	private CandidateView mCandidates;
	private OnCandidateLister mOnCandidateLister; // 候选框事件.
	private PopupWindow mPreviewPopup; // 显示注音字母窗口
	private TextView mPreviewText;

	public CandidateContainer(Context context) {
		this(context, null);
	}

	public CandidateContainer(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CandidateContainer(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initAllViews();
		initPreviewPopup();
	}

	private void initAllViews() {
		View.inflate(getContext(), R.layout.candidates_layout_view, this);
		mButtonLeft = findViewById(R.id.candidate_left);
		mButtonRight = findViewById(R.id.candidate_right);
		// 候选框视图(绘制词组的view)
		mCandidates = (CandidateView) findViewById(R.id.candidateview);
		//
		mButtonLeft.setOnTouchListener(this);
		mButtonRight.setOnTouchListener(this);
		mCandidates.setOnTouchListener(mOnTouchListener);
	}

	/**
	 * 初始化注音字母预览窗口.
	 */
	private void initPreviewPopup() {
		LayoutInflater inflate = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mPreviewPopup = new PopupWindow(getContext());
		mPreviewText = (TextView) inflate.inflate(R.layout.candidate_zhuyin_preview, null);
		mPreviewPopup.setContentView(mPreviewText);
		mPreviewPopup.setWindowLayoutMode(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		mPreviewPopup.setBackgroundDrawable(null);
	}

	public void setPreviewText(String wordText) {
		mPreviewText.setText(wordText);
	}

	public void showPreviewPopup() {
		if (!mPreviewPopup.isShowing()) {
			mPreviewText.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			final int popupHeight = mPreviewText.getMeasuredHeight();
			mPreviewPopup.showAtLocation(this, Gravity.NO_GRAVITY, 0, -popupHeight);
		}
	}

	public void hidePreviewPopup() {
		if (mPreviewPopup.isShowing()) {
			mPreviewPopup.dismiss();
		}
	}

	/**
	 * 候选框鼠标
	 */
	OnTouchListener mOnTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			int x = (int) event.getX();
			int y = (int) event.getY();
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				break;
			case MotionEvent.ACTION_MOVE:
				break;
			case MotionEvent.ACTION_UP:
				int selectIndex = mCandidates.getCandSuggestion(x);
				if (mOnCandidateLister != null) {
					mOnCandidateLister.onCandidateSelect(mCandidates, selectIndex);
				}
				break;
			}
			return false;
		}
	};

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();
		if (action == MotionEvent.ACTION_DOWN) {
			if (v == mButtonRight) {
				mCandidates.scrollNext(); // 下一页.
			} else if (v == mButtonLeft) {
				mCandidates.scrollPrev(); // 上一页.
			}
		} else if (action == MotionEvent.ACTION_UP) {
		}
		return false;
	}

	// @Override
	// public void requestLayout() {
	// if (mCandidates != null) {
	// int availableWidth = mCandidates.getWidth();
	// int neededWidth = mCandidates.computeHorizontalScrollRange();
	// int x = mCandidates.getTargetScrollX();
	// boolean leftVisible = x > 0;
	// boolean rightVisible = (x + availableWidth) < neededWidth;
	// OPENLOG.D("leftVisible:" + leftVisible + " rightVisible:" + rightVisible
	// + " neededWidth:" + neededWidth);
	// if (mButtonLeftLayout != null) {
	// mButtonLeftLayout.setEnabled(leftVisible ? true : false);
	// }
	// if (mButtonRightLayout != null) {
	// mButtonRightLayout.setEnabled(leftVisible ? true : false);
	// }
	// }
	// super.requestLayout();
	// }

	// KEYCODE_ENTER | KEYCODE_DPAD_CENTER(按下，按键)
	public void actionForEnterKey(boolean isDown) {
		if (isDown) {
			int selectIndex = mCandidates.actionForEnterDown();
			if (mOnCandidateLister != null)
				mOnCandidateLister.onCandidateSelect(mCandidates, selectIndex);
		} else {
			mCandidates.actionForEnterUp();
		}
	}

	public void setOnCandidateLister(OnCandidateLister onCandidateLister) {
		this.mOnCandidateLister = onCandidateLister;
	}

	public static interface OnCandidateLister {
		void onCandidateSelect(CandidateView candidates, int selectIndex);
	};

}
