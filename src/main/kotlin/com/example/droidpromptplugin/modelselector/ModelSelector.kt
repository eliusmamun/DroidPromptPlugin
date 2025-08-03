package com.example.droidpromptplugin.modelselector

import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JLabel
import java.awt.Dimension
import java.awt.FlowLayout

class ModelSelector(
    private val modelList: List<String>,
    private val onModelSelected: (String) -> Unit
) {
    val panel: JPanel
    private val comboBox: JComboBox<String>

    init {
        panel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        val label = JLabel("Model:")
        comboBox = JComboBox(modelList.toTypedArray())
        comboBox.preferredSize = Dimension(150, 28)

        comboBox.addActionListener {
            val selectedModel = comboBox.selectedItem as String
            onModelSelected(selectedModel)
        }

        panel.add(label)
        panel.add(comboBox)
    }

    fun getSelectedModel(): String {
        return comboBox.selectedItem as String
    }

    fun setSelectedModel(modelName: String) {
        comboBox.selectedItem = modelName
    }
}
