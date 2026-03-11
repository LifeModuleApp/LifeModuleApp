/*
 * LifeModule — A modular, privacy-focused life tracking app for Android.
 * Copyright (C) 2026 Paul Bernhard Colin Witzke
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.lifemodule.app.di

import android.content.Context
import timber.log.Timber
import androidx.room.Room
import de.lifemodule.app.data.DatabaseKeyManager
import de.lifemodule.app.data.LifeModuleDatabase
import de.lifemodule.app.data.prebuilt.PrebuiltDatabase
import de.lifemodule.app.data.prebuilt.PrebuiltExerciseDao
import de.lifemodule.app.data.prebuilt.PrebuiltFoodDao
import javax.inject.Named
import de.lifemodule.app.data.MIGRATION_12_13
import de.lifemodule.app.data.MIGRATION_13_14
import de.lifemodule.app.data.MIGRATION_14_15
import de.lifemodule.app.data.MIGRATION_15_16
import de.lifemodule.app.data.MIGRATION_16_17
import de.lifemodule.app.data.MIGRATION_17_18
import de.lifemodule.app.data.MIGRATION_18_19
import de.lifemodule.app.data.MIGRATION_19_20
import de.lifemodule.app.data.MIGRATION_20_21
import de.lifemodule.app.data.MIGRATION_21_22
import de.lifemodule.app.data.analytics.ActivityLogDao
import de.lifemodule.app.data.calendar.CalendarDao
import de.lifemodule.app.data.error.ErrorLogDao
import de.lifemodule.app.data.gym.GymDao
import de.lifemodule.app.data.habits.HabitDao
import de.lifemodule.app.data.mentalhealth.MoodDao
import de.lifemodule.app.data.nutrition.FoodDao
import de.lifemodule.app.data.schedule.CourseDao
import de.lifemodule.app.data.shopping.ShoppingDao
import de.lifemodule.app.data.supplements.SupplementDao
import de.lifemodule.app.data.weight.WeightDao
import de.lifemodule.app.data.worktime.WorkTimeDao
import de.lifemodule.app.data.logbook.LogbookDao
import de.lifemodule.app.data.scanner.ReceiptDao
import de.lifemodule.app.data.recipes.RecipeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DB_NAME = "lifemodule_database"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LifeModuleDatabase {
        // Migrate existing plaintext database to encrypted on first run
        if (DatabaseKeyManager.needsMigrationFromUnencrypted(context)) {
            migrateToEncrypted(context)
        }

        val passphrase = DatabaseKeyManager.getHexPassphrase(context)
        val factory = SupportFactory(passphrase)

        // Safety check: if a Keystore failure caused a new passphrase to be generated,
        // the old encrypted DB is inaccessible. Delete it so Room can start fresh.
        val dbFile = context.getDatabasePath(DB_NAME)
        if (dbFile.exists()) {
            try {
                net.sqlcipher.database.SQLiteDatabase.loadLibs(context)
                DatabaseKeyManager.withHexPassphraseString(context) { hexKey ->
                    val testDb = net.sqlcipher.database.SQLiteDatabase.openDatabase(
                        dbFile.absolutePath, hexKey, null,
                        net.sqlcipher.database.SQLiteDatabase.OPEN_READONLY
                    )
                    testDb.close()
                }
            } catch (e: Exception) {
                // Cannot open DB with current key - orphaned from device transfer.
                // Rename it so Room starts fresh, but keep it just in case the user finds their key.
                Timber.w(e, "Existing DB cannot be opened with current key. Renaming orphaned database.")
                val renamed = dbFile.renameTo(File(dbFile.path + ".corrupted_${System.currentTimeMillis()}"))
                if (!renamed) {
                    dbFile.delete()
                }
                File(dbFile.path + "-wal").delete()
                File(dbFile.path + "-shm").delete()

                // Flag for the UI so it can show a warning to the user
                context.getSharedPreferences("lifemodule_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("db_lost_on_startup", true)
                    .apply()
            }
        }

        return Room.databaseBuilder(
            context,
            LifeModuleDatabase::class.java,
            DB_NAME
        )
            .openHelperFactory(factory)
            // Proper migrations - preserves all user data
            .addMigrations(MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22)
            // Safety net for very old installs (v1-v11) that predate proper migrations.
            // These versions had no user data worth preserving.
            .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
            // Prevent crash when user downgrades to an older APK version.
            // Data is lost, but the app remains usable instead of boot-looping.
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    /**
     * One-time migration: encrypts an existing plaintext database using SQLCipher.
     * 1. Opens the plaintext DB.
     * 2. Attaches a new encrypted copy.
     * 3. Exports all data via `sqlcipher_export`.
     * 4. Replaces the original file.
     */
    private fun migrateToEncrypted(context: Context) {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            val tempFile = File(dbFile.parent, "${DB_NAME}_encrypted.tmp")

            // Load native SQLCipher libraries
            net.sqlcipher.database.SQLiteDatabase.loadLibs(context)

            // Open unencrypted database
            DatabaseKeyManager.withHexPassphraseString(context) { hexKey ->
                val plainDb = net.sqlcipher.database.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath, "", null,
                    net.sqlcipher.database.SQLiteDatabase.OPEN_READWRITE
                )

                // Attach encrypted database and export (text key -> PBKDF2, same as SupportFactory)
                plainDb.rawExecSQL("ATTACH DATABASE '${tempFile.absolutePath}' AS encrypted KEY '$hexKey'")
                plainDb.rawExecSQL("SELECT sqlcipher_export('encrypted')")
                plainDb.rawExecSQL("DETACH DATABASE encrypted")
                plainDb.close()
            }

            // Replace original with encrypted version
            dbFile.delete()
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            tempFile.renameTo(dbFile)

            Timber.i("Successfully migrated database to encrypted format")
        } catch (e: Exception) {
            Timber.e(e, "Failed to migrate database to encrypted format")
            // If migration fails, the unencrypted DB stays - we'll try again next launch
        }
    }

    @Provides
    fun provideFoodDao(database: LifeModuleDatabase): FoodDao {
        return database.foodDao()
    }

    @Provides
    fun provideSupplementDao(database: LifeModuleDatabase): SupplementDao {
        return database.supplementDao()
    }

    @Provides
    fun provideHabitDao(database: LifeModuleDatabase): HabitDao {
        return database.habitDao()
    }

    @Provides
    fun provideMoodDao(database: LifeModuleDatabase): MoodDao {
        return database.moodDao()
    }

    @Provides
    fun provideCourseDao(database: LifeModuleDatabase): CourseDao {
        return database.courseDao()
    }

    @Provides
    fun provideCalendarDao(database: LifeModuleDatabase): CalendarDao {
        return database.calendarDao()
    }

    @Provides
    fun provideGymDao(database: LifeModuleDatabase): GymDao {
        return database.gymDao()
    }

    @Provides
    fun provideActivityLogDao(database: LifeModuleDatabase): ActivityLogDao {
        return database.activityLogDao()
    }

    @Provides
    fun provideWeightDao(database: LifeModuleDatabase): WeightDao {
        return database.weightDao()
    }

    @Provides
    fun provideShoppingDao(database: LifeModuleDatabase): ShoppingDao {
        return database.shoppingDao()
    }

    @Provides
    fun provideWorkTimeDao(database: LifeModuleDatabase): WorkTimeDao {
        return database.workTimeDao()
    }

    @Provides
    fun provideErrorLogDao(database: LifeModuleDatabase): ErrorLogDao {
        return database.errorLogDao()
    }

    @Provides
    fun provideLogbookDao(database: LifeModuleDatabase): LogbookDao {
        return database.logbookDao()
    }

    @Provides
    fun provideReceiptDao(database: LifeModuleDatabase): ReceiptDao {
        return database.receiptDao()
    }

    @Provides
    fun provideRecipeDao(database: LifeModuleDatabase): RecipeDao {
        return database.recipeDao()
    }

    // ── PrebuiltDatabase (read-only asset) ─────────────────────────────────

    @Provides
    @Singleton
    @Named("prebuilt")
    fun providePrebuiltDatabase(@ApplicationContext context: Context): PrebuiltDatabase =
        Room.databaseBuilder(context, PrebuiltDatabase::class.java, "prebuilt.db")
            .createFromAsset("databases/prebuilt.db")
            // Safe: prebuilt DB is read-only reference data; user data is never in this file.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePrebuiltExerciseDao(@Named("prebuilt") db: PrebuiltDatabase): PrebuiltExerciseDao =
        db.exerciseDao()

    @Provides
    fun providePrebuiltFoodDao(@Named("prebuilt") db: PrebuiltDatabase): PrebuiltFoodDao =
        db.foodDao()
}
