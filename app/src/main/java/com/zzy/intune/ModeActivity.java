package com.zzy.intune;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public class ModeActivity extends AppCompatActivity {

    private static final int REQ_RECORD_AUDIO = 1001;
    private String modeKey = "standard";

    private TextView tvModeName;
    private TextView tvDiff;
    private TextView tvDirection;
    private TextView tvTarget;
    private TextView tvCurrent;

    private View pitchCenterCircle;

    private volatile boolean recording = false;
    private Thread recordThread;

    private String[] targetNotes;
    private int[] targetMidis;
    private int targetIndex = 0; // 选中的弦对应 index
    private TextView[] tunerButtons;
    private View[] switchButtons;

    // 新布局：右侧表示琴弦的线条视图（从 1 弦到 6 弦）
    private View lineString1, lineString2, lineString3, lineString4, lineString5, lineString6;

    // 基础降噪参数
    private static final double RMS_THRESHOLD_DEFAULT = 400.0; // 默认阈值
    private static final double CORR_CONFIDENCE_THRESHOLD = 0.3; // 自相关归一化峰值阈值

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode);

        Intent i = getIntent();
        if (i != null) {
            String key = i.getStringExtra("mode_key");
            if (key != null) modeKey = key;
            if (modeKey != null && modeKey.startsWith("preset:")) {
                String presetName = modeKey.substring("preset:".length());
                loadPreset(presetName);
            }
        }

        bindViews();
        setupMode(modeKey);
        setupButtons();

        if (hasRecordPermission()) {
            startAudio();
        } else {
            requestRecordPermission();
        }
    }

    private void bindViews() {
        tvModeName = findViewById(R.id.tvModeName);
        tvDiff = findViewById(R.id.tvDiff);
        tvDirection = findViewById(R.id.tvDirection);
        tvTarget = findViewById(R.id.tvTarget);
        tvCurrent = findViewById(R.id.tvCurrent);
        pitchCenterCircle = findViewById(R.id.viewCircle);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { finish(); }
        });

        TextView b6 = findViewById(R.id.btnTuner6);
        TextView b5 = findViewById(R.id.btnTuner5);
        TextView b4 = findViewById(R.id.btnTuner4);
        TextView b3 = findViewById(R.id.btnTuner3);
        TextView b2 = findViewById(R.id.btnTuner2);
        TextView b1 = findViewById(R.id.btnTuner1);
        tunerButtons = new TextView[]{b6, b5, b4, b3, b2, b1};

        View s6 = findViewById(R.id.btnSwitch6);
        View s5 = findViewById(R.id.btnSwitch5);
        View s4 = findViewById(R.id.btnSwitch4);
        View s3 = findViewById(R.id.btnSwitch3);
        View s2 = findViewById(R.id.btnSwitch2);
        View s1 = findViewById(R.id.btnSwitch1);
        switchButtons = new View[]{s6, s5, s4, s3, s2, s1};

        // 新线条视图
        lineString1 = findViewById(R.id.lineString1);
        lineString2 = findViewById(R.id.lineString2);
        lineString3 = findViewById(R.id.lineString3);
        lineString4 = findViewById(R.id.lineString4);
        lineString5 = findViewById(R.id.lineString5);
        lineString6 = findViewById(R.id.lineString6);

        View saveBtn = findViewById(R.id.btnSavePreset);
        if (saveBtn != null) {
            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    promptSavePreset();
                }
            });
        }

        // 自定义模式的单独编辑入口已移除，保持简洁竖排样式
    }

    private void setupMode(String key) {
        tvModeName.setText(key);
        if (key != null && key.startsWith("preset:")) {
            // 预设：loadPreset 已在 onCreate 解析，避免被默认模式覆盖
            if (targetMidis == null || targetMidis.length != 6) {
                targetNotes = new String[]{"E", "A", "D", "G", "B", "E"};
                targetMidis = new int[]{40, 45, 50, 55, 59, 64};
            }
            setTargetByIndex(0);
            updateTunerButtonsUI();
            setCustomUi(false, key);
            return;
        }
        if ("drop_d".equals(key)) {
            targetNotes = new String[]{"D", "A", "D", "G", "B", "E"}; // 6-1
            targetMidis = new int[]{38, 45, 50, 55, 59, 64}; // D2, A2, D3, G3, B3, E4
        } else if ("open_g".equals(key)) {
            targetNotes = new String[]{"D", "G", "D", "G", "B", "D"};
            targetMidis = new int[]{38, 43, 50, 55, 59, 62}; // D2, G2, D3, G3, B3, D4
        } else {
            targetNotes = new String[]{"E", "A", "D", "G", "B", "E"};
            targetMidis = new int[]{40, 45, 50, 55, 59, 64}; // E2, A2, D3, G3, B3, E4
        }
        setTargetByIndex(0);
        updateTunerButtonsUI();

        setCustomUi("custom".equals(key), key);
    }

    private void setCustomUi(boolean isCustom, String keyForTitle) {
        View saveBtn = findViewById(R.id.btnSavePreset);
        if (saveBtn != null) saveBtn.setVisibility(isCustom ? View.VISIBLE : View.GONE);

        // 自定义模式显示右侧切换按钮；普通模式隐藏
        if (switchButtons != null) {
            int vis = isCustom ? View.VISIBLE : View.GONE;
            for (View v : switchButtons) if (v != null) v.setVisibility(vis);
        }

        // 普通模式文本完全居中显示；自定义模式为左侧文字 + 右侧按钮
        if (tunerButtons != null) {
            for (TextView tv : tunerButtons) {
                if (tv == null) continue;
                tv.setGravity(isCustom ? android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.LEFT : android.view.Gravity.CENTER);
            }
        }

        // 标题显示：预设模式显示预设名
        if (keyForTitle != null && keyForTitle.startsWith("preset:")) {
            tvModeName.setText(keyForTitle.substring("preset:".length()));
        } else {
            tvModeName.setText(keyForTitle);
        }
    }

    private void loadPreset(String name) {
        android.content.SharedPreferences sp = getSharedPreferences("presets", MODE_PRIVATE);
        String val = sp.getString(name, null);
        if (val == null) return;
        String[] parts = val.split(",");
        int[] midis = new int[6];
        for (int idx = 0; idx < Math.min(6, parts.length); idx++) {
            try { midis[idx] = Integer.parseInt(parts[idx]); } catch (Exception e) { midis[idx] = targetMidis != null && idx < targetMidis.length ? targetMidis[idx] : 40; }
        }
        targetMidis = midis;
        targetNotes = new String[]{midiToNoteName(midis[0]), midiToNoteName(midis[1]), midiToNoteName(midis[2]), midiToNoteName(midis[3]), midiToNoteName(midis[4]), midiToNoteName(midis[5])};
    }

    private void showNotePicker(final int btnIndex) {
        // 简易选择：给出常见音名+八度列表
        final String[] options = buildNoteOptions();
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_note))
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String sel = options[which];
                        int midi = parseNoteToMidi(sel);
                        if (midi >= 0 && btnIndex >= 0 && btnIndex < targetMidis.length) {
                            targetMidis[btnIndex] = midi;
                            targetNotes[btnIndex] = midiToNoteName(midi);
                            updateTunerButtonsUI();
                            if (btnIndex == targetIndex) {
                                tvTarget.setText(midiToNoteWithOctave(midi));
                            }
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String[] buildNoteOptions() {
        // 提供 E2..E5 等常见范围
        java.util.List<String> list = new java.util.ArrayList<>();
        for (int midi = 36; midi <= 76; midi++) { // C2..E5 大致覆盖吉他范围
            list.add(midiToNoteWithOctave(midi));
        }
        return list.toArray(new String[0]);
    }

    private int parseNoteToMidi(String s) {
        if (s == null || s.length() < 2) return -1;
        // 解析如 E2 / C#4
        String name;
        int octave;
        if (s.length() >= 3 && s.charAt(1) == '#') {
            name = s.substring(0, 2);
            octave = Integer.parseInt(s.substring(2));
        } else {
            name = s.substring(0, 1);
            octave = Integer.parseInt(s.substring(1));
        }
        int idx = -1;
        for (int i = 0; i < NOTE_NAMES.length; i++) if (NOTE_NAMES[i].equals(name)) idx = i;
        if (idx < 0) return -1;
        return (octave + 1) * 12 + idx;
    }

    private void promptSavePreset() {
        final android.widget.EditText et = new android.widget.EditText(this);
        et.setHint(R.string.preset_name_hint);
        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.save_as_preset)
                .setView(et)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                android.widget.Button okBtn = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
                okBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String name = et.getText().toString().trim();
                        if (name.length() == 0) return; // 允许继续编辑
                        if (name.length() > 20) {
                            android.widget.Toast.makeText(ModeActivity.this, R.string.error_preset_length, android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (presetExists(name)) {
                            android.widget.Toast.makeText(ModeActivity.this, R.string.error_preset_exists, android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }
                        savePreset(name);
                        dialog.dismiss();
                    }
                });
            }
        });
        dialog.show();
    }

    private boolean presetExists(String name) {
        android.content.SharedPreferences sp = getSharedPreferences("presets", MODE_PRIVATE);
        java.util.Set<String> keys = sp.getAll().keySet();
        return keys.contains(name);
    }

    private void savePreset(String name) {
        // 保存为 6 个 MIDI 的逗号分隔
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < targetMidis.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(targetMidis[i]);
        }
        getSharedPreferences("presets", MODE_PRIVATE)
                .edit()
                .putString(name, sb.toString())
                .apply();
        android.widget.Toast.makeText(this, R.string.save_as_preset, android.widget.Toast.LENGTH_SHORT).show();

        // 切换为预设模式：更新模式键与UI（隐藏编辑、隐藏保存、标题为预设名）
        modeKey = "preset:" + name;
        setCustomUi(false, modeKey);
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (v * d + 0.5f);
    }

    private void setupButtons() {
        if (tunerButtons != null) {
            for (int idx = 0; idx < tunerButtons.length; idx++) {
                final int finalIdx = idx;
                tunerButtons[idx].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setTargetByIndex(finalIdx);
                    }
                });
            }
        }

        if (switchButtons != null) {
            for (int idx = 0; idx < switchButtons.length; idx++) {
                final int finalIdx = idx;
                View sw = switchButtons[idx];
                if (sw != null) {
                    sw.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) { showNotePicker(finalIdx); }
                    });
                }
            }
        }
    }

    private void setTargetByIndex(int idx) {
        targetIndex = idx;
        String noteWithOctave = midiToNoteWithOctave(targetMidis[idx]);
        tvTarget.setText(noteWithOctave);
        updateTunerButtonsUI();
        updateStringLinesUI();
    }

    private void updateStringLinesUI() {
        // 根据选中项高亮对应线条（未选中为暗色）
        View[] lines = new View[]{lineString6, lineString5, lineString4, lineString3, lineString2, lineString1};
        // 对应 tunerButtons 顺序：{6,5,4,3,2,1}
        for (int i = 0; i < lines.length; i++) {
            View line = lines[i];
            if (line == null) continue;
            boolean selected = (i == targetIndex);
            int color = selected ? getResources().getColor(R.color.colorAccent) : 0xFF999999;
            android.graphics.drawable.GradientDrawable bg;
            try {
                bg = (android.graphics.drawable.GradientDrawable) line.getBackground();
                bg.setColor(color);
            } catch (ClassCastException e) {
                line.setBackgroundColor(color);
            }
        }
    }

    private void updateTunerButtonsUI() {
        if (tunerButtons == null || targetMidis == null) return;
        int n = Math.min(tunerButtons.length, targetMidis.length);
        for (int i = 0; i < n; i++) {
            tunerButtons[i].setText(midiToNoteWithOctave(targetMidis[i]));
            tunerButtons[i].setSelected(i == targetIndex);
        }
    }

    private boolean hasRecordPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRecordPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.permission_mic_rationale))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(ModeActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startAudio();
            } else {
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.permission_mic_denied))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAudio();
    }

    private void startAudio() {
        if (recording) return;
        recording = true;
        recordThread = new Thread(new Runnable() {
            @Override
            public void run() {
                processAudio();
            }
        }, "AudioRecordThread");
        recordThread.start();
    }

    private void stopAudio() {
        recording = false;
        if (recordThread != null) {
            try { recordThread.join(500); } catch (InterruptedException ignored) {}
            recordThread = null;
        }
    }

    private void processAudio() {
        int sampleRate = 44100;
        int channel = AudioFormat.CHANNEL_IN_MONO;
        int format = AudioFormat.ENCODING_PCM_16BIT;
        int minBuf = AudioRecord.getMinBufferSize(sampleRate, channel, format);
        int bufferSize = Math.max(minBuf, 2048);

        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRate, channel, format, bufferSize);
        short[] buffer = new short[bufferSize];
        NoiseSuppressor ns = null;
        try {
            // 尝试启用系统级降噪
            if (NoiseSuppressor.isAvailable()) {
                try { ns = NoiseSuppressor.create(audioRecord.getAudioSessionId()); } catch (Throwable ignored) {}
            }
            audioRecord.startRecording();
            while (recording) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read > 0) {
                    int cfg = getSharedPreferences("settings", MODE_PRIVATE).getInt("rms_threshold", (int) RMS_THRESHOLD_DEFAULT);
                    double threshold = (double) cfg;
                    double rms = computeRms(buffer, read);
                    if (rms < threshold) {
                        // 噪声过低或环境噪声：视为静音，不更新 UI，保持上一次有效显示
                        continue;
                    }

                    PitchResult pr = estimatePitchWithConfidence(buffer, read, sampleRate);
                    if (pr.confidence < CORR_CONFIDENCE_THRESHOLD || pr.frequency <= 0) {
                        // 置信度不足，不更新 UI，保持上一次有效显示
                        continue;
                    }

                    final int currentMidi = frequencyToMidi(pr.frequency);
                    final String currentNote = midiToNoteWithOctave(currentMidi);
                    final double midiExact = 69.0 + 12.0 * (Math.log(pr.frequency / 440.0) / Math.log(2.0));
                    final double diff = midiExact - targetMidis[targetIndex];
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvCurrent.setText("当前音阶: " + currentNote);
                            // 显示到 0.1 精度
                            String diffText = String.format(Locale.getDefault(), "%+.1f", diff);
                            tvDiff.setText(diffText);
                            // 颜色：偏高红、偏低绿、准确中性
                            if (diff > 0.1) {
                                tvDiff.setTextColor(getResources().getColor(R.color.colorError));
                            } else if (diff < -0.1) {
                                tvDiff.setTextColor(getResources().getColor(R.color.colorSuccess));
                            } else {
                                tvDiff.setTextColor(getResources().getColor(R.color.colorNeutral));
                            }
                            tvDirection.setText(Math.abs(diff) <= 0.1 ? "" : (diff > 0 ? getString(R.string.tune_lower) : getString(R.string.tune_higher)));
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { audioRecord.stop(); } catch (Exception ignored) {}
            audioRecord.release();
            if (ns != null) {
                try { ns.release(); } catch (Throwable ignored) {}
            }
        }
    }

    private PitchResult estimatePitchWithConfidence(short[] data, int len, int sampleRate) {
        // 去直流 + Hann 窗 + 归一化自相关，返回频率与置信度
        double mean = 0.0;
        for (int i = 0; i < len; i++) mean += data[i];
        mean /= len;
        double[] x = new double[len];
        for (int i = 0; i < len; i++) {
            double w = 0.5 - 0.5 * Math.cos(2.0 * Math.PI * i / (len - 1)); // Hann 窗
            x[i] = (data[i] - mean) * w;
        }

        // 能量（lag=0）
        double r0 = 0.0;
        for (int i = 0; i < len; i++) r0 += x[i] * x[i];
        if (r0 <= 1e-9) return new PitchResult(0.0, 0.0);

        int minLag = sampleRate / 1200;
        int maxLag = sampleRate / 70;
        if (minLag < 1) minLag = 1;
        if (maxLag >= len) maxLag = len - 1;

        double bestCorr = 0.0;
        int bestLag = -1;
        for (int lag = minLag; lag <= maxLag; lag++) {
            double corr = 0.0;
            for (int i = 0; i < len - lag; i++) {
                corr += x[i] * x[i + lag];
            }
            double normCorr = corr / r0; // 归一化
            if (normCorr > bestCorr) {
                bestCorr = normCorr;
                bestLag = lag;
            }
        }
        if (bestLag <= 0) return new PitchResult(0.0, 0.0);
        double freq = (double) sampleRate / bestLag;
        return new PitchResult(freq, bestCorr);
    }

    private static class PitchResult {
        final double frequency;
        final double confidence;
        PitchResult(double f, double c) { this.frequency = f; this.confidence = c; }
    }

    private double computeRms(short[] data, int len) {
        double acc = 0.0;
        for (int i = 0; i < len; i++) {
            double v = data[i];
            acc += v * v;
        }
        return Math.sqrt(acc / len);
    }

    private static final String[] NOTE_NAMES = new String[]{
            "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    private int frequencyToMidi(double freq) {
        if (freq <= 0) return -1;
        double n = 69 + 12 * (Math.log(freq / 440.0) / Math.log(2));
        return (int) Math.round(n);
    }

    private String midiToNoteName(int midi) {
        if (midi < 0) return "-";
        int index = (midi % 12 + 12) % 12;
        return NOTE_NAMES[index];
    }

    private String midiToNoteWithOctave(int midi) {
        if (midi < 0) return "-";
        String name = midiToNoteName(midi);
        int octave = (midi / 12) - 1; // MIDI 定义
        return name + octave;
    }

    private int noteToIndex(String note) {
        if (note == null) return -100;
        for (int i = 0; i < NOTE_NAMES.length; i++) {
            if (NOTE_NAMES[i].equalsIgnoreCase(note)) return i;
        }
        return -100;
    }

    private int noteDiffSemi(String current, String target) {
        int ci = noteToIndex(current);
        int ti = noteToIndex(target);
        if (ci < 0 || ti < 0) return 0;
        int diff = ci - ti; // 正数：高，负数：低
        // 将差值约束在 [-6, 6] 以提示方向
        if (diff > 6) diff -= 12;
        if (diff < -6) diff += 12;
        return diff;
    }

    private int noteDiffSemiByMidiRaw(int currentMidi, int targetMidi) {
        if (currentMidi < 0) return 0;
        int diff = currentMidi - targetMidi; // 正数：高，负数：低；不跨八度折返
        if (diff > 36) diff = 36; // 简单裁剪，避免异常值
        if (diff < -36) diff = -36;
        return diff;
    }
}


