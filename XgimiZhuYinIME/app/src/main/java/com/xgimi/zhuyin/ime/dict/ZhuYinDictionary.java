package com.xgimi.zhuyin.ime.dict;

import java.util.ArrayList;

import com.xgimi.zhuyin.ime.setting.ZhuYinIMESettings;
import com.xgimi.zhuyin.ime.view.CandidateView;
import com.xgimi.zhuyin.utils.OPENLOG;

import android.content.Context;
import android.database.Cursor;
import android.provider.UserDictionary.Words;

public class ZhuYinDictionary extends Dictionary {
	private static final String TAG = "ZhuYinIME";
	private Integer MaxWords; 
	
    private static final String[] PROJECTION = {
        Words._ID,
        Words.WORD,
        Words.FREQUENCY
    };
	
	private Context mContext;

	public ZhuYinDictionary(Context context) {
		mContext = context;
		MaxWords = CandidateView.getMaxSuggest();
	}


	@Override
	public void getWords(WordComposer composer, WordCallback callback) {
		OPENLOG.D("start start start");
		//Log.i(TAG, "getWords:getTypedWord:"+composer.getTypedWord().toString().length());
		//Log.i(TAG, "getWords:getCodesAt:"+composer.getTypedWord().length());
		//Log.i(TAG, "getWords:composer's length:"+composer.getCodesAt(0).length);
		
		String code="";
		for(int i=0;i<composer.getTypedWord().length();i++){
			code += String.valueOf(composer.getCodesAt(i)[0]);
		}
		//Log.i(TAG, "getWords:code:"+code);
		String[] result=this.loadWordDB(code);
		OPENLOG.D("result:" + result.length);
		for(String s: result){
			char[] word = s.toCharArray();			
			callback.addWord(word, 0, word.length, 10);
		}
		OPENLOG.D("end end end");
	}

	@Override
	public boolean isValidWord(CharSequence word) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public void useWordDB(String word) {
		ZhuYinDictionaryProvider zdb = new ZhuYinDictionaryProvider(mContext);
		zdb.open();
		zdb.useWords(word);
		zdb.close();
	}
		
	public String[] loadWordDB(String code){
		char[] word=null;
		Integer leftWords = ZhuYinIMESettings.getCandidateCnt(); // 搜索字词笔数.(在设置界面中可以看到)
		Cursor cursor;
		ZhuYinDictionaryProvider zdb = new ZhuYinDictionaryProvider(mContext);
		zdb.open();

		ArrayList<String> result = new ArrayList<String>();
		
		zdb.setSearchCode(code);
		
		cursor = zdb.getWordsExactly(leftWords);
		if(cursor.moveToFirst()){
			while(!cursor.isAfterLast()){
				String aword = cursor.getString(0);
				result.add(aword);
				cursor.moveToNext();
				leftWords--;
			}
		}

		if(leftWords > 0) {
			cursor = zdb.getWordsRough(leftWords);
			if(cursor.moveToFirst()){
				while(!cursor.isAfterLast()){
					String aword = cursor.getString(0);
					result.add(aword);
					cursor.moveToNext();
					leftWords--;
				}
			}
		}
		
		if(leftWords > 0) {
			cursor = zdb.getPhrases(leftWords);
			if(cursor.moveToFirst()){
				while(!cursor.isAfterLast()){
					String aword = cursor.getString(0);
					result.add(aword);
					cursor.moveToNext();
					leftWords--;
				}
			}
		}

		zdb.close();
		return result.toArray(new String[0]);
	}
}
