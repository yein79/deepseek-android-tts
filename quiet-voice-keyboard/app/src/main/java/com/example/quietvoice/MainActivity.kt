package com.example.quietvoice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 10)
  val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28,36,28,28) }
  layout.addView(TextView(this).apply { text = "조용한 음성 키보드"; textSize = 24f })
  layout.addView(TextView(this).apply { text = "\n1. 마이크 권한 허용\n2. 키보드 활성화\n3. 기본 키보드로 선택\n4. 입력창에서 마이크 버튼을 누르고 말하세요.\n\n말하는 동안 인식된 문장이 입력창에 표시됩니다. 인식 품질과 아주 작은 목소리 지원 여부는 휴대폰의 음성 인식 서비스에 따라 달라집니다. AI 문장 교정 기능은 아직 포함되지 않았습니다."; textSize = 16f })
  layout.addView(Button(this).apply { text = "키보드 활성화"; setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) } })
  layout.addView(Button(this).apply { text = "기본 키보드 선택"; setOnClickListener { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() } })
  setContentView(layout)
 }
}
