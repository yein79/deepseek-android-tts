package org.example.deepseektts;

import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DeepSeekTtsService extends TextToSpeechService {
    private volatile boolean stopped = false;

    @Override public int onIsLanguageAvailable(String lang, String country, String variant) {
        return ("zh".equalsIgnoreCase(lang) || "en".equalsIgnoreCase(lang) || "ko".equalsIgnoreCase(lang))
            ? TextToSpeech.LANG_AVAILABLE : TextToSpeech.LANG_NOT_SUPPORTED;
    }
    @Override public int onLoadLanguage(String lang, String country, String variant) {
        return onIsLanguageAvailable(lang, country, variant);
    }
    @Override public String[] onGetLanguage() { return new String[]{"zh", "CN", ""}; }
    @Override public void onStop() { stopped = true; }

    @Override protected void onSynthesizeText(SynthesisRequest request, SynthesisCallback callback) {
        stopped = false;
        try {
            SharedPreferences prefs = getSharedPreferences("engine", MODE_PRIVATE);
            String base = prefs.getString("endpoint", "http://127.0.0.1:3080").replaceAll("/+$", "");
            String voice = prefs.getString("voice", "mira");
            HttpURLConnection conn = (HttpURLConnection) new URL(base + "/api/ds-tts/synthesize").openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(120000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            JSONObject body = new JSONObject();
            body.put("text", request.getCharSequenceText().toString());
            body.put("voice", voice);
            body.put("format", "wav");
            try (OutputStream out = conn.getOutputStream()) {
                out.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
            int status = conn.getResponseCode();
            InputStream response = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
            byte[] responseBytes = readAll(response);
            conn.disconnect();
            if (status < 200 || status >= 300) throw new IllegalStateException("Service HTTP " + status);
            JSONObject result = new JSONObject(new String(responseBytes, StandardCharsets.UTF_8));
            String audioUrl = result.optString("url", "");
            if (!result.optBoolean("ok", true) || audioUrl.isEmpty())
                throw new IllegalStateException("Service returned no audio URL");

            HttpURLConnection audioConn = (HttpURLConnection) new URL(audioUrl).openConnection();
            audioConn.setConnectTimeout(15000);
            audioConn.setReadTimeout(120000);
            byte[] wav;
            try (InputStream in = audioConn.getInputStream()) { wav = readAll(in); }
            finally { audioConn.disconnect(); }
            byte[] pcm = extractPcm(wav);
            if (stopped) { callback.done(); return; }
            callback.start(24000, AudioFormat.ENCODING_PCM_16BIT, 1);
            for (int offset = 0; offset < pcm.length && !stopped;) {
                int count = Math.min(8192, pcm.length - offset);
                count -= count % 2;
                if (count <= 0) break;
                callback.audioAvailable(pcm, offset, count);
                offset += count;
            }
            callback.done();
        } catch (Exception e) {
            callback.error("DeepSeek TTS prototype: " + e.getMessage());
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        if (in == null) throw new IllegalStateException("Empty HTTP response");
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = input.read(buffer)) != -1) out.write(buffer, 0, n);
            return out.toByteArray();
        }
    }

    private static byte[] extractPcm(byte[] wav) {
        if (wav.length < 44 || wav[0] != 'R' || wav[1] != 'I' || wav[2] != 'F' || wav[3] != 'F')
            throw new IllegalStateException("Audio is not WAV");
        int pos = 12;
        while (pos + 8 <= wav.length) {
            String chunk = new String(wav, pos, 4, StandardCharsets.US_ASCII);
            int size = (wav[pos+4] & 255) | ((wav[pos+5] & 255) << 8)
                | ((wav[pos+6] & 255) << 16) | ((wav[pos+7] & 255) << 24);
            if ("data".equals(chunk)) {
                int start = pos + 8, end = Math.min(wav.length, start + Math.max(0, size));
                byte[] pcm = new byte[end - start];
                System.arraycopy(wav, start, pcm, 0, pcm.length);
                return pcm;
            }
            if (size < 0) break;
            pos += 8 + size + (size & 1);
        }
        throw new IllegalStateException("WAV has no data chunk");
    }
}
