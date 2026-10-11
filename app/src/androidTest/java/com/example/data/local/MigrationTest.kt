package com.example.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.data.local.SuiteDatabase.MIGRATION_1_2
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class MigrationTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        // Create database at version 1
        var db = helper.createDatabase("test_db", 1)
        db.close()

        // Migrate to version 2
        db = helper.runMigrationsAndValidate("test_db", 2, true, MIGRATION_1_2)
        db.close()
    }
}
