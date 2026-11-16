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
  layout.addView(TextView(this).apply { text = "\n1. 마이크 권한 허용\n2. 키보드 활성화\n3. 기본 키보드로 선택\n4. 마이크 버튼을 누르고 말하세요.\n5. 입력을 마칠 때 ‘↵ 교정’을 누르세요.\n\n‘↵ 교정’은 입력창에 보이는 글의 띄어쓰기와 자주 발생하는 음성 인식 오류를 보수적으로 정리한 뒤 엔터를 입력합니다. 말투를 바꾸는 문장 재작성은 하지 않습니다.\n\n속삭임 인식은 휴대폰의 음성 인식 엔진과 마이크 성능에 좌우됩니다. 조용한 환경에서 마이크에 조금 더 가까이 말해 보세요."; textSize = 16f })
  layout.addView(Button(this).apply { text = "키보드 활성화"; setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) } })
  layout.addView(Button(this).apply { text = "기본 키보드 선택"; setOnClickListener { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() } })
  setContentView(layout)
 }
}
