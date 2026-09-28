package com.paydaytracker.app.data

import org.junit.Assert.*
import org.junit.Test

class ShiftCsvTest {
    @Test fun workplaceWageAndCancellationMatchExportedPay() {
        val settings = AppSettings(wage=15.0,currency="GBP",taxMode="manual",sundayRate=50.0)
        val places = listOf(Workplace("job","Shop, London",20.0))
        val sunday = Shift("a","2026-09-06","10:00","12:00",120,workplaceId="job")
        val cancelled = sunday.copy(id="b",status="cancelled",cancelledBy="employer")
        val csv = ShiftCsv.export("2026-09",listOf(sunday,cancelled),settings,places)
        val lines = csv.lines()
        assertTrue(lines[0].contains("Base_gross_GBP"))
        assertTrue(lines[1].contains("\"Shop, London\""))
        assertTrue(lines[1].contains("\"40.00\",\"0.00\",\"0.00\",\"20.00\",\"0.00\",\"60.00\""))
        assertTrue(lines[2].contains("\"0\",\"0.00\",\"0.00\",\"0.00\",\"0.00\",\"0.00\",\"0.00\",\"cancelled\",\"employer\""))
    }
}
