# Delivers 
## 2022 Mobile Programming Team Project

### 프로젝트 소개
- 이웃들과 함께 배달음식을 주문함으로써 배달비를 절감하고 최소 주문 금액을 충족시킬 수 있도록 하는 Android 어플리케이션입니다.
- 인스타그램과 유사한 등록/조회 UI를 통해, 사용자로 하여금 보다 편리하고 빠른 이용이 가능하도록 만들었습니다.
- 네이버 맵 API를 사용해서 현 위치 근처의 이웃들이 남긴 게시글 위주로 확인할 수 있습니다.
- 1:1 채팅을 통해 유저 간 소통을 원할하게 만들었습니다.

### 프로젝트 기간
2022.11 한달간 진행하였습니다.

### 프로젝트 구조도

<img width="1201" alt="image" src="https://user-images.githubusercontent.com/74501631/219410903-8f9b95e9-0098-4de4-b67d-1d8f3a4a3e21.png">

<img width="1512" alt="image" src="https://user-images.githubusercontent.com/74501631/219410801-d87d5968-4093-441d-b10f-b564acb601f5.png">



### 프로젝트 디테일

#### 1. 메인 페이지
<img width="199" alt="image" src="https://user-images.githubusercontent.com/74501631/219402417-1bcb5c25-a3e6-44c9-896a-309a9aded105.png">
Naver Map API를 이용해서 맵 뷰를 구현했습니다.<br/> <br/> 
24시간 내 등록된 게시글들을 화면 상단에서 한 눈에 확인할 수 있습니다.<br/> 
Geocoder을 사용하여 위도, 경도를 주소 문자열로 받아왔습니다.<br/> 
Marker은 각 post의 카테고리별 아이콘을 Pin에서 확인할 수 있도록 customized 했습니다.<br/> 

<img width="475" alt="image" src="https://user-images.githubusercontent.com/74501631/219406085-770b1178-1e52-451f-b4b3-2625cf01034c.png">
인스타그램과 유사하게, Room DB를 이용해서 한 번 확인한 게시글의 테두리는 회색으로 변하도록 설정했습니다.<br/> <br/> <br/> 



#### 2. 등록 페이지<br/> 
<img width="210" alt="image" src="https://user-images.githubusercontent.com/74501631/219402297-1bf14ff9-83db-409c-ac06-658ff601a888.png">
Kakao Map API를 통해 맵 상에서 핀을 찍어 위치를 받아올 수 있도록 했습니다.<br/> 

<img width="173" alt="image" src="https://user-images.githubusercontent.com/74501631/219403555-cd669525-6f64-43db-8468-2d893f9ce3c7.png">
Tensorflow Lite를 사용해서 Image Classification 진행, 업로드된 사진을 인식하여 음식의 종류를 자동으로 텍스트화 해주었습니다.<br/> <br/> 
<img width="216" alt="image" src="https://user-images.githubusercontent.com/74501631/219402331-16525522-edb3-4215-a909-6f91b96e995d.png"><br/> <br/> <br/> 



#### 3. 디테일 페이지<br/> 
<img width="175" alt="image" src="https://user-images.githubusercontent.com/74501631/219407595-08fa6daa-5132-4e92-8c4e-21963fb56d2b.png">
마찬가지로 인스타그램 스토리와 유사한 UI를 보여주며, 일정 시간이 경과했을 때 자동으로 다음 Post로 넘어가도록 했습니다. 스크린 상 좌우를 클릭하면 이전, 다음 게시글로 기다리지 않고 이동 가능합니다.<br/> 
이 부분은 com.github.teresaholfeld:Stories:1.1.4 라이브러리를 사용했습니다.<br/> 
출처 - https://github.com/teresaholfeld/Stories<br/> <br/> <br/> 

#### 4. 1:1 채팅<br/> 
<img width="354" alt="image" src="https://user-images.githubusercontent.com/74501631/219408962-5ff1939c-4655-4af1-8253-d2a13ad204c4.png"><br/> 
Firebase Realtime Database를 사용해서 실시간 채팅을 구현했습니다.<br/> <br/> 
<img width="176" alt="image" src="https://user-images.githubusercontent.com/74501631/219409262-4847f88a-58f1-4749-9e3d-3bed2986e4c0.png"><br/> <br/> <br/> 

<img width="151" alt="image" src="https://user-images.githubusercontent.com/74501631/219409330-8a6e147b-efe7-4381-9f2f-275eef1cc1a0.png"><br/> 
TessBaseAPI를 사용해서, 채팅방 안에서 결제 내역이 보이는 스크린샷을 전송했을 경우 인원수 입력을 통해 자동으로 받을 돈을 계산해주도록 했습니다.<br/> 
OCR model을 이용하여 사진 상 확인되는 모든 Text를 추출하고, 총 결제금액을 가져오는 방식으로 진행했습니다. <br/> 

<img width="167" alt="image" src="https://user-images.githubusercontent.com/74501631/219409533-58b0dbf4-6465-422c-b007-b3a88e13f816.png"><br/> 
<img width="509" alt="image" src="https://user-images.githubusercontent.com/74501631/219410024-bd66809c-5449-4b0b-96a2-e1abedf2b891.png"><br/> 


