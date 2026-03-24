package com.e108.be.domain.diary.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.nio.FloatBuffer;
import java.util.Map;

/**
 * 강아지 감정 분석기 (ONNX Runtime)
 *
 * PuppySense_v2 모델을 사용하여 강아지 사진에서 감정을 추론한다.
 * 앱 시작 시 모델을 1회 로드하고, 이후 요청마다 재사용한다.
 *
 * 클래스: angry(0), happy(1), relaxed(2), sad(3)
 * 입력: 224x224x3 RGB 이미지
 */
@Component
@Slf4j
public class DogEmotionAnalyzer {

    private static final int IMG_SIZE = 224;
    private static final String[] CLASSES = {"angry", "happy", "relaxed", "sad"};
    private static final Map<String, String> EMOTION_TAG_MAP = Map.of(
            "happy", "행복",
            "relaxed", "편안",
            "sad", "슬픔",
            "angry", "화남"
    );

    @Value("${emotion.model-path}")
    private String modelPath;

    @Value("${emotion.confidence-threshold}")
    private double confidenceThreshold;

    private OrtEnvironment env;
    private OrtSession session;

    @PostConstruct
    public void init() {
        try {
            env = OrtEnvironment.getEnvironment();
            session = env.createSession(modelPath);
            log.info("[감정분석] ONNX 모델 로드 완료: {}", modelPath);
        } catch (Exception e) {
            log.error("[감정분석] ONNX 모델 로드 실패: {}", modelPath, e);
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            if (session != null) session.close();
        } catch (Exception e) {
            log.warn("[감정분석] 세션 종료 중 오류", e);
        }
    }

    /**
     * S3 이미지 URL로부터 강아지 감정을 분석한다.
     *
     * @param imageUrl S3에 저장된 사진 URL
     * @return 분석 결과 (감정 + 신뢰도 + 태그), 실패 시 null
     */
    /**
     * 이미지 전처리 (다운로드 + 리사이즈) — 스레드 안전, 병렬 호출 가능
     */
    public float[][][][] prepareImage(String imageUrl) throws Exception {
        return preprocessImage(imageUrl);
    }

    /**
     * 전처리된 이미지 데이터로 ONNX 추론 수행 — synchronized (세션은 thread-safe하지 않음)
     */
    public synchronized EmotionResult infer(String imageUrl, float[][][][] inputData) {
        try {
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData);
            String inputName = session.getInputNames().iterator().next();
            var results = session.run(Map.of(inputName, inputTensor));

            float[][] output = (float[][]) results.get(0).getValue();
            float[] probabilities = output[0];

            int maxIdx = 0;
            for (int i = 1; i < probabilities.length; i++) {
                if (probabilities[i] > probabilities[maxIdx]) {
                    maxIdx = i;
                }
            }

            String emotion = CLASSES[maxIdx];
            float confidence = probabilities[maxIdx];

            inputTensor.close();
            results.close();

            if (confidence < confidenceThreshold) {
                log.debug("[감정분석] 신뢰도 낮음 ({}), 강아지 없는 사진으로 판단: {}", confidence, imageUrl);
                return null;
            }

            String emotionTag = EMOTION_TAG_MAP.getOrDefault(emotion, "평온");
            log.info("[감정분석] 결과: {} (신뢰도: {}), 태그: {}", emotion, confidence, emotionTag);
            return new EmotionResult(emotion, confidence, emotionTag);

        } catch (Exception e) {
            log.warn("[감정분석] 추론 실패: {}", imageUrl, e);
            return null;
        }
    }

    public EmotionResult analyze(String imageUrl) {
        if (session == null) {
            log.warn("[감정분석] 모델이 로드되지 않아 분석 스킵");
            return null;
        }

        try {
            float[][][][] inputData = preprocessImage(imageUrl);
            return infer(imageUrl, inputData);
        } catch (Exception e) {
            log.warn("[감정분석] 분석 실패, 스킵: {}", imageUrl, e);
            return null;
        }
    }

    /**
     * 이미지 URL에서 다운로드 후 224x224로 리사이즈하고 float 배열로 변환
     * 모델 내부에 Rescaling(1/255) 레이어가 있으므로 0~255 그대로 전달
     */
    private float[][][][] preprocessImage(String imageUrl) throws Exception {
        BufferedImage original;
        try (InputStream is = URI.create(imageUrl).toURL().openStream()) {
            original = ImageIO.read(is);
        }

        // 224x224로 리사이즈
        BufferedImage resized = new BufferedImage(IMG_SIZE, IMG_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, IMG_SIZE, IMG_SIZE, null);
        g.dispose();

        // float[1][224][224][3] 배열로 변환 (NHWC, 0~255)
        float[][][][] data = new float[1][IMG_SIZE][IMG_SIZE][3];
        for (int y = 0; y < IMG_SIZE; y++) {
            for (int x = 0; x < IMG_SIZE; x++) {
                int rgb = resized.getRGB(x, y);
                data[0][y][x][0] = (rgb >> 16) & 0xFF; // R
                data[0][y][x][1] = (rgb >> 8) & 0xFF;  // G
                data[0][y][x][2] = rgb & 0xFF;          // B
            }
        }

        return data;
    }

    /**
     * 모델이 정상 로드되었는지 확인
     */
    public boolean isAvailable() {
        return session != null;
    }
}
