package com.example.quietvoice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28,36,28,28) }
  layout.addView(TextView(this).apply { text = "조용한 음성 키보드"; textSize = 24f })
  layout.addView(TextView(this).apply { text = "\n1. 키보드 활성화\n2. 기본 키보드로 선택\n3. 입력창에서 마이크 버튼을 눌러 말하세요.\n\n음성 인식은 기기에 설치된 음성 인식 서비스에 따라 작동합니다. 현재 버전은 AI 문장 교정 기능을 포함하지 않습니다."; textSize = 16f })
  layout.addView(Button(this).apply { text = "키보드 활성화"; setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) } })
  layout.addView(Button(this).apply { text = "기본 키보드 선택"; setOnClickListener { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() } })
  setContentView(layout)
 }
}
