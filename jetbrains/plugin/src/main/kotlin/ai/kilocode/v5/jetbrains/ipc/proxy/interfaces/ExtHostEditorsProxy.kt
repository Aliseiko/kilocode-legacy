// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package ai.kilocode.v5.jetbrains.ipc.proxy.interfaces

import ai.kilocode.v5.jetbrains.editor.EditorPropertiesChangeData
import ai.kilocode.v5.jetbrains.editor.TextEditorDiffInformation

interface ExtHostEditorsProxy {
    fun acceptEditorPropertiesChanged(id: String, props: EditorPropertiesChangeData)
    fun acceptEditorPositionData(data: Map<String, Int>)
    fun acceptEditorDiffInformation(id: String, diffInformation: List<TextEditorDiffInformation>?)
}
