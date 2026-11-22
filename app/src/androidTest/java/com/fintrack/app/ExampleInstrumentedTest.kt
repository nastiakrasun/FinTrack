package com.fintrack.app

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fintrack.app.data.files.TransactionPdfReport
import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.TransactionType
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.fintrack.app", appContext.packageName)
    }

    @Test
    fun createsValidPdfReportOnAndroid() {
        val output = ByteArrayOutputStream()
        val transaction = Transaction(
            id = "pdf-test",
            description = "Groceries",
            amountMinorUnits = 12_345,
            type = TransactionType.EXPENSE,
            categoryId = "groceries",
            date = LocalDate.of(2026, 11, 19)
        )

        TransactionPdfReport.write(output, listOf(transaction), YearMonth.of(2026, 11))

        assertTrue(output.toByteArray().decodeToString().startsWith("%PDF"))
    }
}