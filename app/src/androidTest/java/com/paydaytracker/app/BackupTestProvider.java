package com.paydaytracker.app;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

// Standalone test-APK process: use platform Java only, with no target-app dependencies.
public class BackupTestProvider extends DocumentsProvider {
    private boolean failCurrent;
    private File root() { File f=new File(getContext().getFilesDir(),"backup-tests"); f.mkdirs(); return f; }
    private File file(String id) { if (id.equals("root")) return root(); if (!id.startsWith("root/") || id.contains("..")) throw new IllegalArgumentException(); return new File(root(),id.substring(5)); }
    private static final String[] COLUMNS={DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_FLAGS};
    private void row(MatrixCursor c,String id) { File f=file(id); c.addRow(new Object[]{id,id.equals("root")?"Test backups":f.getName(),f.isDirectory()?DocumentsContract.Document.MIME_TYPE_DIR:"application/json",f.isDirectory()?DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE:DocumentsContract.Document.FLAG_SUPPORTS_WRITE|DocumentsContract.Document.FLAG_SUPPORTS_DELETE}); }
    @Override public boolean onCreate(){return true;}
    @Override public Cursor queryRoots(String[] projection){MatrixCursor c=new MatrixCursor(new String[]{DocumentsContract.Root.COLUMN_ROOT_ID,DocumentsContract.Root.COLUMN_DOCUMENT_ID,DocumentsContract.Root.COLUMN_TITLE,DocumentsContract.Root.COLUMN_FLAGS});c.addRow(new Object[]{"root","root","Backup test",DocumentsContract.Root.FLAG_SUPPORTS_CREATE});return c;}
    @Override public Cursor queryDocument(String id,String[] projection){MatrixCursor c=new MatrixCursor(COLUMNS);row(c,id);return c;}
    @Override public Cursor queryChildDocuments(String parent,String[] projection,String order){MatrixCursor c=new MatrixCursor(COLUMNS);File[] files=file(parent).listFiles();if(files!=null)for(File f:files)row(c,"root/"+f.getName());return c;}
    @Override public String createDocument(String parent,String mime,String name)throws FileNotFoundException {if(!parent.equals("root")||name.contains("/"))throw new IllegalArgumentException();try{File f=new File(root(),name);if(!f.createNewFile())throw new IOException();return "root/"+name;}catch(IOException e){throw new FileNotFoundException();}}
    @Override public void deleteDocument(String id)throws FileNotFoundException {if(!file(id).delete())throw new FileNotFoundException();}
    @Override public boolean isChildDocument(String parent,String id){return parent.equals("root")&&id.startsWith("root/");}
    @Override public ParcelFileDescriptor openDocument(String id,String mode,CancellationSignal signal)throws FileNotFoundException {if(failCurrent&&id.endsWith("WageTrack-backup.json")&&mode.contains("w"))throw new FileNotFoundException("Simulated write failure");return ParcelFileDescriptor.open(file(id),ParcelFileDescriptor.parseMode(mode));}
    @Override public Bundle call(String method,String arg,Bundle extras){if(method.equals("fail-current")){failCurrent=true;return new Bundle();}if(method.equals("allow-writes")){failCurrent=false;return new Bundle();}return super.call(method,arg,extras);}
}
