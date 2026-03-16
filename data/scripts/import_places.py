"""
반려동문 문화시설 데이터 CSV → places 테이블 임포트 스크립트

사용법:
    pip install psycopg2-binary
    python import_places.py --csv "../raw/반려동문 문화시설 데이터.csv"

환경변수 (또는 infra/.env 파일):
    DB_HOST     (default: localhost)
    DB_PORT     (default: 5432)
    DB_NAME     (default: e108_db)
    DB_USER     (default: postgres)
    DB_PASSWORD (필수)
"""

import csv
import hashlib
import os
import sys
import argparse
from datetime import datetime, date

try:
    import psycopg2
    import psycopg2.extras
except ImportError:
    print("psycopg2가 없습니다. 먼저 실행: pip install psycopg2-binary")
    sys.exit(1)

# ── 카테고리 매핑 (CTGRY_THREE_NM → place_category.name) ──────────────────
CATEGORY_MAP = {
    "동물병원":     "동물병원",
    "동물약국":     "동물약국",
    "미용":         "미용",
    "반려동물용품": "반려동물용품",
    "위탁관리":     "위탁관리",
    "카페":         "카페",
    "식당":         "식당",
    "박물관":       "박물관",
    "미술관":       "미술관",
    "문예회관":     "문예회관",
    "여행지":       "여행지",
    "펜션":         "펜션",
    "호텔":         "호텔",
}


def make_source_id(row: dict) -> str:
    """이름 + 위도 + 경도를 SHA256 해시하여 source_id 생성"""
    key = f"{row['FCLTY_NM']}|{row['LC_LA']}|{row['LC_LO']}"
    return hashlib.sha256(key.encode()).hexdigest()[:40]


def build_description(row: dict) -> str:
    """CSV 여러 컬럼을 합쳐 설명 문자열 생성"""
    parts = []
    if row.get("OPER_TIME"):
        parts.append(f"운영시간: {row['OPER_TIME']}")
    if row.get("RSTDE_GUID_CN"):
        parts.append(f"휴무: {row['RSTDE_GUID_CN']}")
    if row.get("PARKNG_POSBL_AT"):
        parts.append(f"주차: {'가능' if row['PARKNG_POSBL_AT'] == 'Y' else '불가'}")
    if row.get("IN_PLACE_ACP_POSBL_AT"):
        parts.append(f"실내동반: {'가능' if row['IN_PLACE_ACP_POSBL_AT'] == 'Y' else '불가'}")
    if row.get("OUT_PLACE_ACP_POSBL_AT"):
        parts.append(f"실외동반: {'가능' if row['OUT_PLACE_ACP_POSBL_AT'] == 'Y' else '불가'}")
    if row.get("PET_LMTT_MTR_CN"):
        parts.append(f"반려동물 제한: {row['PET_LMTT_MTR_CN']}")
    if row.get("UTILIIZA_PRC_CN"):
        parts.append(f"이용요금: {row['UTILIIZA_PRC_CN']}")
    return " | ".join(parts) if parts else None


def parse_updated_at(val: str):
    """YYYYMMDD 형식 파싱"""
    if val and len(val) == 8:
        try:
            return datetime.strptime(val, "%Y%m%d")
        except ValueError:
            pass
    return None


def get_db_conn():
    return psycopg2.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", 5432)),
        dbname=os.getenv("DB_NAME", "e108_db"),
        user=os.getenv("DB_USER", "postgres"),
        password=os.getenv("DB_PASSWORD", ""),
    )


def load_category_map(cur) -> dict:
    """place_category 테이블에서 name → id 매핑 로드"""
    cur.execute("SELECT id, name FROM place_category")
    return {name: cid for cid, name in cur.fetchall()}


def import_csv(csv_path: str, batch_size: int = 500):
    conn = get_db_conn()
    conn.autocommit = False
    cur = conn.cursor()

    category_name_to_id = load_category_map(cur)
    print(f"카테고리 {len(category_name_to_id)}개 로드 완료")

    INSERT_SQL = """
        INSERT INTO places
            (provider, source_id, name, category_id, location, address, contact, description, is_active, updated_at)
        VALUES
            (%s, %s, %s, %s, ST_SetSRID(ST_MakePoint(%s, %s), 4326), %s, %s, %s, true, %s)
        ON CONFLICT (provider, source_id) DO UPDATE SET
            name        = EXCLUDED.name,
            category_id = EXCLUDED.category_id,
            location    = EXCLUDED.location,
            address     = EXCLUDED.address,
            contact     = EXCLUDED.contact,
            description = EXCLUDED.description,
            updated_at  = EXCLUDED.updated_at,
            is_active   = true
    """

    inserted = skipped = 0
    batch = []

    with open(csv_path, encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        for i, row in enumerate(reader, start=1):
            try:
                lat = float(row["LC_LA"])
                lon = float(row["LC_LO"])
            except (ValueError, KeyError):
                skipped += 1
                continue

            cat_raw = row.get("CTGRY_THREE_NM", "").strip()
            cat_name = CATEGORY_MAP.get(cat_raw)
            category_id = category_name_to_id.get(cat_name) if cat_name else None

            address = (row.get("RDNMADR_NM") or row.get("LNM_ADDR") or "").strip() or None
            contact = row.get("TEL_NO", "").strip()[:50] or None
            updated_at = parse_updated_at(row.get("LAST_UPDT_DE", "").strip())

            batch.append((
                "CULTURE_FACILITY",
                make_source_id(row),
                row["FCLTY_NM"].strip()[:100],
                category_id,
                lon, lat,          # ST_MakePoint(경도, 위도)
                address,
                contact,
                build_description(row),
                updated_at,
            ))

            if len(batch) >= batch_size:
                psycopg2.extras.execute_batch(cur, INSERT_SQL, batch)
                conn.commit()
                inserted += len(batch)
                batch.clear()
                print(f"  {inserted}건 처리 완료...")

    if batch:
        psycopg2.extras.execute_batch(cur, INSERT_SQL, batch)
        conn.commit()
        inserted += len(batch)

    cur.close()
    conn.close()
    print(f"\n완료: 삽입/업서트 {inserted}건, 좌표 오류 스킵 {skipped}건")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--csv", required=True, help="CSV 파일 경로 (예: ../raw/반려동문\ 문화시설\ 데이터.csv)")
    parser.add_argument("--batch", type=int, default=500, help="배치 크기 (default: 500)")
    args = parser.parse_args()

    # infra/.env 파일 로드 (있으면)
    env_path = os.path.join(os.path.dirname(__file__), "../../infra/.env")
    if os.path.exists(env_path):
        with open(env_path) as ef:
            for line in ef:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    os.environ.setdefault(k.strip(), v.strip())

    import_csv(args.csv, args.batch)
