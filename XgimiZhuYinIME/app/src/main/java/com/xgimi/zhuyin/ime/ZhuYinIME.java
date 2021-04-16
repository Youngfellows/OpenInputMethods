package com.xgimi.zhuyin.ime;

import java.util.Arrays;
import java.util.List;

import com.xgimi.xgimizhuyinime.R;
import com.xgimi.zhuyin.ime.dict.Suggest;
import com.xgimi.zhuyin.ime.dict.WordComposer;
import com.xgimi.zhuyin.ime.dict.ZhuYinDictionary;
import com.xgimi.zhuyin.ime.setting.ZhuYinIMESettings;
import com.xgimi.zhuyin.ime.view.CandidateContainer;
import com.xgimi.zhuyin.ime.view.CandidateContainer.OnCandidateLister;
import com.xgimi.zhuyin.ime.view.CandidateView;
import com.xgimi.zhuyin.keyboard.SkbContainer;
import com.xgimi.zhuyin.keyboard.SoftKey;
import com.xgimi.zhuyin.keyboard.SoftKeyBoardListener;
import com.xgimi.zhuyin.keyboard.SoftKeyboard;
import com.xgimi.zhuyin.keyboard.SoftKeyboardView;
import com.xgimi.zhuyin.utils.MeasureHelper;
import com.xgimi.zhuyin.utils.OPENLOG;

import android.content.res.Configuration;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/**
 * 注音输入法服务.
 * 
 * @author hailongqiu
 */
public class ZhuYinIME extends InputMethodService implements SoftKeyBoardListener {

	private static final int KEYCODES_ARRAY_NUM = 12;

	private static final int KEYCODE_ZHUYIN_SKB = 399; // 注音键盘.
	private static final int KEYCODE_ABC_SKB = 400; // 字母键盘.
	private static final int KEYCODE_SYM_123_SKB = 401; // 符号键盘.

	private static final int KEYCODE_LOWERCASE = 402; // 小写转换.
	private static final int KEYCODE_UPPERCASE = 403; // 大写转换.
	private static final int KEYCODE_SYM_ALL_ANGLE = 404; // 全角转换.
	private static final int KEYCODE_SYM_HALF_ANGLE = 405; // 半角转换.

	private SkbContainer mSkbContainer; // 键盘视图.
	private CandidateContainer mCandidateContainer; // 候选框.
	private CandidateView mCandidateView; // 候选框词组视图.

	private String mLocale;
	private Suggest mSuggest; // 注音词库.

	private String mWordZhuYinSeparators;
	private StringBuilder mComposing = new StringBuilder();
	private WordComposer mWord = new WordComposer();
	private int mCorrectionMode;
	private ZhuYinDictionary mUserDictionary;
	private boolean isFirstHide = false;
	private boolean isCandFocus = false;

	@Override
	public void onCreate() {
		super.onCreate();
		/* 初始化注音设置配置 */
		ZhuYinIMESettings.getInstance(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
		/* 初始化LOG */
		OPENLOG.initTag("hailongqiu", true);
		/* 读取屏幕的宽高. */
		MeasureHelper measureHelper = MeasureHelper.getInstance();
		measureHelper.onConfigurationChanged(getResources().getConfiguration(), this);
		/* 初始化词库以及一些其它的东西. */
		initSuggest(getResources().getConfiguration().locale.toString());
	}

	private void initSuggest(String locale) {
		mLocale = locale;
		mSuggest = new Suggest(this, R.raw.main);
		mSuggest.setCorrectionMode(mCorrectionMode);
		mUserDictionary = new ZhuYinDictionary(this);
		mSuggest.setUserDictionary(mUserDictionary);
		// 初始化用于判断注音的字符串
		mWordZhuYinSeparators = getResources().getString(R.string.wrod_zhuyin_separators);
	}

	/**
	 * 创建键盘视图.
	 */
	@Override
	public View onCreateInputView() {
		LayoutInflater inflater = getLayoutInflater();
		mSkbContainer = (SkbContainer) inflater.inflate(R.layout.skb_container, null);
		mSkbContainer.setMoveSoftKey(true);
		mSkbContainer.setMoveDuration(121); // 设置移动边框的时间(默认:300)
		mSkbContainer.setSelectSofkKeyFront(true); // 设置选中边框在最前面.
		mSkbContainer.setOnSoftKeyBoardListener(this); // 键盘连接事件.
		RectF rectf = new RectF(getResources().getDimension(R.dimen.w_15), getResources().getDimension(R.dimen.h_15),
				getResources().getDimension(R.dimen.w_15), getResources().getDimension(R.dimen.h_15));
		mSkbContainer.setSoftKeySelectPadding(rectf);
		return mSkbContainer;
	}

	/**
	 * 创建候选框(词库).
	 */
	@Override
	public View onCreateCandidatesView() {
		LayoutInflater inflater = getLayoutInflater();
		mCandidateContainer = (CandidateContainer) inflater.inflate(R.layout.skb_candidates, null);
		mCandidateView = (CandidateView) mCandidateContainer.findViewById(R.id.candidateview);
		mCandidateContainer.setOnCandidateLister(new OnCandidateLister() {
			@Override
			public void onCandidateSelect(CandidateView candidates, int candSelectIndex) {
				// 点击候选框 的词组 发送文本到编辑框.
				List<CharSequence> candList = candidates.getSuggestions();
				if (candSelectIndex != -1 && candList.size() > candSelectIndex) {
					CharSequence ch = candList.get(candSelectIndex);
					String str = String.valueOf(ch);
					commitResultText(str);
					int selectRow = mSkbContainer.getSelectRow();
					int selectIndex = mSkbContainer.getSelectIndex();
					mSkbContainer.setDefualtSelectKey(selectRow, selectIndex); // 键盘有焦点.
					clear();
				}
			}
		});
		return mCandidateContainer;
	}

	/**
	 * 发送字符到编辑框(EditText)
	 */
	public void commitResultText(String resultText) {
		InputConnection ic = getCurrentInputConnection();
		if (null != ic && !TextUtils.isEmpty(resultText)) {
			ic.commitText(resultText, 1);
		}
	}

	private EditorInfo mSaveEditorInfo;

	@Override
	public void onStartInput(EditorInfo editorInfo, boolean restarting) {
		mSaveEditorInfo = editorInfo;
	}

	@Override
	public void onStartInputView(EditorInfo info, boolean restarting) {
		if (mSkbContainer == null)
			return;

		SoftKey softKey = new SoftKey();
		int inputType = info.inputType & EditorInfo.TYPE_MASK_CLASS;
		switch (inputType) {
		case EditorInfo.TYPE_CLASS_NUMBER: // 切入数字符号键盘.
		case EditorInfo.TYPE_CLASS_DATETIME:
		case EditorInfo.TYPE_CLASS_PHONE:
			softKey.setKeyCode(KEYCODE_SYM_123_SKB);
			onCommitText(softKey);
			//
			break;
		case EditorInfo.TYPE_CLASS_TEXT: // 注音键盘.
			softKey.setKeyCode(KEYCODE_ZHUYIN_SKB);
			onCommitText(softKey);
			break;
		default: // 其它的为字母键盘.
			softKey.setKeyCode(KEYCODE_ABC_SKB);
			onCommitText(softKey);
			break;
		}
		setCandidatesViewShown(true); // 显示候选框.
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		MeasureHelper measureHelper = MeasureHelper.getInstance();
		measureHelper.onConfigurationChanged(newConfig, this);
		super.onConfigurationChanged(newConfig);
	}

	/**
	 * 防止输入法退出还在监听事件.
	 */
	public boolean isImeServiceStop() {
		return ((mSkbContainer == null) || !isInputViewShown());
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (isImeServiceStop()) {
			return super.onKeyDown(keyCode, event);
		}
		// 处理候选框的逻辑.
		if (onCandKeyEvent(keyCode)) {
			return true;
		}
		// 处理键盘逻辑.
		if (mSkbContainer.onSoftKeyDown(keyCode, event)) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * 处理候选框的按键.
	 */
	private boolean onCandKeyEvent(int keyCode) {
		if (mCandidateView.getSuggestions() == null || mCandidateView.getSuggestions().size() <= 0)
			return false;
		int selectRow = mSkbContainer.getSelectRow();
		if (!isCandFocus && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			if (selectRow == 0) {
				isCandFocus = true;
				mSkbContainer.setKeySelected(null);
				mCandidateView.activeCursorUp();
				return true;
			}
		} else if (isCandFocus) {
			int selectIndex = mSkbContainer.getSelectIndex();
			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_DOWN: // 恢复原来的焦点.
				if (selectRow == 0) {
					isCandFocus = false;
					mCandidateView.activeCursorDown(); // 候选词库---失去焦点.
					mSkbContainer.setDefualtSelectKey(selectRow, selectIndex); // 键盘有焦点.
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

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// BUG: 按下键盘上的退出键盘.
		// 导致回车事件传入编辑框，又弹出输入法.
		if (isFirstHide) {
			isFirstHide = false;
			return true;
		}
		if (isImeServiceStop()) {
			return super.onKeyUp(keyCode, event);
		}
		if (isCandFocus || mSkbContainer.onSoftKeyUp(keyCode, event)) {
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onEvaluateFullscreenMode() {
		return false;
	}

	public static final int MAX_INT = Integer.MAX_VALUE / 2 - 1;

	/**
	 * 输入框的光标向右移动.
	 */
	public void setCursorRightMove() {
		int cursorPos = getSelectionStart();
		cursorPos++;
		getCurrentInputConnection().setSelection(cursorPos, cursorPos);
	}

	/**
	 * 输入框的光标向左移动.
	 */
	public void setCursorLeftMove() {
		int cursorPos = getSelectionStart();
		cursorPos -= 1;
		if (cursorPos < 0)
			cursorPos = 0;
		getCurrentInputConnection().setSelection(cursorPos, cursorPos);
	}

	private int getSelectionStart() {
		return getCurrentInputConnection().getTextBeforeCursor(MAX_INT, 0).length();
	}

	/**
	 * 删除字符.
	 */
	private void deleteInputEditChar() {
		getCurrentInputConnection().deleteSurroundingText(1, 0);
	}

	@Override
	public void onCommitText(SoftKey key) {
		String keyStr = key.getKeyLabel();
		int keyCode = key.getKeyCode();
		final SoftKey centerSoftKey;

		switch (keyCode) {
		case KeyEvent.KEYCODE_DEL:
			if (mWord.size() > 0) { // 正在输入注音字母.
				// 删除保存的注音字母.
				mWord.deleteLast();
				final int length = mComposing.length();
				if (length > 0) {
					mComposing.delete(length - 1, length);
				}
				postUpdateSuggestions();
			} else {
				onDelete(key);
			}
			return;
		case KeyEvent.KEYCODE_BACK:
			onBack(key);
			return;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			setCursorLeftMove();
			return;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			setCursorRightMove();
			return;
		case KeyEvent.KEYCODE_ENTER: // 回车.
			sendKeyChar('\n');
			break;
		case KEYCODE_ZHUYIN_SKB: // 注音键盘
			mSkbContainer.setSkbLayout(R.xml.skb_zi_qwerty);
			mSkbContainer.setDefualtSelectKey(0, 3);
			/* 更新回车状态 */
			centerSoftKey = mSkbContainer.getSoftKey(2, 15);
			updateEnterState(mSaveEditorInfo, centerSoftKey);
			clear();
			return;
		case KEYCODE_ABC_SKB: // 字母键盘(小写)
			mSkbContainer.setSkbLayout(R.xml.skb_lc_qwerty);
			mSkbContainer.setDefualtSelectKey(0, 3);
			/* 更新回车状态 */
			centerSoftKey = mSkbContainer.getSoftKey(2, 11);
			updateEnterState(mSaveEditorInfo, centerSoftKey);
			clear();
			return;
		case KEYCODE_SYM_123_SKB: // 符号123键盘
			int layoutID = mSkbContainer.getSkbLayoutId();
			if (layoutID == R.xml.skb_zi_qwerty) {
				// 为中文的注音，切换字符123，是全角的.
				mSkbContainer.setSkbLayout(R.xml.skb_sym123);
			} else {
				// 其它的为半角.
				mSkbContainer.setSkbLayout(R.xml.skb_sym123_half_angle);
			}
			/* 更新回车状态 */
			centerSoftKey = mSkbContainer.getSoftKey(2, 11);
			updateEnterState(mSaveEditorInfo, centerSoftKey);
			clear();
			return;
		case KEYCODE_LOWERCASE: // 小写转换
			mSkbContainer.setSkbLayout(R.xml.skb_lc_qwerty); // 转换到小写字母表.
			mSkbContainer.setDefualtSelectKey(0, 10);
			/* 更新回车状态 */
			centerSoftKey = mSkbContainer.getSoftKey(2, 11);
			updateEnterState(mSaveEditorInfo, centerSoftKey);
			clear();
			return;
		case KEYCODE_UPPERCASE: // 大写转换
			mSkbContainer.setSkbLayout(R.xml.skb_qwerty); // 转换到大写字母表.
			mSkbContainer.setDefualtSelectKey(0, 10);
			/* 更新回车状态 */
			centerSoftKey = mSkbContainer.getSoftKey(2, 11);
			updateEnterState(mSaveEditorInfo, centerSoftKey);
			clear();
			return;
		case KEYCODE_SYM_HALF_ANGLE: // 半角
			mSkbContainer.setSkbLayout(R.xml.skb_sym123_half_angle); // 转换到半角符号表.
			mSkbContainer.setDefualtSelectKey(1, 10);
			/* 更新回车状态 */
			centerSoftKey = mSkbContainer.getSoftKey(2, 11);
			updateEnterState(mSaveEditorInfo, centerSoftKey);
			clear();
			return;
		case KEYCODE_SYM_ALL_ANGLE: // 全角
			mSkbContainer.setSkbLayout(R.xml.skb_sym123); // 转换到全角符号表.
			mSkbContainer.setDefualtSelectKey(1, 10);
			/* 更新回车状态 */
			centerSoftKey = mSkbContainer.getSoftKey(2, 11);
			updateEnterState(mSaveEditorInfo, centerSoftKey);
			clear();
			return;
		case KeyEvent.KEYCODE_SPACE:
			keyStr = " ";
			// commitResultText(" ");
			// return;
		default:
			// 注音字母.
			if (keyCode != -1 && isZhuYinWord(keyCode)) { // 注音字母.
				// 注音字母的keycode处理.
				int[] keyCodes = setZhuYinKeyCodes(keyCode);
				// 更新词库.
				handleCharacter(keyCode, keyCodes);
			} else { // 其它字符.
				if (!TextUtils.isEmpty(keyStr)) {
					// 判断是否是否注音,加入(参照的谷歌注音还有搜狗输入法)
					if (mWord.size() > 0) {
						List<CharSequence> candList = mCandidateView.getSuggestions();
						if (candList != null && candList.size() > 0) {
							String tempStr = candList.get(0) + keyStr;
							keyStr = tempStr;
						}
						clear();
					}
					commitResultText(keyStr);
				}
			}
			break;
		}
	}

	/**
	 * 更新回车按键的状态.
	 */
	private void updateEnterState(EditorInfo info, SoftKey centerSoftKey) {
		String keyLabel = null;
		Drawable keyIcon = null;
		boolean isUpdate = false;
		int action = info.imeOptions & (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION);
		switch (action) {
		case EditorInfo.IME_ACTION_GO:
			keyLabel = getResources().getString(R.string.enter_go);
			break;
		case EditorInfo.IME_ACTION_SEARCH:
			keyIcon = getResources().getDrawable(R.drawable.center_search_icon);
			break;
		case EditorInfo.IME_ACTION_SEND:
			keyLabel = getResources().getString(R.string.enter_send);
			break;
		case EditorInfo.IME_ACTION_NEXT:
			int f = info.inputType & EditorInfo.TYPE_MASK_FLAGS;
			keyLabel = getResources().getString(R.string.enter_next);
			break;
		case EditorInfo.IME_ACTION_DONE:
			keyIcon = getResources().getDrawable(R.drawable.sym_keyboard_return);
			break;
		default:
			break;
		}
		/* 获取 回车键actionLabel 的值. */
		CharSequence charAction = info.actionLabel;
		if (!TextUtils.isEmpty(charAction)) {
			keyLabel = String.valueOf(charAction);
		}
		/* 更新回车的资源 */
		if (keyIcon != null) {
			centerSoftKey.setKeyIcon(keyIcon);
			isUpdate = true;
		}
		if (!TextUtils.isEmpty(keyLabel)) {
			centerSoftKey.setKeyIcon(null);
			centerSoftKey.setKeyLabel(keyLabel);
			isUpdate = true;
		}
		/* 重写刷新键盘布局 */
		if (isUpdate) {
			mSkbContainer.clearCacheBitmap();
		}
	}

	/**
	 * 清空以前的状态.
	 */
	private void clear() {
		isCandFocus = false; // 复位
		mCandidateView.activeCursorDown(); // 复位候选框的焦点问题.
		mComposing.setLength(0);
		mWord.reset();
		postUpdateSuggestions();
	}

	/**
	 * 设置注音的keyCode,并加入上，下，左边的注音keyCode. 然后进入 词库搜索.
	 */
	private int[] setZhuYinKeyCodes(int keyCode) {
		SoftKeyboardView softKeyboardView = mSkbContainer.getSoftKeyboardView();
		SoftKeyboard softKeyboard = softKeyboardView.getSoftKeyboard();

		int[] keyCodes = new int[KEYCODES_ARRAY_NUM];
		Arrays.fill(keyCodes, -1);

		// 点击的注音值.
		keyCodes[0] = keyCode;

		int selectRow = softKeyboard.getSelectRow();
		int selectIndex = softKeyboard.getSelectIndex();

		// 上边的注音值.
		SoftKey topKey = softKeyboard.getSoftKey(selectRow - 1, selectIndex);
		if (topKey != null && isZhuYinWord(topKey.getKeyCode())) {
			keyCodes[1] = topKey.getKeyCode();
		}
		// 下边的注音值.
		SoftKey bottomKey = softKeyboard.getSoftKey(selectRow + 1, selectIndex);
		if (bottomKey != null && isZhuYinWord(bottomKey.getKeyCode())) {
			keyCodes[2] = bottomKey.getKeyCode();
		}
		// 左边的注音值。
		SoftKey leftKey = softKeyboard.getSoftKey(selectRow, selectIndex - 1);
		if (leftKey != null && isZhuYinWord(leftKey.getKeyCode())) {
			keyCodes[3] = leftKey.getKeyCode();
		}
		return keyCodes;
	}

	private void handleCharacter(int primaryCode, int[] keyCodes) {
		mWord.add(primaryCode, keyCodes);
		mComposing.append((char) primaryCode);
		postUpdateSuggestions();
	}

	private void postUpdateSuggestions() {
		mHandler.removeMessages(MSG_UPDATE_SUGGESTIONS);
		mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_UPDATE_SUGGESTIONS), 100);
	}

	private static final int MSG_UPDATE_SUGGESTIONS = 0;

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_SUGGESTIONS:
				updateSuggestions();
				break;
			}
		}
	};

	private void updateSuggestions() {
		mCandidateContainer.setPreviewText(mComposing.toString());
		if (mComposing.length() > 0) {
			// setCandidatesViewShown(true); // 显示候选框.
			mCandidateContainer.showPreviewPopup();
		} else {
			// setCandidatesViewShown(false); // 隐藏候选框.
			mCandidateContainer.hidePreviewPopup();
		}
		// 词库连接测试.
		List<CharSequence> stringList = mSuggest.getSuggestions(mSkbContainer, mWord, false);
		boolean correctionAvailable = mSuggest.hasMinimalCorrection();
		CharSequence typedWord = mWord.getTypedWord();
		boolean typedWordValid = mSuggest.isValidWord(typedWord);
		if (mCandidateView != null) {
			mCandidateView.setSuggestions(stringList, false, typedWordValid, correctionAvailable);
		}
	}

	private boolean isZhuYinWord(int keyCode) {
		return isZhuYinWord(String.valueOf((char) keyCode));
	}

	/**
	 * 判断是否为注音字母.
	 */
	private boolean isZhuYinWord(String str) {
		return mWordZhuYinSeparators.contains(str);
	}

	@Override
	public void onDelete(SoftKey key) {
		deleteInputEditChar();
	}

	@Override
	public void onBack(SoftKey key) {
		requestHideSelf(0);
		isFirstHide = true;
	}

	@Override
	public void onTouchEventDown(MotionEvent event, SoftKey downSKey) {
		// BUG,在按键上到候选框的时候，鼠标点击会导致按键事件需要再按一次.
		// 修复 2016.09.12 hailong.qiu
		if (downSKey != null) {
			isCandFocus = false;
		}
	}

}
