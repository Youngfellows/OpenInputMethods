/*
    Changjie Chinese Input Method for Android
    Copyright (C) 2012 LinkOmnia Ltd.

    Author: Wan Leung Wong (wanleung@linkomnia.com)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.xgimi.ime.changjie;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.util.AttributeSet;

import androidx.annotation.RequiresApi;

import com.xgimi.ime.R;

import java.util.List;

public class IMEKeyboardView extends KeyboardView {

    private Paint mPaint;

    public IMEKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public IMEKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private Context mContext;

    private void init(Context context) {
        mContext = context;

        mKeyTextColor = getResources().getColor(R.color.white);
        //
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(mKeyTextSize);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setAlpha(255);
        //
    }

    @Override
    protected boolean onLongPress(Key key) {
        if (key.codes[0] == IMEKeyboard.KEYCODE_SHIFT) {
            getOnKeyboardActionListener().onKey(IMEKeyboard.KEYCODE_CAPLOCK, null);
            return true;
        } else {
            return super.onLongPress(key);
        }
    }

    int lastKeyIndex = 0;
    private float mShadowRadius;
    private int mShadowColor;
    private int mLabelTextSize = 24;
    private int mKeyTextSize = 24;
    private int mKeyTextColor;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//
//        if (true) {
//            return;
//        }

        IMEKeyboard currentKeyboard = (IMEKeyboard) this.getKeyboard();
        List<Key> keys = currentKeyboard.getKeys();
        //
        final Rect padding = new Rect(0, 0, 0, 0);
        final Paint paint = mPaint;
        //
        // canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR);

        paint.setColor(mKeyTextColor);

        final int keyCount = keys.size();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        for (int i = 0; i < keyCount; i++) {
            final Key key = keys.get(i);
            String label = key.label == null ? null : adjustCase(key.label).toString();

            final int kbdPaddingLeft = 5;
            final int kbdPaddingTop = 5;
            final float raiuds = 8;
            Paint p = new Paint();
            p.setStyle(Paint.Style.FILL);
            RectF rectF = new RectF(
                    key.x + kbdPaddingLeft, key.y + 4 + kbdPaddingTop,
                    key.x + key.width,
                    key.y + key.height
            );

            // 按下焦点问题
//            int[] drawableState = key.getCurrentDrawableState();
//            keyBackground.setState(drawableState);
            if (key.pressed && !mIsCandFocus) {
                p.setColor(getResources().getColor(R.color.key_press_color));
            } else if (lastKeyIndex != -1 && lastKeyIndex == i && !mIsCandFocus) {
                p.setColor(getResources().getColor(R.color.key_focus_color));
            } else {
                p.setColor(getResources().getColor(R.color.key_default_color));
            }

            // 绘制默认/焦点/按下 背景框.
            canvas.drawRoundRect(rectF, raiuds, raiuds, p);
            // int[] drawableState = key.getCurrentDrawableState();
            //
            // for (int stateIndex = 0; stateIndex < drawableState.length; i++) {
            //     int state = drawableState[stateIndex];
            //     android.R.attr.state_pressed
            // }

            if (null != label) { // 文本
                // For characters, use large font. For labels like "Done", use small font.
                if (label.length() > 1 && key.codes.length < 2) {
                    paint.setTextSize(mLabelTextSize);
                    paint.setTypeface(Typeface.DEFAULT_BOLD);
                } else {
                    paint.setTextSize(mKeyTextSize);
                    paint.setTypeface(Typeface.DEFAULT);
                }
                // Draw a drop shadow for the text
                paint.setShadowLayer(mShadowRadius, 0, 0, mShadowColor);

                // 需求: 切换速成的状态
                if (key.codes[0] == IMEKeyboard.KEYCODE_SHIFT && !key.on) {
                    label = "倉頡";
                }

                canvas.drawText(label,
                        key.x + (key.width - padding.left - padding.right) / 2 + padding.left,
                        key.y + (key.height - padding.top - padding.bottom) / 2 + (paint.getTextSize() - paint.descent()) / 2 + padding.top,
                        paint);
                // Turn off drop shadow
                paint.setShadowLayer(0, 0, 0, 0);
            } else if (key.icon != null) { // 图标
                final int drawableX = (key.width - padding.left - padding.right
                        - key.icon.getIntrinsicWidth() + kbdPaddingLeft) / 2 + padding.left;
                final int drawableY = (key.height - padding.top - padding.bottom
                        - key.icon.getIntrinsicHeight() + kbdPaddingTop) / 2 + padding.top;
                final int x = key.x + drawableX;
                final int y = key.y + drawableY;
                key.icon.setBounds(x, y, x + key.icon.getIntrinsicWidth(), y + key.icon.getIntrinsicHeight());
                key.icon.draw(canvas);

                if (key.codes[0] == IMEKeyboard.KEYCODE_SHIFT && currentKeyboard.isShifted()) {
                    p.setColor(0xFF00FF00);
                    canvas.drawCircle(key.x + 30, key.y + 30, 5, p);
                }
            }
        }

    }

    public int getLastKeyIndex() {
        return lastKeyIndex;
    }

    public void setLastKeyIndex(int index) {
        this.lastKeyIndex = index;
        invalidateAllKeys();
    }

    boolean mIsCandFocus = false;

    /**
     * 设置候选框是否上焦点 true 不绘制焦点框 false 反之
     */
    public void setCandContainerFocus(boolean isCandFocus) {
        mIsCandFocus = isCandFocus;
        invalidateAllKeys();
    }

    private CharSequence adjustCase(CharSequence label) {
        if (getKeyboard().isShifted() && label != null && label.length() < 3
                && Character.isLowerCase(label.charAt(0))) {
            label = label.toString().toUpperCase();
        }
        return label;
    }

}