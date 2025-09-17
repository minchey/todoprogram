# 📌 TodoProgram (JavaFX + SQLite 일정 관리 프로그램)

## 📝 소개
**TodoProgram** 은 JavaFX 기반의 데스크톱 일정 관리 애플리케이션입니다.  
단발성 일정과 반복 일정을 함께 관리할 수 있고, **완료/미완료 토글**, **달력 시각화**, **D-Day 표시** 기능을 제공합니다.  
SQLite를 내장 DB로 사용하기 때문에 별도의 서버 없이도 동작합니다.  

---

## 🚀 주요 기능
- ✅ **오늘 할 일 패널**
  - 오늘까지 마감된 단발 일정 표시  
  - 반복 일정 표시  
  - 완료/미완료 토글 가능  
  - D-Day 및 마감일/시간 표시  

- 📅 **달력 뷰**
  - 월 단위 달력 표시  
  - 날짜별 일정 개수를 **동그라미(●)** 로 시각화  
    - 회색 = 미완료  
    - 초록 = 완료  
    - 빨강 = 반복 일정  
  - 날짜 클릭 시 해당 날짜의 할 일 모달창 열림  

- 🖊 **일정 추가**
  - 단발성 일정: 마감일 + 시간 지정 가능  
  - 반복 일정: 요일 선택, 시작일 지정, 매주 반복  

- 🔄 **일정 관리**
  - 완료/미완료 전환  
  - 필요 없는 일정 삭제  

- 💾 **DB 관리**
  - SQLite (`todo.db`) 자동 생성  
  - 테이블/컬럼 자동 마이그레이션  
  - 사용자 PC 환경에 맞게 `%APPDATA%/TodoProgram/todo.db` 저장  

---

## 🛠 기술 스택
- **Language**: Java 21
- **UI**: JavaFX 21
- **DB**: SQLite (xerial-jdbc)
- **Build Tool**: Gradle
- **Packaging**: jlink + jpackage (WiX Toolset)

---

## 📦 설치 방법

### 1) 실행 파일 다운로드
- `build/jpackage/TodoProgram-Setup-x.y.z.msi` 실행 (Windows 설치 프로그램)  
- 설치 후 시작 메뉴에서 **TodoProgram** 실행 가능  

### 2) 자동 실행 설정 (선택)
Windows 시작 시 자동 실행하려면:  
- `Win + R` → `shell:startup` 입력  
- 설치된 `TodoProgram.exe` 바로가기 복사 붙여넣기  

---

## 🔧 개발/빌드 방법

### Prerequisites
- JDK 21 (Adoptium Temurin 추천)  
- Gradle (Wrapper 포함)  
- WiX Toolset (Windows MSI 패키징 시 필요)  

### 빌드 & 실행
```bash
# 클린 빌드
gradlew.bat clean build

# jlink로 런타임 이미지 생성
gradlew.bat jlink

# jpackage로 설치 파일(msi) 생성
gradlew.bat jpackage

# 실행 테스트
build/image/bin/TodoProgram.bat
```

---

## 📂 프로젝트 구조
```
todoprogram/
 ┣ src/main/java/com/example/organizer/
 ┃ ┣ App.java          # 메인 JavaFX 앱
 ┃ ┣ Task.java         # Task 엔티티
 ┃ ┣ TaskDao.java      # DB 접근 (CRUD + 반복업무)
 ┃ ┗ Database.java     # SQLite 연결/마이그레이션
 ┣ build.gradle        # Gradle 빌드 스크립트
 ┣ settings.gradle     # Gradle 설정
 ┗ README.md           # 프로젝트 설명
```

---

## 📸 스크린샷
(캘린더, 오늘 할 일 패널, 모달창 스크린샷 추가 가능)
<img width="1202" height="710" alt="image" src="https://github.com/user-attachments/assets/7da02d69-8360-4adc-b306-25b48c4031d4" />


---

## 👨‍💻 개발자
- **Minchey** (2025)  
