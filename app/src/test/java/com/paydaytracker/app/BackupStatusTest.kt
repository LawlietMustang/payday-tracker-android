package com.paydaytracker.app

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupStatusTest {
    @Test fun previousSuccessDoesNotHideNewerUnsavedDataOrAnAutomaticFailure() {
        assertEquals("outdated",backupDisplayStatus("new","","off",false,"old"))
        assertEquals("pending",backupDisplayStatus("new","old","pending",true,"old"))
        assertEquals("saving",backupDisplayStatus("new","old","saving",true,"old"))
        assertEquals("error",backupDisplayStatus("new","old","error",true,"old"))
        // An undo following an automatic write failure must not claim that file is healthy.
        assertEquals("error",backupDisplayStatus("old","old","error",true,""))
    }
    @Test fun onlyAMatchingSuccessfulCopyIsGreen() {
        assertEquals("off",backupDisplayStatus("new","","off",false,""))
        assertEquals("saved",backupDisplayStatus("new","","off",false,"new"))
        assertEquals("saved",backupDisplayStatus("new","new","saved",true,""))
        assertEquals("saved",backupDisplayStatus("new","new","off",false,""))
        // A successful manual copy still protects the current data if auto-backup fails.
        assertEquals("saved",backupDisplayStatus("new","old","error",true,"new"))
        assertEquals("off",backupDisplayStatus("","","off",false,""))
    }
}
