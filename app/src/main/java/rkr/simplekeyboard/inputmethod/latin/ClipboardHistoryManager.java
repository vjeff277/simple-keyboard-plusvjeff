/*
 * Clipboard history manager for Simple Keyboard.
 */
package rkr.simplekeyboard.inputmethod.latin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat;

/**
 * Manages a bounded clipboard history by listening to primary clip changes and persisting
 * recent items in device-protected SharedPreferences.
 */
final class ClipboardHistoryManager implements ClipboardManager.OnPrimaryClipChangedListener {
    private static final String PREF_KEY_CLIPBOARD_HISTORY = "clipboard_history_json";
    private static final int MAX_HISTORY_ITEMS = 20;

    private final Context mContext;
    private final ClipboardManager mClipboardManager;

    ClipboardHistoryManager(final Context context) {
        mContext = context.getApplicationContext();
        mClipboardManager = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    void start() {
        if (mClipboardManager != null) {
            mClipboardManager.addPrimaryClipChangedListener(this);
        }
    }

    void stop() {
        if (mClipboardManager != null) {
            mClipboardManager.removePrimaryClipChangedListener(this);
        }
    }

    @Override
    public void onPrimaryClipChanged() {
        if (mClipboardManager == null) return;
        final ClipData clip = mClipboardManager.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return;
        final CharSequence coerced = clip.getItemAt(0).coerceToText(mContext);
        if (TextUtils.isEmpty(coerced)) return;
        addToHistory(coerced.toString());
    }

    List<String> getHistory() {
        final SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        final String json = prefs.getString(PREF_KEY_CLIPBOARD_HISTORY, "[]");
        final ArrayList<String> result = new ArrayList<>();
        try {
            final JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                final String item = array.optString(i, null);
                if (!TextUtils.isEmpty(item)) {
                    result.add(item);
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    void addToHistory(final String text) {
        final SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        final String json = prefs.getString(PREF_KEY_CLIPBOARD_HISTORY, "[]");
        final ArrayList<String> items = new ArrayList<>();
        try {
            final JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                final String item = array.optString(i, null);
                if (!TextUtils.isEmpty(item)) {
                    items.add(item);
                }
            }
        } catch (Exception ignored) {
        }

        // Deduplicate: move existing to top
        for (int i = 0; i < items.size(); i++) {
            if (TextUtils.equals(items.get(i), text)) {
                items.remove(i);
                break;
            }
        }
        items.add(0, text);
        while (items.size() > MAX_HISTORY_ITEMS) {
            items.remove(items.size() - 1);
        }

        final JSONArray out = new JSONArray();
        for (final String s : items) {
            out.put(s);
        }
        prefs.edit().putString(PREF_KEY_CLIPBOARD_HISTORY, out.toString()).apply();
    }
}


