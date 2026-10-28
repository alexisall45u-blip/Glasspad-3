package com.glasspad.notes;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public class NotesWidget extends AppWidgetProvider {
    static final String A_MODE = "com.glasspad.notes.MODE";
    static final String A_CANCEL = "com.glasspad.notes.CANCEL";
    static final String A_TOG = "com.glasspad.notes.TOG";
    static final String A_DEL = "com.glasspad.notes.DEL";
    static final int[] BOX = {R.id.b1, R.id.b2, R.id.b3, R.id.b4, R.id.b5, R.id.b6};
    static final int[] TT = {R.id.t1, R.id.t2, R.id.t3, R.id.t4, R.id.t5, R.id.t6};
    static final int[] SS = {R.id.s1, R.id.s2, R.id.s3, R.id.s4, R.id.s5, R.id.s6};

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(c, m.getAppWidgetOptions(id)));
    }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) {
        m.updateAppWidget(id, build(c, o));
    }

    @Override
    public void onReceive(Context c, Intent i) {
        super.onReceive(c, i);
        String a = i.getAction();
        if (a == null) return;
        SharedPreferences w = c.getSharedPreferences("gpw", Context.MODE_PRIVATE);
        try {
            if (A_MODE.equals(a)) {
                w.edit().putString("mode", "select").putString("sel", "[]").apply();
            } else if (A_CANCEL.equals(a)) {
                w.edit().putString("mode", "view").putString("sel", "[]").apply();
            } else if (A_TOG.equals(a)) {
                String id = i.getStringExtra("id");
                JSONArray s = arr(w, "sel"), ns = new JSONArray();
                boolean found = false;
                for (int k = 0; k < s.length(); k++) {
                    String x = s.getString(k);
                    if (x.equals(id)) found = true; else ns.put(x);
                }
                if (!found && id != null) ns.put(id);
                w.edit().putString("sel", ns.toString()).apply();
            } else if (A_DEL.equals(a)) {
                doDelete(c);
            } else return;
            refresh(c);
        } catch (Exception e) {}
    }

    static JSONArray arr(SharedPreferences p, String k) {
        try { return new JSONArray(p.getString(k, "[]")); } catch (Exception e) { return new JSONArray(); }
    }

    static void refresh(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, NotesWidget.class));
        for (int id : ids) m.updateAppWidget(id, build(c, m.getAppWidgetOptions(id)));
    }

    static void doDelete(Context c) throws Exception {
        SharedPreferences w = c.getSharedPreferences("gpw", Context.MODE_PRIVATE);
        SharedPreferences p = c.getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE);
        JSONArray sel = arr(w, "sel"), pend = arr(p, "pending"), del = arr(p, "pdel"), np = new JSONArray();
        Set<String> S = new HashSet<>();
        for (int k = 0; k < sel.length(); k++) S.add(sel.getString(k));
        for (int k = 0; k < pend.length(); k++) {
            JSONObject o = pend.getJSONObject(k);
            if (!S.contains("~" + o.optLong("ts"))) np.put(o);
        }
        for (String id : S) if (!id.startsWith("~")) del.put(id);
        p.edit().putString("pending", np.toString()).putString("pdel", del.toString()).apply();
        w.edit().putString("sel", "[]").putString("mode", "view").apply();
    }

    static PendingIntent open(Context c, String uri, int code) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        i.setClass(c, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static PendingIntent quick(Context c) {
        Intent i = new Intent(c, QuickNoteActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(c, 3, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static PendingIntent bc(Context c, String act, String id, int code) {
        Intent i = new Intent(c, NotesWidget.class);
        i.setAction(act);
        i.setData(Uri.parse("gp://w/" + act + "/" + id));
        i.putExtra("id", id);
        return PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static RemoteViews build(Context c, Bundle opt) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_notes);
        SharedPreferences p = c.getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE);
        SharedPreferences w = c.getSharedPreferences("gpw", Context.MODE_PRIVATE);
        boolean selMode = "select".equals(w.getString("mode", "view"));
        Set<String> sel = new HashSet<>(), dels = new HashSet<>();
        ArrayList<String[]> L = new ArrayList<>();
        try {
            JSONArray sj = arr(w, "sel"), dj = arr(p, "pdel"), pend = arr(p, "pending"), a = arr(p, "widget");
            for (int k = 0; k < sj.length(); k++) sel.add(sj.getString(k));
            for (int k = 0; k < dj.length(); k++) dels.add(dj.getString(k));
            for (int k = pend.length() - 1; k >= 0; k--) {
                JSONObject o = pend.getJSONObject(k);
                String t = o.optString("t"), b = o.optString("b");
                if (t.isEmpty()) { t = b.length() > 40 ? b.substring(0, 40) : b; b = ""; }
                L.add(new String[]{"~" + o.optLong("ts"), t, b});
            }
            for (int k = 0; k < a.length(); k++) {
                JSONObject o = a.getJSONObject(k);
                if (!dels.contains(o.optString("i")))
                    L.add(new String[]{o.optString("i"), o.optString("t"), o.optString("b")});
            }
        } catch (Exception e) {}
        int h = opt != null ? opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180) : 180;
        int n = Math.max(1, Math.min(6, (int) ((h - 76) / 66f)));
        v.setViewVisibility(R.id.empty, L.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < 6; i++) {
            if (i < n && i < L.size()) {
                String[] r = L.get(i);
                boolean on = sel.contains(r[0]);
                v.setViewVisibility(BOX[i], View.VISIBLE);
                v.setTextViewText(TT[i], selMode ? (on ? "\u2713 " : "\u25CB ") + r[1] : r[1]);
                v.setTextViewText(SS[i], r[2]);
                v.setViewVisibility(SS[i], r[2].isEmpty() ? View.GONE : View.VISIBLE);
                v.setInt(BOX[i], "setBackgroundResource", on ? R.drawable.widget_item_sel_bg : R.drawable.widget_item_bg);
                v.setOnClickPendingIntent(BOX[i], selMode ? bc(c, A_TOG, r[0], 100 + i)
                        : open(c, "glasspad://note/" + r[0], 10 + i));
            } else {
                v.setViewVisibility(BOX[i], View.GONE);
            }
        }
        if (selMode) {
            v.setTextViewText(R.id.hd, sel.size() + " selected");
            v.setViewVisibility(R.id.selbtn, View.GONE);
            v.setViewVisibility(R.id.add, View.GONE);
            v.setViewVisibility(R.id.cancel, View.VISIBLE);
            v.setViewVisibility(R.id.delbtn, sel.isEmpty() ? View.GONE : View.VISIBLE);
            v.setOnClickPendingIntent(R.id.cancel, bc(c, A_CANCEL, "x", 5));
            v.setOnClickPendingIntent(R.id.delbtn, bc(c, A_DEL, "x", 6));
        } else {
            v.setTextViewText(R.id.hd, "Glasspad");
            v.setViewVisibility(R.id.selbtn, L.isEmpty() ? View.GONE : View.VISIBLE);
            v.setViewVisibility(R.id.add, View.VISIBLE);
            v.setViewVisibility(R.id.cancel, View.GONE);
            v.setViewVisibility(R.id.delbtn, View.GONE);
            v.setOnClickPendingIntent(R.id.selbtn, bc(c, A_MODE, "x", 4));
            v.setOnClickPendingIntent(R.id.add, quick(c));
            v.setOnClickPendingIntent(R.id.hd, open(c, "glasspad://home", 2));
        }
        return v;
    }
}
