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

import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.xgimi.ime.R;
import com.xgimi.ime.utils.OPENLOG;
import com.xgimi.ime.view.CandidateContainer;
import com.xgimi.ime.view.CandidateView;

import java.util.ArrayList;
import java.util.List;

public class ChangjieIME extends InputMethodService implements
        KeyboardView.OnKeyboardActionListener {
    /**
     * Called when the activity is first created.
     */
    private IMEKeyboardView mInputView;
    // private CandidateView candidateView;

    //private ChangjieTable stroke5WordDictionary;
    private WordProcessor wordProcessor;

    private IMESwitch imeSwitch;

    private char[] charbuffer = new char[5];
    private int strokecount = 0;

    private SharedPreferences mSharedPrefs;
    private int mCurKeyboardKeyNums;
    private Keyboard mCurrentKeyboard;
    private List<Keyboard.Key> nKeys;
    private int mLastKeyIndex = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        //stroke5WordDictionary = new ChangjieTable(this, false);
        //stroke5WordDictionary.open();
        this.wordProcessor = new WordProcessor(this);
        this.wordProcessor.init();
        mSharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        /* 初始化LOG */
        OPENLOG.initTag("hailongqiu", true);
    }

    @Override
    public void onInitializeInterface() {
        imeSwitch = new IMESwitch(this);
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    /**
     * 输入法键盘
     */
    @Override
    public View onCreateInputView() {
        OPENLOG.D("onCreateInputView");
        mInputView = (IMEKeyboardView) getLayoutInflater().inflate(R.layout.main, null);
        mInputView.setPreviewEnabled(false);
        mInputView.setOnKeyboardActionListener(this);
        return mInputView;
    }

    private CandidateContainer mCandidateContainer;
    private com.xgimi.ime.view.CandidateView mCandidateView;

    /**
     * 候选框
     */
    @Override
    public View onCreateCandidatesView() {
        OPENLOG.D("onCreateCandidatesView");

        mCandidateContainer = (CandidateContainer) getLayoutInflater().inflate(R.layout.skb_candidates, null);
        mCandidateView = (com.xgimi.ime.view.CandidateView) mCandidateContainer.findViewById(R.id.candidateview);

        mCandidateContainer.setOnCandidateLister(new CandidateContainer.OnCandidateLister() {
            @Override
            public void onCandidateSelect(CandidateView candidates, int candSelectIndex) {
                // 点击候选框 的词组 发送文本到编辑框.
                List<String> candList = candidates.getSuggestions();
                if (candSelectIndex != -1 && candList.size() > candSelectIndex) {
                	String str = candList.get(candSelectIndex);
                    onChooseWord(str);
                	// commitResultText(str);
                	// int selectRow = mSkbContainer.getSelectRow();
                	// int selectIndex = mSkbContainer.getSelectIndex();
                	// mSkbContainer.setDefualtSelectKey(selectRow, selectIndex); // 键盘有焦点.
                	// clear();
                }
            }
        });

        // candidateView = (CandidateView) getLayoutInflater().inflate(R.layout.candidates, null);
        // candidateView.setDelegate(this);

        return mCandidateContainer;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        this.strokereset();
        //this.mInputView.closing();
    }

    @Override
    public void onFinishInput() {
        this.strokereset();
        super.onFinishInput();

    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        OPENLOG.D("onStartInputView");
        this.imeSwitch.init();
        this.mInputView.setKeyboard(this.imeSwitch.getCurrentKeyboard());
        this.setCandidatesViewShown(true);
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd,
                                  int newSelStart, int newSelEnd,
                                  int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
    }

    @Override
    public void onDisplayCompletions(CompletionInfo[] completions) {
        super.onDisplayCompletions(completions);
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        OPENLOG.D("onKey primaryCode:" + primaryCode);
        if (isImeServiceStop()) {
           return;
        }
        //
        this.wordProcessor.getChinesePhraseDictLinkedHashMap("(");

        // 切换键盘 (123 中/英 ...)
        if (imeSwitch.handleKey(primaryCode)) {
            setKeyPressed(false);
            //
            this.strokereset();

            IMEKeyboard keyboard = imeSwitch.getCurrentKeyboard();
            // 更新回车状态

            // 切换键盘
            this.mInputView.setKeyboard(keyboard);
            return;
        }
        //
        if (primaryCode == Keyboard.KEYCODE_CANCEL) { // 退出输入法键盘
            this.handleClose();
            return;
        }
        if (primaryCode == Keyboard.KEYCODE_DELETE) { // 删除
            this.handleBackspace();
            return;
        }
        if (primaryCode == IMEKeyboard.KEYCODE_ENTER) {
            this.handleEnter();
            return;
        }
        if (primaryCode == Keyboard.KEYCODE_DONE) { //
            return;
        }

        if (primaryCode == 2550) { // 方向左
            setCursorLeftMove();
            return;
        } else if (primaryCode == 2551) { // 方向右
            setCursorRightMove();
            return;
        }

        this.handleKey(primaryCode, keyCodes);
    }

    /**
     * 输入框的光标向右移动.
     */
    public void setCursorRightMove() {
        int cursorPos = getSelectionStart();
        cursorPos++;
        if (null != getCurrentInputConnection()) {
            getCurrentInputConnection().setSelection(cursorPos, cursorPos);
        }
    }

    /**
     * 输入框的光标向左移动.
     */
    public void setCursorLeftMove() {
        int cursorPos = getSelectionStart();
        cursorPos--;
        if (cursorPos < 0)
            cursorPos = 0;
        if (null != getCurrentInputConnection()) {
            getCurrentInputConnection().setSelection(cursorPos, cursorPos);
        }
    }

    private static final int MAX_INT = Integer.MAX_VALUE / 2 - 1;

    private int getSelectionStart() {
        if (null != getCurrentInputConnection()) {
            return getCurrentInputConnection().getTextBeforeCursor(MAX_INT, 0).length();
        }
        return 0;
    }

    /**
     * 根据回车状态.
     */
    private void updateDoneState() {
        EditorInfo editorInfo = getCurrentInputEditorInfo();
        int action = editorInfo.imeOptions & (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        OPENLOG.D("updateDoneState action:" + action);
        switch (action) {
            case EditorInfo.IME_ACTION_GO:
                break;
            case EditorInfo.IME_ACTION_SEARCH:
                break;
            case EditorInfo.IME_ACTION_SEND:
                break;
            case EditorInfo.IME_ACTION_NEXT:
                int f = editorInfo.inputType & EditorInfo.TYPE_MASK_FLAGS;
                if (!isCenterMultiLine(editorInfo)) { //  TOGGLE_ENTER_NEXT
                } else { // TOGGLE_ENTER_MULTI_LINE_DONE
                }
                break;
            case EditorInfo.IME_ACTION_DONE:
                if (!isCenterMultiLine(editorInfo)) { // TOGGLE_ENTER_DONE
                } else { // TOGGLE_ENTER_MULTI_LINE_DONE
                }
            default: // 暂时定为多行. TOGGLE_ENTER_MULTI_LINE_DONE
                break;
        }
    }

    /**
     * 判断是否为多行文本 true 多行 false 反之
     */
    private boolean isCenterMultiLine(EditorInfo editorInfo) {
        int f = editorInfo.inputType & EditorInfo.TYPE_MASK_FLAGS;
        return (f == EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE);
    }

    /**
     * 防止输入法退出还在监听事件.
     */
    public boolean isImeServiceStop() {
        return ((mInputView == null) || !isInputViewShown());
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        OPENLOG.D("onKeyUp keyCode:" + keyCode + " event:" + event);
        // 防止“确认”退出输入法界面，UP的时候再次给 EditText 发送“确认”，导致输入法界面再次启动.
        if (mIsQuitIme) {
            mIsQuitIme = false;
            setKeyPressed(false);
            return true;
        }

        if (isImeServiceStop()) {
            return super.onKeyDown(keyCode, event);
        }

        // 设置属性
        // setKeyBoardFields();

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
                setKeyPressed(false);
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void setKeyPressed(boolean isPressed) {
        if (nKeys != null && mLastKeyIndex < nKeys.size()) {
            Keyboard.Key key = nKeys.get(mLastKeyIndex);
            if (null != key) {
                key.pressed = isPressed;
                mInputView.invalidate();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        OPENLOG.D("onKeyDown keyCode:" + keyCode + " event:" + event);
        if (isImeServiceStop()) {
            return super.onKeyDown(keyCode, event);
        }

        // 设置属性
        setKeyBoardFields();

        // 处理候选框的逻辑.
        if (onCandKeyEvent(keyCode)) {
            return true;
        }

        if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getRepeatCount() == 0)) {
            // Handle the back-key to close the pop-up keyboards.
            if ((mInputView != null) && mInputView.handleBack()) {
                return true;
            }
        }

        // 根据按键移动键盘
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                mLastKeyIndex--;
                if (mLastKeyIndex < 0) {
                    mLastKeyIndex = mCurKeyboardKeyNums - 1;
                }
                mInputView.setLastKeyIndex(mLastKeyIndex);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mLastKeyIndex++;
                if (mLastKeyIndex >= mCurKeyboardKeyNums) {
                    mLastKeyIndex = 0;
                }
                mInputView.setLastKeyIndex(mLastKeyIndex);
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
                Keyboard.Key key = nKeys.get(mLastKeyIndex);
                if (null != key) {
                    key.pressed = true;
                    mInputView.invalidate();
                    int curKeyCode = key.codes[0];
                    onKey(curKeyCode, null);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                Keyboard.Key lastKey = nKeys.get(mLastKeyIndex);

                // if (lastKey.y == 0 && mCandidateView.isShown()) {
                //     mCandidateView.requestFocusTest();
                //     return true;
                // }

                int[] nearestKeyIndices = mCurrentKeyboard.getNearestKeys(
                        nKeys.get(mLastKeyIndex).x, nKeys.get(mLastKeyIndex).y);
//                Log.d("hailong.qiu", "KEYCODE_DPAD_UP nearestKeyIndices len:" + nearestKeyIndices.length
//                        + " nLastKeyIndex:" + nLastKeyIndex);

                for (int i = nearestKeyIndices.length - 1; i >= 0; i--) {
                    int index = nearestKeyIndices[i];
//                    Log.d("hailong.qiu", "KEYCODE_DPAD_UP index:" + index);
                    if (index < mLastKeyIndex) {
                        Keyboard.Key nearKey = nKeys.get(index);
//                        int lxw = lastKey.x + lastKey.width;
//                        int nxw = nearKey.x + nearKey.width;
//                        Log.d("hailong.qiu", "KEYCODE_DPAD_UP index2:" + index
//                                + " lastKey.x:" + lastKey.x + " lxw:" + lxw
//                                + " nearKey.x:" + nearKey.x + " nxw:" + nxw);
                        if (lastKey.y == nearKey.y) {
                            continue;
                        }
                        if ((lastKey.x + lastKey.width) > (nearKey.x)) {
                            mInputView.setLastKeyIndex(index);
                            break;
                        }
                    }
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                nearestKeyIndices = mCurrentKeyboard.getNearestKeys(
                        nKeys.get(mLastKeyIndex).x, nKeys.get(mLastKeyIndex).y);
                Log.d("hailong.qiu", "KEYCODE_DPAD_DOWN nearestKeyIndices len:" + nearestKeyIndices.length
                    + " nLastKeyIndex:" + mLastKeyIndex);
                for (int index : nearestKeyIndices) {
                    if (index > mLastKeyIndex) {
                        Log.d("hailong.qiu", "KEYCODE_DPAD_DOWN index2:" + index);
                        Keyboard.Key nearKey = nKeys.get(index);
                        lastKey = nKeys.get(mLastKeyIndex);
                        if (lastKey.y == nearKey.y) {
                            continue;
                        }
                        if (((lastKey.x >= nearKey.x) // left side compare
                                && (lastKey.x < (nearKey.x + nearKey.width)))
                                || (((lastKey.x + lastKey.width) > nearKey.x) // right side compare
                                && ((lastKey.x + lastKey.width) <= (nearKey.x + nearKey.width)))) {
                            mInputView.setLastKeyIndex(index);
                            break;
                        }
                    }
                }
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void setKeyBoardFields() {
        if (null == mInputView) {
            return;
        }
        mCurrentKeyboard = mInputView.getKeyboard();
        nKeys = mCurrentKeyboard.getKeys();
        mCurKeyboardKeyNums = nKeys.size();
        mLastKeyIndex = mInputView.getLastKeyIndex();
    }

    @Override
    public void onPress(int primaryCode) {
        // TODO Auto-generated method stub       
    }

    @Override
    public void onRelease(int primaryCode) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onText(CharSequence text) {
        // TODO Auto-generated method stub
    }

    @Override
    public void swipeDown() {
        // TODO Auto-generated method stub
    }

    @Override
    public void swipeLeft() {
        // TODO Auto-generated method stub
    }

    @Override
    public void swipeRight() {
        // TODO Auto-generated method stub
    }

    @Override
    public void swipeUp() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onDestroy() {
        if (this.mInputView != null) {
            this.mInputView.closing();
        }
        //this.stroke5WordDictionary.close();
        super.onDestroy();
    }

    private void strokereset() {
        this.charbuffer = new char[5];
        this.strokecount = 0;
        if (this.mCandidateView != null) {
            this.updateInputCode(new String(this.charbuffer, 0, this.strokecount));
            // new
            // this.candidateView.updateInputBox(new String(this.charbuffer, 0, this.strokecount));

            this.updateCandidates(new ArrayList<String>());
        }
    }

    private void handleKey(int keyCode, int[] keyCodes) {
        if (this.imeSwitch.isChinese() && keyCode == ' ') {
            if (this.strokecount == 0) {
                this.handleCharacter(keyCode, keyCodes);
            } else if (this.strokecount == 1) {
                this.onChooseWord(WordProcessor.translateToChangjieCode(new String(this.charbuffer, 0, this.strokecount)));
            } else {
                if (this.mSharedPrefs.getBoolean("setting_quick", false)) {
                    // this.candidateView.goRight();
                    this.mCandidateView.activeCursorRight();
                } else {
                    // if (this.candidateView.getSuggestion().size() > 0) {
                    //     this.onChooseWord(this.candidateView.getSuggestion().get(0));
                    // }
                    if (this.mCandidateView.getSuggestions().size() > 0) {
                        this.onChooseWord(this.mCandidateView.getSuggestions().get(0));
                    }
                }
            }
        } else if (this.imeSwitch.isChinese() && (keyCode >= 'a' && keyCode <= 'z')) {
            this.typingStroke(keyCode);
        } else {
            this.handleCharacter(keyCode, keyCodes);
        }
    }

    private void typingStroke(int keycode) {
        char c = (char) keycode;
        int maxKeyNum = 5;
        if (this.mSharedPrefs.getBoolean("setting_quick", false)) {
            maxKeyNum = 2;
        }
        if (this.strokecount < maxKeyNum) {
            this.charbuffer[this.strokecount++] = c;
        }
        // this.candidateView.updateInputBox(new String(this.charbuffer, 0, this.strokecount));
        // new

        this.updateInputCode(new String(this.charbuffer, 0, this.strokecount));
        ArrayList<String> words = this.wordProcessor.getChineseWordDictArrayList(new String(this.charbuffer, 0, this.strokecount));
        updateCandidates(words);
    }

    private void handleBackspace() {
        if (imeSwitch.isChinese()) {
            if (this.strokecount > 1) {
                this.strokecount -= 1;

                // this.candidateView.updateInputBox(new String(this.charbuffer, 0, this.strokecount));
                // new

                this.updateInputCode(new String(this.charbuffer, 0, this.strokecount));
                ArrayList<String> words = this.wordProcessor.getChineseWordDictArrayList(new String(this.charbuffer, 0, this.strokecount));
                updateCandidates(words);
            } else if (this.strokecount > 0) {
                this.strokereset();
                //this.setCandidatesViewShown(false);
            } else {
                //this.setCandidatesViewShown(false);
                keyDownUp(KeyEvent.KEYCODE_DEL);
                this.strokereset();
            }
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
    }

    private void handleCharacter(int primaryCode, int[] keyCodes) {
        this.strokereset();
        if (isInputViewShown()) {
            if (mInputView.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
                mInputView.setShifted(!(!imeSwitch.getCurrentKeyboard().isCapLock() && mInputView.isShifted()));
            }
        }
        getCurrentInputConnection().commitText(String.format("%c", primaryCode), 1);
    }

    // TODO:回车的状态需要修改. done
    private void handleEnter() {
        if (isCenterMultiLine(getCurrentInputEditorInfo())) { // 如果再文档状态下，属于多行，输入法不应该消失
            commitResultText("\n");
        } else {
            sendKeyChar('\n');
            mIsQuitIme = true;
        }

        // this.keyDownUp(KeyEvent.KEYCODE_ENTER);
    }

    /**
     * 发送字符到编辑框(EditText)
     */
    public void commitResultText(String resultText) {
        OPENLOG.D("commitResultText resultText:" + resultText);
        InputConnection ic = getCurrentInputConnection();
        if (null != ic && !TextUtils.isEmpty(resultText)) {
            ic.commitText(resultText, 1);
        }
    }

    private void keyDownUp(int keyEventCode) {
        if (null != getCurrentInputConnection()) {
            getCurrentInputConnection().sendKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
            getCurrentInputConnection().sendKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
        }
    }

    /**
     * 更新候选框的词组
     */
    private void updateCandidates(ArrayList<String> words) {
        OPENLOG.D("updateCandidates words:" + words.toString());
        // new
        mCandidateView.setSuggestions(words);
        //
        if (words.isEmpty()) {
            // this.candidateView.setSuggestion(words);
            // setCandidatesViewShown(false);
        } else {
            // this.candidateView.setSuggestion(words);
            setCandidatesViewShown(true);
        }
    }

    public void onChooseWord(String word) {
        InputConnection ic = getCurrentInputConnection();
        ic.commitText(word, 1);
        this.strokereset();
        if (this.wordProcessor.getChinesePhraseDictLinkedHashMap(word) != null) {
            // this.candidateView.setSuggestion(new ArrayList<String>(this.wordProcessor.getChinesePhraseDictLinkedHashMap(word)));
            // new
            this.mCandidateView.setSuggestions(new ArrayList<String>(this.wordProcessor.getChinesePhraseDictLinkedHashMap(word)));
        } else { // BUG：当候选框选择后，没有词组后，键盘焦点出问题
            mInputView.setCandContainerFocus(false); // 键盘上焦点
            mCandidateView.activeCursorDown(); // 候选词库---失去焦点.
            isCandFocus = false;
        }
        // //setCandidatesViewShown(false);
    }

    boolean mIsQuitIme = false; // 退出输入法，避免再次 确认 UP的时候再次启动输入法界面

    private void handleClose() {
        this.strokereset();
        requestHideSelf(0);
        mInputView.closing();
        mIsQuitIme = true;
    }

    /**
     * 更新候选框的 候选词
     */
    private void updateInputCode(String code) {
        InputConnection ic = getCurrentInputConnection();
        String output = WordProcessor.translateToChangjieCode(code);
        // ic.setComposingText(output, output.length());

        // 显示预览 词组 弹出候选框.
        mCandidateContainer.setPreviewText(output);
        if (output.length() > 0) {
            // setCandidatesViewShown(true); // 显示候选框.
            mCandidateContainer.showPreviewPopup();
        } else {
            // setCandidatesViewShown(false); // 隐藏候选框.
            mCandidateContainer.hidePreviewPopup();
        }
    }

    boolean isCandFocus = false;

    /**
     * 处理候选框的按键事件处理.
     */
    private boolean onCandKeyEvent(int keyCode) {
        if (mCandidateView.getSuggestions() == null || mCandidateView.getSuggestions().size() <= 0) {
            return false;
        }
        //
        nKeys = mCurrentKeyboard.getKeys();
        if (nKeys != null && nKeys.size() <= 0) {
            return false;
        }
        Keyboard.Key key = nKeys.get(mLastKeyIndex);
        OPENLOG.D("onCandKeyEvent key:" + key);
        boolean isSelectRow = (key != null && key.y <= 0) ? true : false;
        if (!isCandFocus && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (isSelectRow) {
                isCandFocus = true;
                // mSkbContainer.setKeySelected(null);
                mInputView.setCandContainerFocus(true);
                mCandidateView.activeCursorUp();
                return true;
            }
        } else if (isCandFocus) {
            int selectIndex = 0; //mSkbContainer.getSelectIndex();
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_DOWN: // 恢复原来的焦点.
                    if (isSelectRow) {
                        isCandFocus = false;
                        mCandidateView.activeCursorDown(); // 候选词库---失去焦点.
                        // mSkbContainer.setDefualtSelectKey(selectRow, selectIndex); // 键盘有焦点.
                        mInputView.setCandContainerFocus(false);
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    mCandidateView.activeCursorLeft(); // 候选框词库向左.
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    mCandidateView.activeCursorRight(); // 候选框词库向右.
                    return true;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    mCandidateContainer.actionForEnterKey(true); // 回车.
                    return true;
                default:
                    break;
            }
        }
        return false;
    }

}
