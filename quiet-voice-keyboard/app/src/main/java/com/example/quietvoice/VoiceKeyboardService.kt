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
 private var voiceButton: Button? = null
 private val repeatHandler = Handler(Looper.getMainLooper())
 private val repeatDelete = object : Runnable {
  override fun run() {
   currentInputConnection?.deleteSurroundingText(1, 0)
   repeatHandler.postDelayed(this, 55L)
  }
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

 private fun correctText(source: String): String {
  var s = source.replace('\u00a0', ' ')
  s = s.replace(Regex("[ \\t]+"), " ")
  s = s.replace(Regex("([가-힣])\\1{2,}"), "$1$1")
  s = s.replace(Regex("\\b(\\w+)(?:\\s+\\1\\b)+"), "$1")
  // Conservative rule-based spelling/spacing cleanup, not AI semantic rewriting.
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
  return s
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
     } finally {
      connection.endBatchEdit()
     }
    }
   } else {
    // Fallback for editors that don't provide ExtractedText.
    val before = connection.getTextBeforeCursor(10000, 0)?.toString()
    val after = connection.getTextAfterCursor(10000, 0)?.toString()
    if (before != null && after != null) {
     val wholeFallback = before + after
     val corrected = correctText(wholeFallback)
     if (corrected != wholeFallback) {
      connection.beginBatchEdit()
      try {
       connection.deleteSurroundingText(before.length, after.length)
       connection.commitText(corrected, 1)
      } finally {
       connection.endBatchEdit()
      }
     }
    }
   }
  } catch (_: Exception) {
   // Preserve Enter behavior even if an editor blocks text extraction.
  }
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
  if (listening) { recognizer?.stopListening(); return }
  if (!SpeechRecognizer.isRecognitionAvailable(this)) { Toast.makeText(this, "이 기기에서 음성 인식을 사용할 수 없습니다.", Toast.LENGTH_LONG).show(); return }
  if (recognizer == null) {
   recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
    sr.setRecognitionListener(object : RecognitionListener {
     override fun onReadyForSpeech(params: Bundle?) { listening = true; voiceButton?.text = "듣는 중…" }
     override fun onBeginningOfSpeech() {}
     override fun onRmsChanged(rmsdB: Float) {}
     override fun onBufferReceived(buffer: ByteArray?) {}
     override fun onEndOfSpeech() { voiceButton?.text = "처리 중…" }
     override fun onError(error: Int) { listening = false; currentInputConnection?.setComposingText("", 1); currentInputConnection?.finishComposingText(); voiceButton?.text = "🎙 다시" }
     override fun onResults(results: Bundle?) {
      val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
      currentInputConnection?.setComposingText("", 1)
      currentInputConnection?.finishComposingText()
      if (text.isNotBlank()) currentInputConnection?.commitText(text, 1)
      listening = false; voiceButton?.text = "🎙 음성"
     }
     override fun onPartialResults(partialResults: Bundle?) {
      val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
      if (text.isNotBlank()) currentInputConnection?.setComposingText(text, 1)
     }
     override fun onEvent(eventType: Int, params: Bundle?) {}
    })
   }
  }
  val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
   putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
   putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
   putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
   putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
   putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
   putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
   putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
  }
  try { recognizer?.startListening(intent) } catch (_: Exception) { listening = false; Toast.makeText(this, "음성 인식을 시작하지 못했습니다.", Toast.LENGTH_SHORT).show() }
 }

 override fun onDestroy() {
  repeatHandler.removeCallbacks(repeatDelete)
  recognizer?.destroy()
  recognizer = null
  super.onDestroy()
 }
}
