package com.example.tripplan.domain.model

object TravelCityCatalog {
    private val citiesByCountry = mapOf(
        "KR" to listOf("서울", "부산", "제주", "경주", "강릉"),
        "JP" to listOf("도쿄", "오사카", "교토", "후쿠오카", "삿포로", "오키나와"),
        "TH" to listOf("방콕", "파타야", "푸껫", "치앙마이", "코사무이"),
        "VN" to listOf("다낭", "하노이", "호찌민", "나트랑", "푸꾸옥"),
        "TW" to listOf("타이베이", "가오슝", "타이중", "타이난"),
        "CN" to listOf("베이징", "상하이", "칭다오", "광저우", "장가계"),
        "US" to listOf("뉴욕", "로스앤젤레스", "라스베이거스", "샌프란시스코", "하와이"),
        "FR" to listOf("파리", "니스", "리옹", "마르세유"),
        "IT" to listOf("로마", "밀라노", "피렌체", "베네치아", "나폴리"),
        "ES" to listOf("바르셀로나", "마드리드", "세비야", "그라나다"),
        "GB" to listOf("런던", "에든버러", "맨체스터", "리버풀"),
        "DE" to listOf("베를린", "뮌헨", "프랑크푸르트", "함부르크"),
        "CH" to listOf("취리히", "인터라켄", "루체른", "제네바"),
        "SG" to listOf("싱가포르"),
        "PH" to listOf("마닐라", "세부", "보라카이", "보홀"),
        "ID" to listOf("발리", "자카르타", "족자카르타"),
        "MY" to listOf("쿠알라룸푸르", "코타키나발루", "페낭", "말라카"),
        "AU" to listOf("시드니", "멜버른", "브리즈번", "퍼스", "골드코스트"),
        "CA" to listOf("밴쿠버", "토론토", "몬트리올", "퀘벡"),
        "CZ" to listOf("프라하", "체스키크룸로프"),
        "AT" to listOf("빈", "잘츠부르크", "인스브루크"),
    )

    fun cities(countryCode: String): List<String> =
        citiesByCountry[countryCode.uppercase()].orEmpty()
}
