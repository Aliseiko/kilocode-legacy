package ai.kilocode.v5.jetbrains.core

import ai.kilocode.v5.jetbrains.actors.MainThreadTerminalShellIntegrationShape
import ai.kilocode.v5.jetbrains.ipc.IMessagePassingProtocol
import ai.kilocode.v5.jetbrains.ipc.MessageListener
import ai.kilocode.v5.jetbrains.ipc.proxy.RPCProtocol
import com.intellij.openapi.Disposable
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceProxyRegistryTest {
    private class NoopMessagePassingProtocol : IMessagePassingProtocol {
        override fun send(buffer: ByteArray) {}
        override fun onMessage(listener: MessageListener): Disposable = Disposable {}
        override fun onDidDispose(listener: () -> Unit): Disposable = Disposable {}
        override suspend fun drain() {}
        override fun dispose() {}
    }

    @Test
    fun `proxy numeric IDs match bundled vscode 1_84_0 protocol and support late lazy registration`() {
        // Trigger base initialization (MainContext 1..71, then ExtHostContext 72..139)
        assertEquals(1, ServiceProxyRegistry.MainContext.MainThreadAuthentication.nid)
        assertEquals(21, ServiceProxyRegistry.MainContext.MainThreadErrors.nid)
        assertEquals(26, ServiceProxyRegistry.MainContext.MainThreadLogger.nid)
        assertEquals(38, ServiceProxyRegistry.MainContext.MainThreadWebviews.nid)
        assertEquals(40, ServiceProxyRegistry.MainContext.MainThreadWebviewViews.nid)
        assertEquals(71, ServiceProxyRegistry.MainContext.MainThreadIssueReporter.nid)

        assertEquals(72, ServiceProxyRegistry.ExtHostContext.ExtHostCommands.nid)
        assertEquals(92, ServiceProxyRegistry.ExtHostContext.ExtHostExtensionService.nid)
        assertEquals(94, ServiceProxyRegistry.ExtHostContext.ExtHostTerminalService.nid)
        assertEquals(100, ServiceProxyRegistry.ExtHostContext.ExtHostWebviews.nid)
        assertEquals(103, ServiceProxyRegistry.ExtHostContext.ExtHostWebviewViews.nid)
        assertEquals(139, ServiceProxyRegistry.ExtHostContext.ExtHostIssueReporter.nid)

        // Construct RPCProtocol before lazy proxy IDs (>= 140) are accessed, matching WebStorm startup order
        val rpcProtocol = RPCProtocol(NoopMessagePassingProtocol())
        try {
            assertTrue(ServiceProxyRegistry.MainContext.MainThreadTerminalShellIntegration.nid > 139)
            assertTrue(ServiceProxyRegistry.ExtHostContext.ExtHostTerminalShellIntegration.nid > 139)

            val registered = Proxy.newProxyInstance(
                MainThreadTerminalShellIntegrationShape::class.java.classLoader,
                arrayOf(MainThreadTerminalShellIntegrationShape::class.java),
            ) { _, _, _ -> null } as MainThreadTerminalShellIntegrationShape
            rpcProtocol.set(ServiceProxyRegistry.MainContext.MainThreadTerminalShellIntegration, registered)
            rpcProtocol.assertRegistered(listOf(ServiceProxyRegistry.MainContext.MainThreadTerminalShellIntegration))
            assertNotNull(rpcProtocol.getProxy(ServiceProxyRegistry.ExtHostContext.ExtHostTerminalShellIntegration))
        } finally {
            rpcProtocol.dispose()
        }
    }
}
