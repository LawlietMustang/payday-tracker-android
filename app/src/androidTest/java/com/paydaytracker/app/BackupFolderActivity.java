package com.paydaytracker.app;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
public class BackupFolderActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);setResult(RESULT_OK,new Intent().setData(Uri.parse("content://com.paydaytracker.backup.tests/tree/root")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION));finish();}
}
