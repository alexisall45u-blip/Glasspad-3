p = 'android/app/src/main/AndroidManifest.xml'
s = open(p, encoding='utf-8').read()
r = ''
if 'NotesWidget' not in s:
    r += ('    <receiver android:name=".NotesWidget" android:exported="true">\n'
          '        <intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE"/></intent-filter>\n'
          '        <meta-data android:name="android.appwidget.provider" android:resource="@xml/notes_widget_info"/>\n'
          '    </receiver>\n')
if 'QuickNoteActivity' not in s:
    r += ('    <activity android:name=".QuickNoteActivity" android:theme="@style/GPQuick" android:exported="false"\n'
          '        android:excludeFromRecents="true" android:taskAffinity="" android:launchMode="singleInstance"/>\n')
s = s.replace('</application>', r + '</application>')
open(p, 'w', encoding='utf-8').write(s)
