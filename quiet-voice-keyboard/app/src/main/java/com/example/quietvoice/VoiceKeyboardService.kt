package com.example.quietvoice

import android.inputmethodservice.InputMethodService
import android.content.Intent
import android.speech.RecognizerIntent
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class VoiceKeyboardService : InputMethodService() {
 override fun onCreateInputView(): View {
  val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,8,8,8) }
  row.addView(Button(this).apply { text = "🎙 음성 입력"; setOnClickListener { startVoiceRecognition() } }, LinearLayout.LayoutParams(0, 130, 1f))
  row.addView(Button(this).apply { text = "공백"; setOnClickListener { currentInputConnection?.commitText(" ", 1) } }, LinearLayout.LayoutParams(0, 130, 1f))
  row.addView(Button(this).apply { text = "⌫"; setOnClickListener { currentInputConnection?.deleteSurroundingText(1, 0) } }, LinearLayout.LayoutParams(0, 130, 1f))
  return row
 }
 private fun startVoiceRecognition() {
  val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
   putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
   putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
   putExtra(RecognizerIntent.EXTRA_PROMPT, "편하게 말씀하세요")
  }
  try { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
  catch (_: Exception) { Toast.makeText(this, "기기에서 음성 인식 서비스를 사용할 수 없습니다.", Toast.LENGTH_LONG).show() }
 }
}
