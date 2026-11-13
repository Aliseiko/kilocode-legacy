package ai.kilocode.v5.jetbrains.core

import ai.kilocode.v5.jetbrains.actors.MainThreadBulkEditsShape
import ai.kilocode.v5.jetbrains.actors.MainThreadClipboardShape
import ai.kilocode.v5.jetbrains.actors.MainThreadCommandsShape
import ai.kilocode.v5.jetbrains.actors.MainThreadConfigurationShape
import ai.kilocode.v5.jetbrains.actors.MainThreadConsoleShape
import ai.kilocode.v5.jetbrains.actors.MainThreadDebugServiceShape
import ai.kilocode.v5.jetbrains.actors.MainThreadDiaglogsShape
import ai.kilocode.v5.jetbrains.actors.MainThreadDocumentContentProvidersShape
import ai.kilocode.v5.jetbrains.actors.MainThreadDocumentsShape
import ai.kilocode.v5.jetbrains.actors.MainThreadEditorTabsShape
import ai.kilocode.v5.jetbrains.actors.MainThreadErrorsShape
import ai.kilocode.v5.jetbrains.actors.MainThreadExtensionServiceShape
import ai.kilocode.v5.jetbrains.actors.MainThreadFileSystemEventServiceShape
import ai.kilocode.v5.jetbrains.actors.MainThreadFileSystemShape
import ai.kilocode.v5.jetbrains.actors.MainThreadLanguageFeaturesShape
import ai.kilocode.v5.jetbrains.actors.MainThreadLanguageModelToolsShape
import ai.kilocode.v5.jetbrains.actors.MainThreadLoggerShape
import ai.kilocode.v5.jetbrains.actors.MainThreadMessageServiceShape
import ai.kilocode.v5.jetbrains.actors.MainThreadOutputServiceShape
import ai.kilocode.v5.jetbrains.actors.MainThreadSearchShape
import ai.kilocode.v5.jetbrains.actors.MainThreadSecretStateShape
import ai.kilocode.v5.jetbrains.actors.MainThreadStatusBarShape
import ai.kilocode.v5.jetbrains.actors.MainThreadStorageShape
import ai.kilocode.v5.jetbrains.actors.MainThreadTaskShape
import ai.kilocode.v5.jetbrains.actors.MainThreadTelemetryShape
import ai.kilocode.v5.jetbrains.actors.MainThreadTerminalServiceShape
import ai.kilocode.v5.jetbrains.actors.MainThreadTerminalShellIntegrationShape
import ai.kilocode.v5.jetbrains.actors.MainThreadTextEditorsShape
import ai.kilocode.v5.jetbrains.actors.MainThreadUrlsShape
import ai.kilocode.v5.jetbrains.actors.MainThreadWebviewViewsShape
import ai.kilocode.v5.jetbrains.actors.MainThreadWebviewsShape
import ai.kilocode.v5.jetbrains.actors.MainThreadWindowShape
import ai.kilocode.v5.jetbrains.ipc.proxy.createProxyIdentifier
import ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostCommandsProxy
import ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostConfigurationProxy
import ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostDocumentsAndEditorsProxy
import ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostDocumentsProxy
import ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostEditorTabsProxy
import ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostEditorsProxy
import ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostExtensionServiceProxy
import ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostFileSystemEventServiceProxy
import ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostTerminalServiceProxy
import ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostTerminalShellIntegrationProxy
import ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostWebviewViewsProxy
import ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostWebviewsProxy
import ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostWorkspaceProxy
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger

/**
 * Service proxy registry class, centrally manages registration of all service proxies
 */
@Service(Service.Level.PROJECT)
class ServiceProxyRegistry private constructor() {
    private val logger = Logger.getInstance(this::class.java)

//    companion object {
//        private val instance = ServiceProxyRegistry()
//
//        fun getInstance(): ServiceProxyRegistry {
//            return instance
//        }
//    }
//
    /**
     * Initialize and register all service proxies
     */
    fun initialize() {
        // Initialize all proxy identifiers
        initializeAllProxies()
    }

    /**
     * Initialize all proxy identifiers
     * Ensure all services are initialized when created
     */
    private fun initializeAllProxies() {
        logger.info("Initialize all proxy identifiers")

        // Main thread service proxies (must match bundled vscode 1.84.0 extHost.protocol.ts order)
        val mainThreadProxies = listOf(
            MainContext.MainThreadAuthentication,
            MainContext.MainThreadBulkEdits,
            MainContext.MainThreadChatProvider,
            MainContext.MainThreadChatAgents,
            MainContext.MainThreadChatAgents2,
            MainContext.MainThreadChatVariables,
            MainContext.MainThreadClipboard,
            MainContext.MainThreadCommands,
            MainContext.MainThreadComments,
            MainContext.MainThreadConfiguration,
            MainContext.MainThreadConsole,
            MainContext.MainThreadDebugService,
            MainContext.MainThreadDecorations,
            MainContext.MainThreadDiagnostics,
            MainContext.MainThreadDialogs,
            MainContext.MainThreadDocuments,
            MainContext.MainThreadDocumentContentProviders,
            MainContext.MainThreadTextEditors,
            MainContext.MainThreadEditorInsets,
            MainContext.MainThreadEditorTabs,
            MainContext.MainThreadErrors,
            MainContext.MainThreadTreeViews,
            MainContext.MainThreadDownloadService,
            MainContext.MainThreadLanguageFeatures,
            MainContext.MainThreadLanguages,
            MainContext.MainThreadLogger,
            MainContext.MainThreadMessageService,
            MainContext.MainThreadOutputService,
            MainContext.MainThreadProgress,
            MainContext.MainThreadQuickDiff,
            MainContext.MainThreadQuickOpen,
            MainContext.MainThreadStatusBar,
            MainContext.MainThreadSecretState,
            MainContext.MainThreadStorage,
            MainContext.MainThreadSpeech,
            MainContext.MainThreadTelemetry,
            MainContext.MainThreadTerminalService,
            MainContext.MainThreadWebviews,
            MainContext.MainThreadWebviewPanels,
            MainContext.MainThreadWebviewViews,
            MainContext.MainThreadCustomEditors,
            MainContext.MainThreadUrls,
            MainContext.MainThreadUriOpeners,
            MainContext.MainThreadProfileContentHandlers,
            MainContext.MainThreadWorkspace,
            MainContext.MainThreadFileSystem,
            MainContext.MainThreadFileSystemEventService,
            MainContext.MainThreadExtensionService,
            MainContext.MainThreadSCM,
            MainContext.MainThreadSearch,
            MainContext.MainThreadShare,
            MainContext.MainThreadTask,
            MainContext.MainThreadWindow,
            MainContext.MainThreadLabelService,
            MainContext.MainThreadNotebook,
            MainContext.MainThreadNotebookDocuments,
            MainContext.MainThreadNotebookEditors,
            MainContext.MainThreadNotebookKernels,
            MainContext.MainThreadNotebookRenderers,
            MainContext.MainThreadInteractive,
            MainContext.MainThreadChat,
            MainContext.MainThreadInlineChat,
            MainContext.MainThreadTheming,
            MainContext.MainThreadTunnelService,
            MainContext.MainThreadManagedSockets,
            MainContext.MainThreadTimeline,
            MainContext.MainThreadTesting,
            MainContext.MainThreadLocalization,
            MainContext.MainThreadAiRelatedInformation,
            MainContext.MainThreadAiEmbeddingVector,
            MainContext.MainThreadIssueReporter,
        )

        // Extension host service proxies (must match bundled vscode 1.84.0 extHost.protocol.ts order)
        val extHostProxies = listOf(
            ExtHostContext.ExtHostCommands,
            ExtHostContext.ExtHostConfiguration,
            ExtHostContext.ExtHostDiagnostics,
            ExtHostContext.ExtHostDebugService,
            ExtHostContext.ExtHostDecorations,
            ExtHostContext.ExtHostDocumentsAndEditors,
            ExtHostContext.ExtHostDocuments,
            ExtHostContext.ExtHostDocumentContentProviders,
            ExtHostContext.ExtHostDocumentSaveParticipant,
            ExtHostContext.ExtHostEditors,
            ExtHostContext.ExtHostTreeViews,
            ExtHostContext.ExtHostFileSystem,
            ExtHostContext.ExtHostFileSystemInfo,
            ExtHostContext.ExtHostFileSystemEventService,
            ExtHostContext.ExtHostLanguages,
            ExtHostContext.ExtHostLanguageFeatures,
            ExtHostContext.ExtHostQuickOpen,
            ExtHostContext.ExtHostQuickDiff,
            ExtHostContext.ExtHostStatusBar,
            ExtHostContext.ExtHostShare,
            ExtHostContext.ExtHostExtensionService,
            ExtHostContext.ExtHostLogLevelServiceShape,
            ExtHostContext.ExtHostTerminalService,
            ExtHostContext.ExtHostSCM,
            ExtHostContext.ExtHostSearch,
            ExtHostContext.ExtHostTask,
            ExtHostContext.ExtHostWorkspace,
            ExtHostContext.ExtHostWindow,
            ExtHostContext.ExtHostWebviews,
            ExtHostContext.ExtHostWebviewPanels,
            ExtHostContext.ExtHostCustomEditors,
            ExtHostContext.ExtHostWebviewViews,
            ExtHostContext.ExtHostEditorInsets,
            ExtHostContext.ExtHostEditorTabs,
            ExtHostContext.ExtHostProgress,
            ExtHostContext.ExtHostComments,
            ExtHostContext.ExtHostSecretState,
            ExtHostContext.ExtHostStorage,
            ExtHostContext.ExtHostUrls,
            ExtHostContext.ExtHostUriOpeners,
            ExtHostContext.ExtHostProfileContentHandlers,
            ExtHostContext.ExtHostOutputService,
            ExtHostContext.ExtHostLabelService,
            ExtHostContext.ExtHostNotebook,
            ExtHostContext.ExtHostNotebookDocuments,
            ExtHostContext.ExtHostNotebookEditors,
            ExtHostContext.ExtHostNotebookKernels,
            ExtHostContext.ExtHostNotebookRenderers,
            ExtHostContext.ExtHostNotebookDocumentSaveParticipant,
            ExtHostContext.ExtHostInteractive,
            ExtHostContext.ExtHostInlineChat,
            ExtHostContext.ExtHostChat,
            ExtHostContext.ExtHostChatAgents,
            ExtHostContext.ExtHostChatAgents2,
            ExtHostContext.ExtHostChatVariables,
            ExtHostContext.ExtHostChatProvider,
            ExtHostContext.ExtHostSpeech,
            ExtHostContext.ExtHostAiRelatedInformation,
            ExtHostContext.ExtHostAiEmbeddingVector,
            ExtHostContext.ExtHostTheming,
            ExtHostContext.ExtHostTunnelService,
            ExtHostContext.ExtHostManagedSockets,
            ExtHostContext.ExtHostAuthentication,
            ExtHostContext.ExtHostTimeline,
            ExtHostContext.ExtHostTesting,
            ExtHostContext.ExtHostTelemetry,
            ExtHostContext.ExtHostLocalization,
            ExtHostContext.ExtHostIssueReporter,
        )

        logger.info("Initialized ${mainThreadProxies.size} main thread services and ${extHostProxies.size} extension host services")
    }

    /**
     * Main thread context - Context ID enum values defined in VSCode 1.84.0
     */
    object MainContext {
        val MainThreadAuthentication = createProxyIdentifier<Any>("MainThreadAuthentication")
        val MainThreadBulkEdits = createProxyIdentifier<MainThreadBulkEditsShape>("MainThreadBulkEdits")
        val MainThreadChatProvider = createProxyIdentifier<Any>("MainThreadChatProvider")
        val MainThreadChatAgents = createProxyIdentifier<Any>("MainThreadChatAgents")
        val MainThreadChatAgents2 = createProxyIdentifier<Any>("MainThreadChatAgents2")
        val MainThreadChatVariables = createProxyIdentifier<Any>("MainThreadChatVariables")
        val MainThreadClipboard = createProxyIdentifier<MainThreadClipboardShape>("MainThreadClipboard")
        val MainThreadCommands = createProxyIdentifier<MainThreadCommandsShape>("MainThreadCommands")
        val MainThreadComments = createProxyIdentifier<Any>("MainThreadComments")
        val MainThreadConfiguration = createProxyIdentifier<MainThreadConfigurationShape>("MainThreadConfiguration")
        val MainThreadConsole = createProxyIdentifier<MainThreadConsoleShape>("MainThreadConsole")
        val MainThreadDebugService = createProxyIdentifier<MainThreadDebugServiceShape>("MainThreadDebugService")
        val MainThreadDecorations = createProxyIdentifier<Any>("MainThreadDecorations")
        val MainThreadDiagnostics = createProxyIdentifier<Any>("MainThreadDiagnostics")
        val MainThreadDialogs = createProxyIdentifier<MainThreadDiaglogsShape>("MainThreadDiaglogs")
        val MainThreadDocuments = createProxyIdentifier<MainThreadDocumentsShape>("MainThreadDocuments")
        val MainThreadDocumentContentProviders = createProxyIdentifier<MainThreadDocumentContentProvidersShape>("MainThreadDocumentContentProviders")
        val MainThreadTextEditors = createProxyIdentifier<MainThreadTextEditorsShape>("MainThreadTextEditors")
        val MainThreadEditorInsets = createProxyIdentifier<Any>("MainThreadEditorInsets")
        val MainThreadEditorTabs = createProxyIdentifier<MainThreadEditorTabsShape>("MainThreadEditorTabs")
        val MainThreadErrors = createProxyIdentifier<MainThreadErrorsShape>("MainThreadErrors")
        val MainThreadTreeViews = createProxyIdentifier<Any>("MainThreadTreeViews")
        val MainThreadDownloadService = createProxyIdentifier<Any>("MainThreadDownloadService")
        val MainThreadLanguageFeatures = createProxyIdentifier<MainThreadLanguageFeaturesShape>("MainThreadLanguageFeatures")
        val MainThreadLanguages = createProxyIdentifier<Any>("MainThreadLanguages")
        val MainThreadLogger = createProxyIdentifier<MainThreadLoggerShape>("MainThreadLogger")
        val MainThreadMessageService = createProxyIdentifier<MainThreadMessageServiceShape>("MainThreadMessageService")
        val MainThreadOutputService = createProxyIdentifier<MainThreadOutputServiceShape>("MainThreadOutputService")
        val MainThreadProgress = createProxyIdentifier<Any>("MainThreadProgress")
        val MainThreadQuickDiff = createProxyIdentifier<Any>("MainThreadQuickDiff")
        val MainThreadQuickOpen = createProxyIdentifier<Any>("MainThreadQuickOpen")
        val MainThreadStatusBar = createProxyIdentifier<MainThreadStatusBarShape>("MainThreadStatusBar")
        val MainThreadSecretState = createProxyIdentifier<MainThreadSecretStateShape>("MainThreadSecretState")
        val MainThreadStorage = createProxyIdentifier<MainThreadStorageShape>("MainThreadStorage")
        val MainThreadSpeech = createProxyIdentifier<Any>("MainThreadSpeechProvider")
        val MainThreadTelemetry = createProxyIdentifier<MainThreadTelemetryShape>("MainThreadTelemetry")
        val MainThreadTerminalService = createProxyIdentifier<MainThreadTerminalServiceShape>("MainThreadTerminalService")
        val MainThreadWebviews = createProxyIdentifier<MainThreadWebviewsShape>("MainThreadWebviews")
        val MainThreadWebviewPanels = createProxyIdentifier<Any>("MainThreadWebviewPanels")
        val MainThreadWebviewViews = createProxyIdentifier<MainThreadWebviewViewsShape>("MainThreadWebviewViews")
        val MainThreadCustomEditors = createProxyIdentifier<Any>("MainThreadCustomEditors")
        val MainThreadUrls = createProxyIdentifier<MainThreadUrlsShape>("MainThreadUrls")
        val MainThreadUriOpeners = createProxyIdentifier<Any>("MainThreadUriOpeners")
        val MainThreadProfileContentHandlers = createProxyIdentifier<Any>("MainThreadProfileContentHandlers")
        val MainThreadWorkspace = createProxyIdentifier<Any>("MainThreadWorkspace")
        val MainThreadFileSystem = createProxyIdentifier<MainThreadFileSystemShape>("MainThreadFileSystem")
        val MainThreadFileSystemEventService = createProxyIdentifier<MainThreadFileSystemEventServiceShape>("MainThreadFileSystemEventService")
        val MainThreadExtensionService = createProxyIdentifier<MainThreadExtensionServiceShape>("MainThreadExtensionService")
        val MainThreadSCM = createProxyIdentifier<Any>("MainThreadSCM")
        val MainThreadSearch = createProxyIdentifier<MainThreadSearchShape>("MainThreadSearch")
        val MainThreadShare = createProxyIdentifier<Any>("MainThreadShare")
        val MainThreadTask = createProxyIdentifier<MainThreadTaskShape>("MainThreadTask")
        val MainThreadWindow = createProxyIdentifier<MainThreadWindowShape>("MainThreadWindow")
        val MainThreadLabelService = createProxyIdentifier<Any>("MainThreadLabelService")
        val MainThreadNotebook = createProxyIdentifier<Any>("MainThreadNotebook")
        val MainThreadNotebookDocuments = createProxyIdentifier<Any>("MainThreadNotebookDocumentsShape")
        val MainThreadNotebookEditors = createProxyIdentifier<Any>("MainThreadNotebookEditorsShape")
        val MainThreadNotebookKernels = createProxyIdentifier<Any>("MainThreadNotebookKernels")
        val MainThreadNotebookRenderers = createProxyIdentifier<Any>("MainThreadNotebookRenderers")
        val MainThreadInteractive = createProxyIdentifier<Any>("MainThreadInteractive")
        val MainThreadChat = createProxyIdentifier<Any>("MainThreadChat")
        val MainThreadInlineChat = createProxyIdentifier<Any>("MainThreadInlineChatShape")
        val MainThreadTheming = createProxyIdentifier<Any>("MainThreadTheming")
        val MainThreadTunnelService = createProxyIdentifier<Any>("MainThreadTunnelService")
        val MainThreadManagedSockets = createProxyIdentifier<Any>("MainThreadManagedSockets")
        val MainThreadTimeline = createProxyIdentifier<Any>("MainThreadTimeline")
        val MainThreadTesting = createProxyIdentifier<Any>("MainThreadTesting")
        val MainThreadLocalization = createProxyIdentifier<Any>("MainThreadLocalizationShape")
        val MainThreadAiRelatedInformation = createProxyIdentifier<Any>("MainThreadAiRelatedInformation")
        val MainThreadAiEmbeddingVector = createProxyIdentifier<Any>("MainThreadAiEmbeddingVector")
        val MainThreadIssueReporter = createProxyIdentifier<Any>("MainThreadIssueReporter")

        // Lazy extras not in bundled vscode 1.84.0 extHost.protocol.ts so they do not shift wire proxy nids 1..139
        val MainThreadLanguageModels by lazy { createProxyIdentifier<Any>("MainThreadLanguageModels") }
        val MainThreadEmbeddings by lazy { createProxyIdentifier<Any>("MainThreadEmbeddings") }
        val MainThreadCodeMapper by lazy { createProxyIdentifier<Any>("MainThreadCodeMapper") }
        val MainThreadLanguageModelTools by lazy { createProxyIdentifier<MainThreadLanguageModelToolsShape>("MainThreadLanguageModelTools") }
        val MainThreadTerminalShellIntegration by lazy { createProxyIdentifier<MainThreadTerminalShellIntegrationShape>("MainThreadTerminalShellIntegration") }
        val MainThreadMcp by lazy { createProxyIdentifier<Any>("MainThreadMcpShape") }
        val MainThreadChatStatus by lazy { createProxyIdentifier<Any>("MainThreadChatStatus") }
    }

    /**
     * Extension host context - Extension host context ID enum values defined in VSCode 1.84.0
     */
    object ExtHostContext {
        init {
            // Ensure MainContext claims nids 1..71 before ExtHostContext claims nids 72..139
            MainContext.MainThreadAuthentication
        }

        val ExtHostCommands = createProxyIdentifier<ExtHostCommandsProxy>("ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostCommandsProxy")
        val ExtHostConfiguration = createProxyIdentifier<ExtHostConfigurationProxy>("ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostConfigurationProxy")
        val ExtHostDiagnostics = createProxyIdentifier<Any>("ExtHostDiagnostics")
        val ExtHostDebugService = createProxyIdentifier<Any>("ExtHostDebugService")
        val ExtHostDecorations = createProxyIdentifier<Any>("ExtHostDecorations")
        val ExtHostDocumentsAndEditors = createProxyIdentifier<ExtHostDocumentsAndEditorsProxy>("ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostDocumentsAndEditorsProxy")
        val ExtHostDocuments = createProxyIdentifier<ExtHostDocumentsProxy>("ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostDocumentsProxy")
        val ExtHostDocumentContentProviders = createProxyIdentifier<Any>("ExtHostDocumentContentProviders")
        val ExtHostDocumentSaveParticipant = createProxyIdentifier<Any>("ExtHostDocumentSaveParticipant")
        val ExtHostEditors = createProxyIdentifier<ExtHostEditorsProxy>("ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostEditorsProxy")
        val ExtHostTreeViews = createProxyIdentifier<Any>("ExtHostTreeViews")
        val ExtHostFileSystem = createProxyIdentifier<Any>("ExtHostFileSystem")
        val ExtHostFileSystemInfo = createProxyIdentifier<Any>("ExtHostFileSystemInfo")
        val ExtHostFileSystemEventService = createProxyIdentifier<ExtHostFileSystemEventServiceProxy>("ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostFileSystemEventServiceProxy")
        val ExtHostLanguages = createProxyIdentifier<Any>("ExtHostLanguages")
        val ExtHostLanguageFeatures = createProxyIdentifier<Any>("ExtHostLanguageFeatures")
        val ExtHostQuickOpen = createProxyIdentifier<Any>("ExtHostQuickOpen")
        val ExtHostQuickDiff = createProxyIdentifier<Any>("ExtHostQuickDiff")
        val ExtHostStatusBar = createProxyIdentifier<Any>("ExtHostStatusBar")
        val ExtHostShare = createProxyIdentifier<Any>("ExtHostShare")
        val ExtHostExtensionService = createProxyIdentifier<ExtHostExtensionServiceProxy>("ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostExtensionServiceProxy")
        val ExtHostLogLevelServiceShape = createProxyIdentifier<Any>("ExtHostLogLevelServiceShape")
        val ExtHostTerminalService = createProxyIdentifier<ExtHostTerminalServiceProxy>("ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostTerminalServiceProxy")
        val ExtHostSCM = createProxyIdentifier<Any>("ExtHostSCM")
        val ExtHostSearch = createProxyIdentifier<Any>("ExtHostSearch")
        val ExtHostTask = createProxyIdentifier<Any>("ExtHostTask")
        val ExtHostWorkspace = createProxyIdentifier<ExtHostWorkspaceProxy>("ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostWorkspaceProxy")
        val ExtHostWindow = createProxyIdentifier<Any>("ExtHostWindow")
        val ExtHostWebviews = createProxyIdentifier<ExtHostWebviewsProxy>("ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostWebviewsProxy")
        val ExtHostWebviewPanels = createProxyIdentifier<Any>("ExtHostWebviewPanels")
        val ExtHostCustomEditors = createProxyIdentifier<Any>("ExtHostCustomEditors")
        val ExtHostWebviewViews = createProxyIdentifier<ExtHostWebviewViewsProxy>("ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostWebviewViewsProxy")
        val ExtHostEditorInsets = createProxyIdentifier<Any>("ExtHostEditorInsets")
        val ExtHostEditorTabs = createProxyIdentifier<ExtHostEditorTabsProxy>("ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostEditorTabsProxy")
        val ExtHostProgress = createProxyIdentifier<Any>("ExtHostProgress")
        val ExtHostComments = createProxyIdentifier<Any>("ExtHostComments")
        val ExtHostSecretState = createProxyIdentifier<Any>("ExtHostSecretState")
        val ExtHostStorage = createProxyIdentifier<Any>("ExtHostStorage")
        val ExtHostUrls = createProxyIdentifier<Any>("ExtHostUrls")
        val ExtHostUriOpeners = createProxyIdentifier<Any>("ExtHostUriOpeners")
        val ExtHostProfileContentHandlers = createProxyIdentifier<Any>("ExtHostProfileContentHandlers")
        val ExtHostOutputService = createProxyIdentifier<Any>("ExtHostOutputService")
        val ExtHostLabelService = createProxyIdentifier<Any>("ExtHostLabelService")
        val ExtHostNotebook = createProxyIdentifier<Any>("ExtHostNotebook")
        val ExtHostNotebookDocuments = createProxyIdentifier<Any>("ExtHostNotebookDocuments")
        val ExtHostNotebookEditors = createProxyIdentifier<Any>("ExtHostNotebookEditors")
        val ExtHostNotebookKernels = createProxyIdentifier<Any>("ExtHostNotebookKernels")
        val ExtHostNotebookRenderers = createProxyIdentifier<Any>("ExtHostNotebookRenderers")
        val ExtHostNotebookDocumentSaveParticipant = createProxyIdentifier<Any>("ExtHostNotebookDocumentSaveParticipant")
        val ExtHostInteractive = createProxyIdentifier<Any>("ExtHostInteractive")
        val ExtHostInlineChat = createProxyIdentifier<Any>("ExtHostInlineChat")
        val ExtHostChat = createProxyIdentifier<Any>("ExtHostChat")
        val ExtHostChatAgents = createProxyIdentifier<Any>("ExtHostChatAgents")
        val ExtHostChatAgents2 = createProxyIdentifier<Any>("ExtHostChatAgents2")
        val ExtHostChatVariables = createProxyIdentifier<Any>("ExtHostChatVariables")
        val ExtHostChatProvider = createProxyIdentifier<Any>("ExtHostChatProvider")
        val ExtHostSpeech = createProxyIdentifier<Any>("ExtHostSpeech")
        val ExtHostAiRelatedInformation = createProxyIdentifier<Any>("ExtHostAiRelatedInformation")
        val ExtHostAiEmbeddingVector = createProxyIdentifier<Any>("ExtHostAiEmbeddingVector")
        val ExtHostTheming = createProxyIdentifier<Any>("ExtHostTheming")
        val ExtHostTunnelService = createProxyIdentifier<Any>("ExtHostTunnelService")
        val ExtHostManagedSockets = createProxyIdentifier<Any>("ExtHostManagedSockets")
        val ExtHostAuthentication = createProxyIdentifier<Any>("ExtHostAuthentication")
        val ExtHostTimeline = createProxyIdentifier<Any>("ExtHostTimeline")
        val ExtHostTesting = createProxyIdentifier<Any>("ExtHostTesting")
        val ExtHostTelemetry = createProxyIdentifier<Any>("ExtHostTelemetry")
        val ExtHostLocalization = createProxyIdentifier<Any>("ExtHostLocalization")
        val ExtHostIssueReporter = createProxyIdentifier<Any>("ExtHostIssueReporter")

        // Lazy extras not in bundled vscode 1.84.0 extHost.protocol.ts so they do not shift wire proxy nids 1..139
        val ExtHostCodeMapper by lazy { createProxyIdentifier<Any>("ExtHostCodeMapper") }
        val ExtHostTerminalShellIntegration by lazy { createProxyIdentifier<ExtHostTerminalShellIntegrationProxy>("ai.kilocode.v5.jetbrains.ipc.proxy.interfaces.ExtHostTerminalShellIntegrationProxy") }
        val ExtHostLanguageModelTools by lazy { createProxyIdentifier<Any>("ExtHostChatSkills") }
        val ExtHostEmbeddings by lazy { createProxyIdentifier<Any>("ExtHostEmbeddings") }
        val ExtHostMcp by lazy { createProxyIdentifier<Any>("ExtHostMcp") }
    }
}
