package com.bj.fishingdiary.domain.entity

/**
 * 해역 및 관측소 정보 Entity
 * Sea Area and Observation Station Information Entities
 *
 * 한국 해역(서해, 동해, 남해)과 각 해역의 관측소 정보를 정의
 * Defines Korean sea areas (West, East, South) and observation stations in each area
 */

/**
 * 해역 타입
 * Sea Area Type
 *
 * 한국 주변 3개 해역 구분
 * Classification of 3 sea areas around Korea
 *
 * @property displayName 화면에 표시될 이름
 * @property id API 요청 시 사용할 ID
 */
enum class Sea(val displayName: String, val id: String) {
    /**
     * 선택 안 함
     * Not selected
     */
    NONE("선택", ""),

    /**
     * 서해 (황해)
     * West Sea (Yellow Sea)
     */
    WEST("서해", "W"),

    /**
     * 동해
     * East Sea
     */
    EAST("동해", "E"),

    /**
     * 남해
     * South Sea
     */
    SOUTH("남해", "S");

    companion object {
        /**
         * ID로 해역 찾기
         * Find sea area by ID
         *
         * @param id 해역 ID ("W", "E", "S")
         * @return 해당하는 Sea 객체, 없으면 NONE
         */
        fun fromId(id: String): Sea {
            return values().find { it.id == id } ?: NONE
        }
    }
}

/**
 * 관측소 인터페이스
 * Observation Station Interface
 *
 * 모든 관측소가 구현해야 하는 공통 속성
 * Common properties that all observation stations must implement
 */
interface Observ {
    /**
     * 관측소 코드 (API 요청 시 사용)
     * Station code (used in API requests)
     */
    val cd: String

    /**
     * 관측소 이름 (화면 표시용)
     * Station name (for display)
     */
    val title: String
}

/**
 * 서해 관측소
 * West Sea Observation Stations
 *
 * 서해(황해)에 위치한 해양 관측소 목록
 * List of marine observation stations located in the West Sea (Yellow Sea)
 *
 * enum class로 정의하여 타입 안전성 보장
 * Defined as enum class to ensure type safety
 */
enum class WestObserv(override val title: String, override val cd: String) : Observ {
    NONE("선택", ""),
    GUNSAN_BIANDO("군산 비안도", "bgbi5"),
    GUNSAN_SINSIDO("군산 신시도", "egsi4"),
    GUNSAN_HOENGGYEONGDO("군산 횡경도", "bghi5"),
    MOKPO("목포", "emp67"),
    MOKPO_OEDAL("목포 외달", "fmoj7"),
    MOKPO_DORIPO("목포 도리포", "fmdka"),
    MUAN_SEOBUG("무안 서북", "fmsm6"),
    MUAN_SEONGNAE("무안 성내", "fmsj7"),
    BAEGLYEONGDO("백령도", "fbn69"),
    BORYEONG_SABSIDO("보령 삽시도", "bbsi5"),
    BORYEONG_HYOJADO("보령 효자도", "br001"),
    BUAN_BYEONSAN("부안 변산", "bbbi5"),
    BUAN_WIDO("부안 위도", "bbwi5"),
    SEOSAN_JIGOG("서산 지곡", "sj086"),
    SEOSAN_CHANGLI("서산 창리", "fsch6"),
    SEOCHEON_MARYANG("서천 마량", "bsmi5"),
    // ... 더 많은 관측소 (필요시 추가)
    // ... More stations (add as needed)

    TAEAN_NAEPO("태안 내포", "btni5"),
    TAEAN_DAEYADO("태안 대야도", "ftdk5"),
    TAEAN_SINJINDO("태안 신진도", "btsi5"),
    TAEAN_ANMYEONDO("태안 안면도", "btai5"),
    INCHEON_IJAGDO("인천 이작도", "biii5"),
    INCHEON_JAWOLDO("인천 자월도", "biai5"),
    INCHEON_JANGBONGDO("인천 장봉도", "biji5");

    companion object {
        /**
         * 관측소 코드로 찾기
         * Find station by code
         */
        fun fromCode(code: String): WestObserv {
            return values().find { it.cd == code } ?: NONE
        }

        /**
         * 모든 관측소 목록 반환 (NONE 제외)
         * Return all stations (excluding NONE)
         */
        fun allStations(): List<WestObserv> {
            return values().filter { it != NONE }
        }
    }
}

/**
 * 동해 관측소
 * East Sea Observation Stations
 *
 * 동해에 위치한 해양 관측소 목록
 * List of marine observation stations located in the East Sea
 */
enum class EastObserv(override val title: String, override val cd: String) : Observ {
    NONE("선택", ""),
    GANGNEUNG("강릉", "bgna3"),
    GORI("고리", "bgrh3"),
    GOSEONG("고성 가진", "fggo3"),
    GURYONGPO_HAJEONG("구룡포 하정", "fghe8"),
    GIJANG("기장", "bgj8a"),
    GIJANG_HANSUWON("기장(한수원)", "bgjh3"),
    NAGOG("나곡", "bngh3"),
    DEOGCHEON("덕천", "bdch3"),
    BUSAN_JANGAN("부산 장안", "bbji5"),
    SAMCHEOG("삼척", "bsc87"),
    YANGYANG("양양", "byy87"),
    YEONGDEOG("영덕", "byd8a"),
    ONYANG("온양", "byyh3"),
    ULSAN_GANJEOLGOJ("울산 간절곶", "bugi5"),
    ULJIN_HUPO("울진 후포", "buhi5"),
    JINHA("진하", "bjhh3"),
    POHANG_WOLPO("포항 월포", "bpwi5");

    companion object {
        fun fromCode(code: String): EastObserv {
            return values().find { it.cd == code } ?: NONE
        }

        fun allStations(): List<EastObserv> {
            return values().filter { it != NONE }
        }
    }
}

/**
 * 남해 관측소
 * South Sea Observation Stations
 *
 * 남해에 위치한 해양 관측소 목록 (일부만 표시, 전체 목록은 iOS 참조)
 * List of marine observation stations located in the South Sea (partial list, refer to iOS for full list)
 */
enum class SouthObserv(override val title: String, override val cd: String) : Observ {
    NONE("선택", ""),
    GANGJIN_MARYANG("강진 마량", "fgmk6"),
    GANGJIN_SACHO("강진 사초", "fgsl6"),
    GEOJE_GABAE("거제 가배", "fgg4c"),
    GEOJE_ILUN("거제 일운", "gi086"),
    GEOJE_HAEGEUMGANG("거제 해금강", "btei5"),
    GOHEUNG_GEUMSAN("고흥 금산", "fggm6"),
    NAMHAE_GANGJIN("남해 강진", "eng5c"),
    NAMHAE_MIJO("남해 미조", "fnm5b"),
    NAMHAE_SANGJU("남해 상주", "bnsi5"),
    BUSAN_DADAEPO("부산 다대포", "bbdi5"),
    WEST_JEJU("서제주", "ejj47"),
    YEOSU_GUNNAE("여수 군내", "fygl4"),
    YEOSU_GEUMODO("여수 금오도", "byki5"),
    YEOSU_DOLSAN("여수 돌산", "fydo4"),
    WANDO_GAHAG("완도 가학", "fwgm6"),
    WANDO_NOHWADO("완도 노화도", "wn087"),
    JEJU_GAPADO("제주 가파도", "bjgi5"),
    JEJU_GIMNYEONG("제주 김녕", "bjii5"),
    JEJU_UDO("제주 우도", "bjui5"),
    JINDO_GEUMGAB("진도 금갑", "fjgl6"),
    CHUJADO("추자도", "bcji5"),
    TONGYEONG_DUMIDO("통영 두미도", "btdi5"),
    TONGYEONG_SARYANG("통영 사량", "ty005"),
    TONGYEONG_HANSANDO("통영 한산도", "bthi5"),
    HAENAM_NAMSEONG("해남 남성", "fhno4"),
    HAENAM_BUGIL("해남 북일", "fhbl6");
    // ... 더 많은 관측소 (필요시 추가)
    // ... More stations (add as needed)

    companion object {
        fun fromCode(code: String): SouthObserv {
            return values().find { it.cd == code } ?: NONE
        }

        fun allStations(): List<SouthObserv> {
            return values().filter { it != NONE }
        }
    }
}
