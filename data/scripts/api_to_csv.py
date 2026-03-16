"""
공공데이터포털 반려동물 관광 API → CSV 수집 스크립트

사용법:
    pip install requests pandas
    python api_to_csv.py

출력 파일: ../raw/pet_tour_data.csv
"""

import os
import requests
import pandas as pd


def save_openapi_to_csv():
    # 1. API 엔드포인트 및 인증키 설정
    url = "http://apis.data.go.kr/B551011/KorPetTourService2/areaBasedList2"

    # 발급받은 일반 인증키(Decoding)를 아래에 문자열로 넣어줘
    service_key = "e5575f26e347f6f0af2858796a7bf349716a34de5a6052cf411327ae6ab10dc3"

    # 2. 파라미터 세팅 (API 명세서 참고)
    params = {
        "serviceKey": service_key,
        "numOfRows": "100000",      # 한 번에 가져올 데이터 개수
        "pageNo": "1",              # 페이지 번호
        "MobileOS": "AND",          # OS 구분 (필수)
        "MobileApp": "Dang",        # 서비스명 (필수)
        "_type": "json"             # 데이터를 JSON 형식으로 받기
    }

    try:
        # 3. 데이터 요청
        print("데이터를 요청하는 중...")
        response = requests.get(url, params=params)
        response.raise_for_status()

        data = response.json()

        # 4. JSON에서 실제 데이터 리스트 추출
        items = data['response']['body']['items']['item']

        # 5. Pandas 데이터프레임으로 변환 후 CSV 저장 (raw/ 디렉토리에 저장)
        df = pd.DataFrame(items)
        output_path = os.path.join(os.path.dirname(__file__), "../raw/pet_tour_data.csv")
        df.to_csv(output_path, index=False, encoding="utf-8-sig")
        print(f"pet_tour_data.csv 파일이 성공적으로 저장됐어! → {output_path}")

    except requests.exceptions.RequestException as e:
        print(f"네트워크 요청 에러가 발생했어: {e}")
    except KeyError:
        print("데이터 구조가 예상과 다르거나 데이터가 없어. 인증키가 미승인 상태이거나 파라미터 오류일 수 있어!")
        print(f"응답 데이터 확인: {data}")


if __name__ == "__main__":
    save_openapi_to_csv()
