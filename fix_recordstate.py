with open("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("class ChatViewModel : ViewModel() {", "enum class RecordState {\n    IDLE,\n    RECORDING,\n    LOCKED_RECORDING,\n    PREVIEWING,\n    CANCELING\n}\n\nclass ChatViewModel : ViewModel() {")
content = content.replace("private val _recordState = MutableStateFlow<String>(\"idle\")", "private val _recordState = MutableStateFlow(RecordState.IDLE)")
content = content.replace("val recordState: StateFlow<String> = _recordState.asStateFlow()", "val recordState: StateFlow<RecordState> = _recordState.asStateFlow()")

with open("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt", "w") as f:
    f.write(content)
