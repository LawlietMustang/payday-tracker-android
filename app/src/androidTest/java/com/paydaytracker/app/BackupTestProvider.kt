package com.paydaytracker.app

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract as Docs
import android.provider.DocumentsProvider
import java.io.File

// Instrumentation-only provider: failures affect its private test directory, never user files.
class BackupTestProvider : DocumentsProvider() {
    private var failCurrent=false
    private val root get()=File(requireNotNull(context).filesDir,"backup-tests").apply {mkdirs()}
    override fun onCreate()=true
    private fun file(id:String):File {require(id=="root"||id.startsWith("root/"));val f=if(id=="root")root else File(root,id.removePrefix("root/"));require(f.canonicalPath==root.canonicalPath||f.canonicalPath.startsWith(root.canonicalPath+"/"));return f}
    private fun row(cursor:MatrixCursor,id:String){val f=file(id);cursor.newRow().apply {add(Docs.Document.COLUMN_DOCUMENT_ID,id);add(Docs.Document.COLUMN_DISPLAY_NAME,if(id=="root")"Test backups" else f.name);add(Docs.Document.COLUMN_MIME_TYPE,if(f.isDirectory)Docs.Document.MIME_TYPE_DIR else "application/json");add(Docs.Document.COLUMN_FLAGS,if(f.isDirectory)Docs.Document.FLAG_DIR_SUPPORTS_CREATE else Docs.Document.FLAG_SUPPORTS_WRITE or Docs.Document.FLAG_SUPPORTS_DELETE)}}
    override fun queryRoots(projection:Array<out String>?):Cursor = MatrixCursor(arrayOf(Docs.Root.COLUMN_ROOT_ID,Docs.Root.COLUMN_DOCUMENT_ID,Docs.Root.COLUMN_TITLE,Docs.Root.COLUMN_FLAGS)).apply {addRow(arrayOf("root","root","Backup test",Docs.Root.FLAG_SUPPORTS_CREATE))}
    override fun queryDocument(documentId:String,projection:Array<out String>?):Cursor=MatrixCursor(arrayOf(Docs.Document.COLUMN_DOCUMENT_ID,Docs.Document.COLUMN_DISPLAY_NAME,Docs.Document.COLUMN_MIME_TYPE,Docs.Document.COLUMN_FLAGS)).also {row(it,documentId)}
    override fun queryChildDocuments(parentDocumentId:String,projection:Array<out String>?,sortOrder:String?):Cursor=MatrixCursor(arrayOf(Docs.Document.COLUMN_DOCUMENT_ID,Docs.Document.COLUMN_DISPLAY_NAME,Docs.Document.COLUMN_MIME_TYPE,Docs.Document.COLUMN_FLAGS)).also {c->file(parentDocumentId).listFiles()?.forEach {row(c,"root/"+it.name)}}
    override fun createDocument(parentDocumentId:String,mimeType:String,displayName:String):String {require(parentDocumentId=="root"&&!displayName.contains('/'));val f=File(root,displayName);check(f.createNewFile());return "root/"+f.name}
    override fun deleteDocument(documentId:String){check(file(documentId).delete())}
    override fun isChildDocument(parentDocumentId:String,documentId:String)=parentDocumentId=="root"&&documentId.startsWith("root/")
    override fun openDocument(documentId:String,mode:String,signal:CancellationSignal?):ParcelFileDescriptor {if(failCurrent&&documentId.endsWith(AutoBackup.CURRENT)&&mode.contains('w'))throw java.io.FileNotFoundException("Simulated write failure");return ParcelFileDescriptor.open(file(documentId),ParcelFileDescriptor.parseMode(mode))}
    override fun call(method:String,arg:String?,extras:Bundle?):Bundle? {if(method=="fail-current"){failCurrent=true;return Bundle()};if(method=="allow-writes"){failCurrent=false;return Bundle()};return super.call(method,arg,extras)}
}
class BackupFolderActivity : Activity() {
    override fun onCreate(state:Bundle?) {super.onCreate(state);setResult(RESULT_OK,Intent().setData(Uri.parse("content://com.paydaytracker.backup.tests/tree/root")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION));finish()}
}
