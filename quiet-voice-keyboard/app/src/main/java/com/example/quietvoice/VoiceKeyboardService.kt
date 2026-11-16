package com.example.quietvoice

import android.inputmethodservice.InputMethodService
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class VoiceKeyboardService : InputMethodService() {
 private var recognizer: SpeechRecognizer? = null
 private var listening = false
 private var voiceButton: Button? = null

 override fun onCreateInputView(): View {
  val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,8,8,8) }
  voiceButton = Button(this).apply { text = "🎙 음성 입력"; setOnClickListener { toggleRecognition() } }
  row.addView(voiceButton, LinearLayout.LayoutParams(0, 130, 1f))
  row.addView(Button(this).apply { text = "공백"; setOnClickListener { currentInputConnection?.commitText(" ", 1) } }, LinearLayout.LayoutParams(0, 130, 1f))
  row.addView(Button(this).apply { text = "⌫"; setOnClickListener { currentInputConnection?.deleteSurroundingText(1, 0) } }, LinearLayout.LayoutParams(0, 130, 1f))
  return row
 }

 private fun toggleRecognition() {
  if (listening) { recognizer?.stopListening(); return }
  if (!SpeechRecognizer.isRecognitionAvailable(this)) { Toast.makeText(this, "이 기기에서 음성 인식을 사용할 수 없습니다.", Toast.LENGTH_LONG).show(); return }
  if (recognizer == null) {
   recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
    sr.setRecognitionListener(object : RecognitionListener {
     override fun onReadyForSpeech(params: Bundle?) { listening = true; voiceButton?.text = "듣는 중… 탭하여 종료" }
     override fun onBeginningOfSpeech() {}
     override fun onRmsChanged(rmsdB: Float) {}
     override fun onBufferReceived(buffer: ByteArray?) {}
     override fun onEndOfSpeech() { voiceButton?.text = "처리 중…" }
     override fun onError(error: Int) { listening = false; currentInputConnection?.setComposingText("", 1); currentInputConnection?.finishComposingText(); voiceButton?.text = "🎙 다시 말하기" }
     override fun onResults(results: Bundle?) {
      val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
      currentInputConnection?.setComposingText("", 1)
      currentInputConnection?.finishComposingText()
      if (text.isNotBlank()) currentInputConnection?.commitText(text, 1)
      listening = false; voiceButton?.text = "🎙 음성 입력"
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
  }
  try { recognizer?.startListening(intent) } catch (_: Exception) { listening = false; Toast.makeText(this, "음성 인식을 시작하지 못했습니다.", Toast.LENGTH_SHORT).show() }
 }

 override fun onDestroy() { recognizer?.destroy(); recognizer = null; super.onDestroy() }
}
