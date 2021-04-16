package com.xgimi.zhuyin.keyboard;

import android.view.MotionEvent;

public interface SoftKeyBoardListener {
	
	public void onCommitText(SoftKey key);
	public void onDelete(SoftKey key);
	public void onBack(SoftKey key);
	public void onTouchEventDown(MotionEvent event, SoftKey downSKey);
	
}
