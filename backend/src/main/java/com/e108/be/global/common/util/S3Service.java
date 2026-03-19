package com.e108.be.global.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

/**
 * S3 이미지 업로드 공통 서비스
 *
 * 파일을 S3에 업로드하고 공개 URL을 반환한다.
 * key 형식: {prefix}/{폴더}/{UUID}.{확장자}
 * 예: local/places/550e8400-e29b-41d4-a716-446655440000.jpg
 */
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.prefix:}")
    private String prefix;

    /**
     * 환경별 prefix 포함하여 S3에 업로드 (환경 분리용)
     * key: {prefix}/{folder}/{uuid}.{ext}  (예: local/diary/uuid.jpg)
     *
     * @param file   업로드할 파일
     * @param folder S3 내 폴더 (예: "diary", "dogs")
     * @return 업로드된 파일의 S3 URL
     */
    public String upload(MultipartFile file, String folder) {
        String key = buildKey(folder, file.getOriginalFilename());

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        try {
            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new RuntimeException("S3 파일 업로드에 실패했습니다.", e);
        }

        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    /**
     * 환경 무관 고정 경로로 S3에 업로드 (환경 간 공유용)
     * key: shared/{folder}/{uuid}.{ext}  (예: shared/places/uuid.jpg)
     *
     * 장소 이미지처럼 로컬/dev/prod에서 동일한 URL로 접근해야 하는 경우 사용
     *
     * @param file   업로드할 파일
     * @param folder S3 내 폴더 (예: "places")
     * @return 업로드된 파일의 S3 URL
     */
    public String uploadShared(MultipartFile file, String folder) {
        String ext = extractExtension(file.getOriginalFilename());
        String uuid = UUID.randomUUID().toString();
        String key = String.format("shared/%s/%s.%s", folder, uuid, ext);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        try {
            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new RuntimeException("S3 파일 업로드에 실패했습니다.", e);
        }

        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    /**
     * S3 key 생성: {prefix}/{folder}/{uuid}.{ext}
     */
    private String buildKey(String folder, String originalFilename) {
        String ext = extractExtension(originalFilename);
        String uuid = UUID.randomUUID().toString();

        if (prefix != null && !prefix.isBlank()) {
            return String.format("%s/%s/%s.%s", prefix, folder, uuid, ext);
        }
        return String.format("%s/%s.%s", folder, uuid, ext);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
