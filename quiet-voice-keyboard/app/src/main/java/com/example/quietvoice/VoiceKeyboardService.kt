package com.example.quietvoice

import android.inputmethodservice.InputMethodService
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class VoiceKeyboardService : InputMethodService() {
 private var recognizer: SpeechRecognizer? = null
 private var listening = false
 private var listeningRequested = false
 private var voiceButton: Button? = null
 private val repeatHandler = Handler(Looper.getMainLooper())
 private val repeatDelete = object : Runnable {
  override fun run() {
   currentInputConnection?.deleteSurroundingText(1, 0)
   repeatHandler.postDelayed(this, 55L)
  }
 }
 private val restartListening = Runnable {
  if (listeningRequested) startRecognitionSession()
 }

 override fun onCreateInputView(): View {
  val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(4,4,4,4) }
  voiceButton = Button(this).apply { text = "🎙 음성"; setOnClickListener { toggleRecognition() } }
  row.addView(voiceButton, LinearLayout.LayoutParams(0, 130, 1.5f))
  row.addView(Button(this).apply { text = "공백"; setOnClickListener { currentInputConnection?.commitText(" ", 1) } }, LinearLayout.LayoutParams(0, 130, 0.8f))
  val backspace = Button(this).apply {
   text = "⌫"
   setOnTouchListener { _, event ->
    when (event.actionMasked) {
     MotionEvent.ACTION_DOWN -> {
      repeatHandler.removeCallbacks(repeatDelete)
      currentInputConnection?.deleteSurroundingText(1, 0)
      repeatHandler.postDelayed(repeatDelete, 380L)
      true
     }
     MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
      repeatHandler.removeCallbacks(repeatDelete)
      true
     }
     else -> true
    }
   }
  }
  row.addView(backspace, LinearLayout.LayoutParams(0, 130, 0.7f))
  row.addView(Button(this).apply { text = "↵ 교정"; setOnClickListener { correctThenEnter() } }, LinearLayout.LayoutParams(0, 130, 1f))
  return row
 }

 private fun addPunctuation(source: String): String {
  val s = source.trim()
  if (s.isEmpty() || s.last() in listOf('.', ',', '!', '?', '。', '！', '？')) return s
  val questionEnding = Regex("(인가요|인가|나요|습니까|습니까요|습니까|습니까|는가요|는가|ㄹ까요|을까요|까요|할까|일까|니|냐|까|어|아|야|여)$")
  val questionWords = listOf("왜", "뭐", "무엇", "어떻게", "어디", "언제", "누구", "어느", "무슨", "몇", "얼마", "어떤", "어째서")
  val looksQuestion = questionEnding.containsMatchIn(s) || questionWords.any { s.startsWith(it) || s.contains(" $it") }
  if (looksQuestion) return "$s?"
  val exclamations = listOf("와", "우와", "아", "아이고", "세상에", "헐", "대박", "정말", "진짜", "너무 좋", "정말 좋", "멋지")
  val endingExclamation = Regex("(구나|군요|네요|네|다)$").containsMatchIn(s)
  if (exclamations.any { s.startsWith(it) || s.contains(" $it") } || (endingExclamation && (s.startsWith("정말") || s.startsWith("진짜") || s.startsWith("너무")))) return "$s!"
  return s
 }

 private fun correctText(source: String): String {
  var s = source.replace('\u00a0', ' ')
  s = s.replace(Regex("[ \\t]+"), " ")
  s = s.replace(Regex("([가-힣])\\1{2,}"), "$1$1")
  s = s.replace(Regex("\\b(\\w+)(?:\\s+\\1\\b)+"), "$1")
  val rules = listOf(
   "할수있다" to "할 수 있다", "할수없다" to "할 수 없다",
   "할수 있는" to "할 수 있는", "할수 없는" to "할 수 없는",
   "될수있다" to "될 수 있다", "될수없다" to "될 수 없다",
   "수밖에없다" to "수밖에 없다", "것같다" to "것 같다",
   "것같아요" to "것 같아요", "것같습니다" to "것 같습니다",
   "할것이다" to "할 것이다", "그런것" to "그런 것",
   "이런것" to "이런 것", "저런것" to "저런 것",
   "한것" to "한 것", "하는것" to "하는 것",
   "있는것" to "있는 것", "없는것" to "없는 것",
   "될것" to "될 것", "안돼요" to "안 돼요",
   "안됩니다" to "안 됩니다", "할수도" to "할 수도",
   "될수도" to "될 수도", "말할수" to "말할 수",
   "알수있다" to "알 수 있다", "알수없다" to "알 수 없다"
  )
  for ((wrong, right) in rules) s = s.replace(wrong, right)
  s = s.replace(Regex(" +([,.!?])"), "$1")
  s = s.replace(Regex("([,.!?])(?=[가-힣A-Za-z0-9])"), "$1 ")
  s = s.replace(Regex(" {2,}"), " ").trim()
  return addPunctuation(s)
 }

 private fun correctThenEnter() {
  val connection = currentInputConnection ?: return
  try {
   val extracted = connection.getExtractedText(ExtractedTextRequest(), 0)
   val whole = extracted?.text?.toString()
   if (!whole.isNullOrEmpty() && whole.length < 20000) {
    val corrected = correctText(whole)
    if (corrected != whole) {
     val start = extracted.startOffset.coerceAtLeast(0)
     connection.beginBatchEdit()
     try {
      connection.setSelection(start, start + whole.length)
      connection.commitText(corrected, 1)
     } finally { connection.endBatchEdit() }
    }
   } else {
    val before = connection.getTextBeforeCursor(10000, 0)?.toString()
    val after = connection.getTextAfterCursor(10000, 0)?.toString()
    if (before != null && after != null) {
     val all = before + after
     val corrected = correctText(all)
     if (corrected != all) {
      connection.beginBatchEdit()
      try {
       connection.deleteSurroundingText(before.length, after.length)
       connection.commitText(corrected, 1)
      } finally { connection.endBatchEdit() }
     }
    }
   }
  } catch (_: Exception) { }
  val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
  val handled = when (action) {
   EditorInfo.IME_ACTION_GO -> connection.performEditorAction(EditorInfo.IME_ACTION_GO)
   EditorInfo.IME_ACTION_SEARCH -> connection.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
   EditorInfo.IME_ACTION_SEND -> connection.performEditorAction(EditorInfo.IME_ACTION_SEND)
   EditorInfo.IME_ACTION_DONE -> connection.performEditorAction(EditorInfo.IME_ACTION_DONE)
   else -> false
  }
  if (!handled) {
   connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
   connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
  }
 }

 private fun toggleRecognition() {
  if (listeningRequested) {
   listeningRequested = false
   repeatHandler.removeCallbacks(restartListening)
   try { recognizer?.stopListening() } catch (_: Exception) { }
   listening = false
   currentInputConnection?.finishComposingText()
   voiceButton?.text = "🎙 음성"
   return
  }
  if (!SpeechRecognizer.isRecognitionAvailable(this)) {
   Toast.makeText(this, "이 기기에서 음성 인식을 사용할 수 없습니다.", Toast.LENGTH_LONG).show()
   return
  }
  listeningRequested = true
  voiceButton?.text = "듣는 중… (눌러 종료)"
  if (recognizer == null) {
   recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
    sr.setRecognitionListener(object : RecognitionListener {
     override fun onReadyForSpeech(params: Bundle?) { listening = true; voiceButton?.text = "듣는 중… (눌러 종료)" }
     override fun onBeginningOfSpeech() {}
     override fun onRmsChanged(rmsdB: Float) {}
     override fun onBufferReceived(buffer: ByteArray?) {}
     override fun onEndOfSpeech() { listening = false; voiceButton?.text = if (listeningRequested) "처리 중…" else "🎙 음성" }
     override fun onError(error: Int) {
      listening = false
      currentInputConnection?.finishComposingText()
      if (listeningRequested && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
       voiceButton?.text = "다시 듣는 중…"
       repeatHandler.removeCallbacks(restartListening)
       repeatHandler.postDelayed(restartListening, 300L)
      } else {
       listeningRequested = false
       voiceButton?.text = "🎙 다시"
      }
     }
     override fun onResults(results: Bundle?) {
      val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
      currentInputConnection?.finishComposingText()
      if (text.isNotBlank()) currentInputConnection?.commitText(addPunctuation(text), 1)
      listening = false
      if (listeningRequested) {
       voiceButton?.text = "다시 듣는 중…"
       repeatHandler.removeCallbacks(restartListening)
       repeatHandler.postDelayed(restartListening, 220L)
      } else voiceButton?.text = "🎙 음성"
     }
     override fun onPartialResults(partialResults: Bundle?) {
      val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
      if (text.isNotBlank()) currentInputConnection?.setComposingText(text, 1)
     }
     override fun onEvent(eventType: Int, params: Bundle?) {}
    })
   }
  }
  startRecognitionSession()
 }

 private fun startRecognitionSession() {
  if (!listeningRequested) return
  val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
   putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
   putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
   putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
   putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
   putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
   // Silence closes only the current recognition segment; listening restarts automatically.
   putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
   putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 800L)
  }
  try {
   recognizer?.startListening(intent)
  } catch (_: Exception) {
   listening = false
   repeatHandler.removeCallbacks(restartListening)
   repeatHandler.postDelayed(restartListening, 500L)
  }
 }

 override fun onDestroy() {
  listeningRequested = false
  repeatHandler.removeCallbacks(repeatDelete)
  repeatHandler.removeCallbacks(restartListening)
  recognizer?.destroy()
  recognizer = null
  super.onDestroy()
 }
}
