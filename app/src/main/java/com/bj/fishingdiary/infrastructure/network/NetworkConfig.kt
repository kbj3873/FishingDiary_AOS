package com.bj.fishingdiary.infrastructure.network

/**
 * 네트워크 설정 인터페이스
 * Network Configuration Interface
 *
 * iOS의 NetworkConfigurable에 대응
 */
interface NetworkConfigurable {
    val baseURL: String
    val headers: Map<String, String>
    val queryParameters: Map<String, String>
}

/**
 * API 데이터 네트워크 설정
 * API Data Network Configuration
 *
 * iOS의 ApiDataNetworkConfig에 대응
 */
data class ApiDataNetworkConfig(
    override val baseURL: String,
    override val headers: Map<String, String> = emptyMap(),
    override val queryParameters: Map<String, String> = emptyMap()
) : NetworkConfigurable
