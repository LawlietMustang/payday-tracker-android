package com.paydaytracker.app

import com.paydaytracker.app.data.AppSettings
import com.paydaytracker.app.data.Expense
import com.paydaytracker.app.data.PayrollCalculator
import com.paydaytracker.app.data.SavingsGoal
import com.paydaytracker.app.data.SavingsLedgerEntry
import com.paydaytracker.app.data.Shift
import com.paydaytracker.app.data.Workplace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PayrollCalculatorTest {

    @Test
    fun testEasterDate() {
        assertEquals(LocalDate.of(2026, 4, 5), PayrollCalculator.easterDate(2026))
        assertEquals(LocalDate.of(2024, 3, 31), PayrollCalculator.easterDate(2024))
        assertEquals(LocalDate.of(2030, 4, 21), PayrollCalculator.easterDate(2030))
    }

    @Test
    fun testGermanHolidays() {
        val sachsen2026 = PayrollCalculator.holidays(2026, "Sachsen")
        // Buß- und Bettag: Wednesday before Nov 23
        assertTrue("Sachsen has Buß- und Bettag", sachsen2026.contains("2026-11-18"))
        assertFalse("Hessen does not have Buß- und Bettag", PayrollCalculator.holidays(2026, "Hessen").contains("2026-11-18"))

        val bayern2026 = PayrollCalculator.holidays(2026, "Bayern")
        assertTrue("Bayern has Heilige Drei Könige", bayern2026.contains("2026-01-06"))
        assertTrue("Bayern has Fronleichnam", bayern2026.contains("2026-06-04"))
        assertTrue("Bayern has Allerheiligen", bayern2026.contains("2026-11-01"))

        val berlin2026 = PayrollCalculator.holidays(2026, "Berlin")
        assertTrue("Berlin has Frauentag", berlin2026.contains("2026-03-08"))
        assertFalse("Berlin does not have Allerheiligen", berlin2026.contains("2026-11-01"))
    }

    @Test
    fun testShiftRangeAndNightMinutes() {
        val settings = AppSettings(
            nightStart = "22:00",
            nightEnd = "06:00"
        )
        // Overnight shift 22:00 -> 06:00 (8h = 480 min, 0 break)
        val shift1 = Shift(
            id = "s1",
            date = "2026-05-10",
            start = "22:00",
            end = "06:00",
            minutes = 480,
            breakMin = 0
        )
        val night1 = PayrollCalculator.nightMinutes(shift1, settings)
        assertEquals(480.0, night1, 0.001)

        // Shift 14:00 -> 23:00 (9h span, 1h in night window, 0 break)
        val shift2 = Shift(
            id = "s2",
            date = "2026-05-10",
            start = "14:00",
            end = "23:00",
            minutes = 540,
            breakMin = 0
        )
        val night2 = PayrollCalculator.nightMinutes(shift2, settings)
        assertEquals(60.0, night2, 0.001)
    }

    @Test
    fun testSundayAndHolidayMinutes() {
        val settings = AppSettings(
            state = "Bayern",
            currency = "EUR",
            taxMode = "germany"
        )
        // 2026-05-10 is a Sunday
        val sundayShift = Shift(
            id = "sun",
            date = "2026-05-10",
            start = "08:00",
            end = "16:00",
            minutes = 480
        )
        assertEquals(480.0, PayrollCalculator.specialMinutes(sundayShift, "sunday", settings), 0.001)
        assertEquals(0.0, PayrollCalculator.specialMinutes(sundayShift, "holiday", settings), 0.001)

        // 2026-05-01 is Tag der Arbeit (Friday)
        val holidayShift = Shift(
            id = "hol",
            date = "2026-05-01",
            start = "08:00",
            end = "16:00",
            minutes = 480
        )
        assertEquals(0.0, PayrollCalculator.specialMinutes(holidayShift, "sunday", settings), 0.001)
        assertEquals(480.0, PayrollCalculator.specialMinutes(holidayShift, "holiday", settings), 0.001)
    }

    @Test
    fun testCancelledShiftHasZeroGrossAndBonus() {
        val settings = AppSettings()
        val cancelledShift = Shift(
            id = "c1",
            date = "2026-05-10",
            start = "08:00",
            end = "16:00",
            minutes = 480,
            wage = 20.0,
            status = "cancelled",
            cancelledBy = "employer"
        )
        assertEquals(0.0, PayrollCalculator.grossEntry(cancelledShift, 20.0), 0.001)
        val bonus = PayrollCalculator.bonusForEntry(cancelledShift, settings, overtimeMinutes = 60)
        assertEquals(0.0, bonus.total, 0.001)
    }

    @Test
    fun testManualEstimate() {
        val settings = AppSettings(
            taxMode = "manual",
            deductionPercent = 20.0
        )
        val estimate = PayrollCalculator.estimate(1000.0, settings)
        assertEquals(200.0, estimate.manualDeduction!!, 0.001)
        assertEquals(800.0, estimate.net, 0.001)
        assertEquals(0.0, estimate.wageTax, 0.001)
    }

    @Test
    fun testGermanTaxEstimate2026() {
        val settings = AppSettings(
            taxMode = "germany",
            currency = "EUR",
            taxclass = "I",
            state = "Hessen",
            health = 2.9,
            childless = true,
            church = false
        )
        val gross = 3000.0
        val est = PayrollCalculator.estimate(gross, settings)

        // Social contributions:
        // Pension: 3000 * 0.093 = 279.0
        assertEquals(279.0, est.pension, 0.001)
        // Unemployment: 3000 * 0.013 = 39.0
        assertEquals(39.0, est.unemployment, 0.001)
        // Health: 3000 * (0.073 + 2.9 / 200.0) = 3000 * (0.073 + 0.0145) = 3000 * 0.0875 = 262.5
        assertEquals(262.5, est.health, 0.001)
        // Care childless: 3000 * 0.024 = 72.0
        assertEquals(72.0, est.care, 0.001)

        // Annual taxable: 3000 * 12 = 36000 - 12348 - 1230 = 22422
        // rate: 0.14 + (22422 / 70000) * 0.28 = 0.14 + 0.089688 = 0.229688
        // annualTax: 22422 * 0.229688 = 5150.064
        // wageTax: 5150.064 / 12 = 429.172
        assertEquals(429.172, est.wageTax, 0.01)

        // Net: gross - wageTax - soli - church - pension - unemployment - health - care
        val expectedNet = gross - est.wageTax - est.soli - est.church - est.pension - est.unemployment - est.health - est.care
        assertEquals(expectedNet, est.net, 0.001)
    }

    @Test
    fun testSummaryAndForecastExactFiguresFromOutstandingCjs() {
        // Reproduce exact setup from tests/outstanding.cjs
        val settings = AppSettings(
            autoCompletePlanned = false,
            taxMode = "manual",
            deductionPercent = 0.0,
            nightRate = 0.0,
            sundayRate = 0.0,
            holidayRate = 0.0,
            overtimeRate = 0.0,
            days = emptyList(),
            target = 160.0,
            theme = "light"
        )
        val workplaces = listOf(
            Workplace(id = "default", name = "First job", wage = 10.0),
            Workplace(id = "second", name = "Second job", wage = 20.0)
        )
        val shifts = listOf(
            Shift(id = "done", date = "2030-01-14", start = "08:00", end = "16:00", minutes = 480, wage = 10.0, workplaceId = "default", status = "completed", breakMin = 0),
            Shift(id = "plan", date = "2030-01-16", start = "09:00", end = "13:00", minutes = 240, wage = 20.0, workplaceId = "second", status = "planned", breakMin = 0),
            Shift(id = "cancel", date = "2030-01-17", start = "08:00", end = "16:00", minutes = 480, wage = 10.0, workplaceId = "default", status = "cancelled", breakMin = 0)
        )

        val selected = "2030-01"
        val s = PayrollCalculator.summary(
            monthKey = selected,
            shifts = shifts,
            settings = settings,
            workplaces = workplaces,
            filterWorkplaceId = "all"
        )

        // assert.deepEqual(totals,{s:160,minutes:720,worked:480,count:1,forecast:160,forecastMinutes:720,cutoff:80});
        assertEquals(160.0, s.gross, 0.001)
        assertEquals(720, s.minutes)
        assertEquals(480, s.workedMinutes)
        assertEquals(1, s.completed.size)

        // cutoff: summary(selected,'all',new Date('2030-01-15'),false).gross == 80
        val cutoffSummary = PayrollCalculator.summary(
            monthKey = selected,
            shifts = shifts,
            settings = settings,
            workplaces = workplaces,
            filterWorkplaceId = "all",
            cutoff = LocalDateTime.of(2030, 1, 15, 0, 0),
            includePlanned = false
        )
        assertEquals(80.0, cutoffSummary.gross, 0.001)

        // Forecast with days = []
        val now = LocalDate.of(2030, 1, 15)
        val f1 = PayrollCalculator.forecast(s, settings, selected, now)
        assertEquals(160.0, f1.gross, 0.001)
        assertEquals(720, f1.minutes)

        // Forecast with all days [0,1,2,3,4,5,6]
        // Jan16 is scheduled and Jan17 cancelled. Only the 14 remaining empty dates extrapolate.
        // assert.equal(projected.minutes,720+14*360);
        // assert.equal(projected.gross,160+14*80);
        val settingsWithDays = settings.copy(days = listOf(0, 1, 2, 3, 4, 5, 6))
        val projected = PayrollCalculator.forecast(s, settingsWithDays, selected, now)
        assertEquals(720 + 14 * 360, projected.minutes)
        assertEquals(160.0 + 14 * 80.0, projected.gross, 0.001)
    }

    @Test
    fun testSpendingProjectionFromPlanningCjs() {
        val expenses = listOf(
            Expense(id = "r", date = "2026-09-01", amount = 600.0, category = "rent", recurringId = "r"),
            Expense(id = "v", date = "2026-09-05", amount = 100.0, category = "groceries"),
            Expense(id = "s", date = "2026-09-25", amount = 50.0, category = "utilities", recurringId = "s")
        )
        val now = LocalDate.of(2026, 9, 10)
        val proj = PayrollCalculator.spendingProjection("2026-09", expenses, now)
        // assert.equal(projection.spent,700);assert.equal(projection.scheduled,50);assert.equal(projection.projected,950);
        assertEquals(700.0, proj.spent, 0.001)
        assertEquals(50.0, proj.scheduled, 0.001)
        assertEquals(950.0, proj.projected, 0.001)
    }

    @Test
    fun testNextSavingsDateFromRound2Cjs() {
        // assert.deepEqual(schedule,['2026-02-28','2026-03-31','2025-02-28','2027-01-05']);
        assertEquals("2026-02-28", PayrollCalculator.nextSavingsDate("2026-01-31", "monthly", 31))
        assertEquals("2026-03-31", PayrollCalculator.nextSavingsDate("2026-02-28", "monthly", 31))
        assertEquals("2025-02-28", PayrollCalculator.nextSavingsDate("2024-02-29", "yearly", 29))
        assertEquals("2027-01-05", PayrollCalculator.nextSavingsDate("2026-12-29", "weekly", 29))
    }

    @Test
    fun testSavingsReconciliationFromRound2Cjs() {
        val settings = AppSettings(
            taxMode = "manual",
            deductionPercent = 0.0,
            nightRate = 0.0,
            sundayRate = 0.0,
            holidayRate = 0.0,
            overtimeRate = 0.0
        )
        val shifts = listOf(
            Shift(id = "pay", workplaceId = "default", date = "2026-01-01", start = "08:00", end = "18:00", minutes = 600, breakMin = 0, wage = 20.0, status = "completed")
        )
        fun makeGoal(id: String, amount: Double) = SavingsGoal(
            id = id,
            name = id,
            target = 1000.0,
            saved = 0.0,
            auto = true,
            autoInterval = "monthly",
            autoAmount = amount,
            autoAnchor = 1,
            autoNext = "2026-02-01",
            autoEpoch = "epoch",
            due = ""
        )
        val goals = listOf(makeGoal("a", 150.0), makeGoal("b", 100.0))
        val ledger = emptyList<SavingsLedgerEntry>()
        val now = LocalDateTime.of(2026, 2, 2, 12, 0)

        val before = PayrollCalculator.savingsAvailable(now, shifts, emptyList(), emptyList(), goals, ledger, settings)
        assertEquals(20000L, before)

        val res1 = PayrollCalculator.reconcileSavings(now, shifts, emptyList(), emptyList(), goals, ledger, settings)
        val savedValues = res1.updatedGoals.associateBy { it.id }.let { listOf(it["a"]!!.saved, it["b"]!!.saved) }
        assertEquals(listOf(150.0, 0.0), savedValues)
        assertEquals(50.0, res1.updatedGoals.first { it.id == "b" }.autoOverdue, 0.001)
        assertEquals(1, res1.newLedgerEntries.size)

        // Idempotency: second run shouldn't change
        val res2 = PayrollCalculator.reconcileSavings(now, shifts, emptyList(), emptyList(), res1.updatedGoals, res1.newLedgerEntries, settings)
        assertFalse(res2.changed)

        // With 300 EUR expense and goal c with autoAmount=0
        val expenses = listOf(
            Expense(id = "bill", date = "2026-01-01", amount = 300.0, category = "rent")
        )
        val goalC = listOf(makeGoal("c", 0.0))
        val res3 = PayrollCalculator.reconcileSavings(now, shifts, expenses, emptyList(), goalC, emptyList(), settings)
        assertEquals(100.0, res3.updatedGoals[0].autoOverdue, 0.001)

        // With expenses empty again -> full 200 EUR saved for goal c
        val res4 = PayrollCalculator.reconcileSavings(now, shifts, emptyList(), emptyList(), goalC, emptyList(), settings)
        assertEquals(200.0, res4.updatedGoals[0].saved, 0.001)
    }

    @Test
    fun testIsoCalendarWeekFromPlanningCjs() {
        // assert.deepEqual(await page.evaluate(()=>[isoCalendarWeek(new Date(2021,0,1)),isoCalendarWeek(new Date(2024,11,30))]),[{year:2020,week:53},{year:2025,week:1}]);
        val w1 = PayrollCalculator.isoCalendarWeek(LocalDate.of(2021, 1, 1))
        assertEquals(2020, w1.first)
        assertEquals(53, w1.second)

        val w2 = PayrollCalculator.isoCalendarWeek(LocalDate.of(2024, 12, 30))
        assertEquals(2025, w2.first)
        assertEquals(1, w2.second)
    }
}
