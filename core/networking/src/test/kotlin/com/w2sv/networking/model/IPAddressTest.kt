package com.w2sv.networking.model

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IPAddressTest {

    @Test
    fun `test fetchPublic`() {
        val httpClient = OkHttpClient()
        runTest {
            val v4FetchResult = IPAddress.fetchPublic(httpClient, IPAddress.Version.V4)
            val v6FetchResult = IPAddress.fetchPublic(httpClient, IPAddress.Version.V6)

            assertTrue(
                v4FetchResult.isSuccess || v6FetchResult.isSuccess,
                "Fetching of both IP versions failed with ${v4FetchResult.exceptionOrNull()} and ${v6FetchResult.exceptionOrNull()}"
            )
        }
    }

    @Test
    fun `test subnetMask`() {
        val httpClient = OkHttpClient()
        runTest {
            val v4FetchResult = IPAddress.fetchPublic(httpClient, IPAddress.Version.V4)
            val v6FetchResult = IPAddress.fetchPublic(httpClient, IPAddress.Version.V6)

            assertTrue(
                v4FetchResult.isSuccess || v6FetchResult.isSuccess,
                "Fetching of both IP versions failed with ${v4FetchResult.exceptionOrNull()} and ${v6FetchResult.exceptionOrNull()}"
            )
        }
    }

    @Test
    fun `subnetMask should return correct mask for common prefix lengths`() {
        val cases = listOf(
            8 to "255.0.0.0",
            16 to "255.255.0.0",
            24 to "255.255.255.0",
            32 to "255.255.255.255",
            0 to "0.0.0.0"
        )

        cases.forEach { (prefixLength, expectedMask) ->
            assertEquals(expectedMask, dummyIPAddress(prefixLength).subnetMask)
        }
    }

    @Test
    fun `subnetMask should handle non-standard prefix lengths`() {
        val cases = listOf(
            10 to "255.192.0.0",
            18 to "255.255.192.0",
            26 to "255.255.255.192",
            30 to "255.255.255.252"
        )

        cases.forEach { (prefixLength, expectedMask) ->
            assertEquals(expectedMask, dummyIPAddress(prefixLength).subnetMask)
        }
    }

    @Test
    fun `subnetMask should throw an exception for prefix length greater than 32`() {
        assertThrows<IllegalArgumentException> {
            dummyIPAddress(33).subnetMask
        }
    }
}

private fun dummyIPAddress(prefixLength: Int): IPAddress =
    IPAddress(
        prefixLength = prefixLength,
        hostAddress = "10.0.0.1",
        isLinkLocal = false,
        isSiteLocal = false,
        isAnyLocal = false,
        isLoopback = false,
        isMulticast = false
    )
