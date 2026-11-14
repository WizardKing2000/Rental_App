package com.example.ra

import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Uses local Firebase emulators only. No real users or messages are created. */
@RunWith(AndroidJUnit4::class)
class AccountSessionIntegrationTest {
    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = runCatching(block) }
        return result!!.getOrThrow()
    }

    private fun awaitIdle(session: AccountSession) {
        val deadline = android.os.SystemClock.elapsedRealtime() + 35_000
        while (onMain { session.busy } && android.os.SystemClock.elapsedRealtime() < deadline)
            android.os.SystemClock.sleep(50)
        assertFalse("Account operation timed out", onMain { session.busy })
    }

    @Test fun registrationRoleIsolationLoginAndRestoration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
            .setApplicationId("1:123456789:android:account-tests")
            .setApiKey("fake-emulator-key")
            .setProjectId("demo-yaadride-accounts")
            .setDatabaseUrl("https://demo-yaadride-accounts-default-rtdb.firebaseio.com")
            .build(), "account-test-" + UUID.randomUUID())
        val auth = FirebaseAuth.getInstance(app).apply { useEmulator("10.0.2.2", 9099) }
        val database = FirebaseDatabase.getInstance(app).apply { useEmulator("10.0.2.2", 9000) }
        val viewModels = ViewModelStore()
        val session = onMain {
            AccountSession(auth, database.getReference("accountProfiles")).also { viewModels.put("session", it) }
        }
        val id = UUID.randomUUID().toString()
        val ownerEmail = "owner-$id@yaadride.test"
        val renterEmail = "renter-$id@yaadride.test"
        val password = "Pass12345!"
        try {
            awaitIdle(session)
            onMain { session.register(AccountRole.OWNER, "Test Owner", ownerEmail, password) }
            awaitIdle(session)
            val owner = onMain { session.account }
            assertNotNull(onMain { session.error }, owner)
            assertEquals(AccountRole.OWNER, owner!!.role)

            onMain { session.signOut() }
            assertNull(onMain { session.account })
            onMain { session.register(AccountRole.RENTER, "Test Renter", renterEmail, password) }
            awaitIdle(session)
            val renter = onMain { session.account }
            assertNotNull(onMain { session.error }, renter)
            assertEquals(AccountRole.RENTER, renter!!.role)
            assertNotEquals(owner.id, renter.id)

            onMain { session.signOut(); session.login(AccountRole.OWNER, renterEmail, password) }
            awaitIdle(session)
            assertNull(onMain { session.account })
            assertTrue(onMain { session.error.orEmpty().contains("renter login") })

            onMain { session.login(AccountRole.RENTER, renterEmail, password) }
            awaitIdle(session)
            assertEquals(renter.id, onMain { session.account?.id })
            val restored = onMain {
                AccountSession(auth, database.getReference("accountProfiles")).also { viewModels.put("restored", it) }
            }
            awaitIdle(restored)
            assertEquals(renter.id, onMain { restored.account?.id })
            assertEquals(AccountRole.RENTER, onMain { restored.account?.role })
        } finally {
            onMain { viewModels.clear(); auth.signOut() }
            database.goOffline()
            app.delete()
        }
    }
}
