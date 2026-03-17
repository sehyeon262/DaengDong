# data/

반려견 동반 장소 데이터 수집 및 DB 적재 관련 스크립트 모음

## 디렉토리 구조

```
data/
  scripts/
    api_to_csv.py       # 공공데이터 API → CSV 수집
    import_places.py    # CSV → places 테이블 적재
  raw/                  # CSV 원본 파일 보관 (gitignore 처리 - 로컬에만 존재)
```

## CSV 파일 출처

| 파일명 | 출처 | 비고 |
|--------|------|------|
| 반려동문 문화시설 데이터.csv | [공공데이터포털 - 반려동물 동반 문화시설](https://www.data.go.kr) | 23,929건 |
| pet_tour_data.csv | 한국관광공사 반려동물 동반여행 API | `api_to_csv.py`로 수집 |

## 사용법

### 1. CSV → DB 적재
```bash
pip install psycopg2-binary

cd data/scripts
python import_places.py --csv "../raw/반려동문 문화시설 데이터.csv"
```

### 2. 관광공사 API → CSV 수집
```bash
pip install requests pandas

# infra/.env 에 API 키 추가
echo "PET_TOUR_API_KEY=발급받은_인증키" >> ../infra/.env

cd data/scripts
python api_to_csv.py
# → raw/pet_tour_data.csv 생성
```

## 환경변수 (infra/.env)

| 변수명 | 용도 | 필수 |
|--------|------|------|
| `DB_PASSWORD` | DB 연결 비밀번호 | ✅ |
| `PET_TOUR_API_KEY` | 공공데이터포털 반려동물 관광 API 인증키 | `api_to_csv.py` 사용 시 |

## 주의사항
- `raw/` 폴더는 `.gitignore` 처리되어 있어 git에 커밋되지 않음
- CSV 파일은 팀 공유 채널(Notion/Mattermost)에서 다운로드 후 `raw/`에 위치시킬 것
- `PET_TOUR_API_KEY`는 절대 코드에 하드코딩하지 말 것 — 반드시 `infra/.env`를 통해 주입
