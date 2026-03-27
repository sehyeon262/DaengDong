package com.e108.be.global.config;

import com.e108.be.global.common.util.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 서버 시작 시 resources/demo 폴더의 사진들을 S3 shared/demo/에 자동 업로드.
 * 업로드된 URL은 DemoPhotoConfig.photoUrls에 자동 세팅됨.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoPhotoInitializer {

    private final DemoPhotoConfig demoPhotoConfig;
    private final S3Service s3Service;

    @EventListener(ApplicationReadyEvent.class)
    public void uploadDemoPhotos() {
        if (!demoPhotoConfig.isEnabled()) {
            log.info("[데모모드] 비활성화 상태, 사진 업로드 스킵");
            return;
        }

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:demo/*");

            if (resources.length == 0) {
                log.warn("[데모모드] resources/demo 폴더에 사진이 없습니다");
                return;
            }

            List<String> uploadedUrls = new ArrayList<>();

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;

                // 이미지 파일만 처리
                String lowerName = filename.toLowerCase();
                if (!lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg") && !lowerName.endsWith(".png")) {
                    continue;
                }

                try (InputStream is = resource.getInputStream()) {
                    byte[] data = is.readAllBytes();
                    String contentType = lowerName.endsWith(".png") ? "image/png" : "image/jpeg";

                    // 파일명을 소문자로 통일해서 업로드 (emotion-overrides 매칭 용이)
                    String url = s3Service.uploadSharedWithFixedKey(data, contentType, "demo", lowerName);
                    uploadedUrls.add(url);
                    log.info("[데모모드] 사진 업로드 완료: {} → {}", filename, url);
                } catch (IOException e) {
                    log.error("[데모모드] 사진 업로드 실패: {}", filename, e);
                }
            }

            // DemoPhotoConfig에 URL 세팅
            demoPhotoConfig.setPhotoUrls(uploadedUrls);
            log.info("[데모모드] 총 {}장 사진 업로드 완료, photoUrls 세팅됨", uploadedUrls.size());

        } catch (IOException e) {
            log.error("[데모모드] resources/demo 폴더 읽기 실패", e);
        }
    }
}
