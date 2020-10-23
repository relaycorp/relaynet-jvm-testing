package tech.relaycorp.relaynet.testing.pdc

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistration
import tech.relaycorp.relaynet.messages.control.PrivateNodeRegistrationRequest
import tech.relaycorp.relaynet.testing.CertificationPath
import tech.relaycorp.relaynet.testing.KeyPairSet
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InMemoryPDCClientTest {
    @Nested
    inner class Close {
        @Test
        fun `Client should not be reported closed initially`() {
            val client = InMemoryPDCClient()

            assertFalse(client.wasClosed)
        }

        @Test
        fun `Client should be reported closed only when actually closed`() {
            val client = InMemoryPDCClient()

            client.close()

            assertTrue(client.wasClosed)
        }

        @Test
        fun `Client should not be closed if there are calls in the queue`() {
            val client = InMemoryPDCClient(
                PreRegisterNodeCall(
                    Result.success(
                        PrivateNodeRegistrationRequest(KeyPairSet.PUBLIC_GW.public, ByteArray(0))
                    )
                )
            )

            val exception = assertThrows<IllegalStateException> { client.close() }

            assertEquals("Client was closed with calls in the queue", exception.message)
            assertTrue(client.wasClosed)
        }
    }

    private val exception = Exception("Something went wrong")

    @Nested
    inner class PreRegisterNode :
        MethodCallTest<PreRegisterNodeArgs, PrivateNodeRegistrationRequest, PreRegisterNodeCall>(
            PreRegisterNodeCall::class.java,
            PrivateNodeRegistrationRequest(KeyPairSet.PUBLIC_GW.public, ByteArray(0)),
            { client -> client.preRegisterNode(KeyPairSet.PUBLIC_GW.public) },
            PreRegisterNodeArgs(KeyPairSet.PUBLIC_GW.public),
            PreRegisterNodeCall(Result.failure(exception)),
            RegisterNodeCall(Result.failure(exception))
        )

    val pnra = "the registration authorization".toByteArray()

    @Nested
    inner class RegisterNode :
        MethodCallTest<RegisterNodeArgs, PrivateNodeRegistration, RegisterNodeCall>(
            RegisterNodeCall::class.java,
            PrivateNodeRegistration(CertificationPath.PRIVATE_GW, CertificationPath.PUBLIC_GW),
            { client -> client.registerNode(pnra) },
            RegisterNodeArgs(pnra),
            RegisterNodeCall(Result.failure(exception)),
            PreRegisterNodeCall(Result.failure(exception))
        )
}
