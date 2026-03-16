package com.onbada.seathermo.domain.entity

/**
 * 앱 버전 체크 결과 엔티티.
 *
 * [개념] data class는 데이터를 담기 위한 전용 클래스입니다.
 *        equals(), hashCode(), toString(), copy() 메서드를 컴파일러가 자동으로 생성해 줍니다.
 *        iOS의 struct와 동일한 역할이며, 값(Value) 기반으로 비교됩니다.
 *
 * @property minimumVersion 최소 지원 버전. 이 버전 미만이면 강제 업데이트가 발생합니다.
 * @property latestVersion 스토어에 등록된 최신 버전.
 * @property forceUpdate 강제 업데이트 여부. true이면 사용자가 업데이트를 취소할 수 없습니다.
 * @property needUpdate 업데이트 권장 여부. true이면 선택적 업데이트 안내를 표시합니다.
 * @property message 서버에서 전달하는 업데이트 안내 메시지.
 */
data class VersionStatus(
    val minimumVersion: String,
    val latestVersion: String,
    val forceUpdate: Boolean,
    val needUpdate: Boolean,
    val message: String
)
