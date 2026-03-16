package com.czt.bbt.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface BusApiService {
    // 1. 경기도 버스 노선번호 목록조회 v2
    @GET("6410000/busrouteservice/v2/getBusRouteListv2")
    suspend fun getBusRouteList(
        @Query("serviceKey") serviceKey: String,
        @Query("keyword") keyword: String,
        @Query("format") format: String = "json"
    ): Response<GBusRouteResponse>

    // 2. 경기도 버스 실시간 위치정보 v2
    @GET("6410000/buslocationservice/v2/getBusLocationListv2")
    suspend fun getBusLocationList(
        @Query("serviceKey") serviceKey: String,
        @Query("routeId") routeId: String,
        @Query("format") format: String = "json"
    ): Response<GBusLocationResponse>

    // 3. 경기도 버스 정류소 상세 조회 v2
    @GET("6410000/busstationservice/v2/busStationInfov2")
    suspend fun getBusStationInfo(
        @Query("serviceKey") serviceKey: String,
        @Query("stationId") stationId: String,
        @Query("format") format: String = "json"
    ): Response<GBusStationDetailResponse>

    // 4. 경기도 버스 주변정류소 목록조회 v2
    @GET("6410000/busstationservice/v2/getBusStationAroundListv2")
    suspend fun getBusStationAroundList(
        @Query("serviceKey") serviceKey: String,
        @Query("x") x: String,
        @Query("y") y: String,
        @Query("format") format: String = "json"
    ): Response<GBusStationAroundResponse>

    // 5. 경기도 버스 경유 정류소 목록 조회 v2 (새로 추가)
    @GET("6410000/busrouteservice/v2/getBusRouteStationListv2")
    suspend fun getBusRouteStationList(
        @Query("serviceKey") serviceKey: String,
        @Query("routeId") routeId: String,
        @Query("format") format: String = "json"
    ): Response<GBusRouteStationResponse>

    // 6. 경기도 버스 도착 정보 조회 (v2 없음)
    @GET("6410000/busarrivalservice/getBusArrivalList")
    suspend fun getBusArrivalList(
        @Query("serviceKey") serviceKey: String,
        @Query("stationId") stationId: String,
        @Query("format") format: String = "json"
    ): Response<GBusArrivalResponse>

    // 7. 경기도 버스 정류소 목록 조회 v2 (검색)
    @GET("6410000/busstationservice/v2/getBusStationListv2")
    suspend fun getBusStationList(
        @Query("serviceKey") serviceKey: String,
        @Query("keyword") keyword: String,
        @Query("format") format: String = "json"
    ): Response<GBusStationListResponse>

    // 8. 경기도 버스 정류소 경유 노선 목록 조회 v2 (정확한 경로 적용)
    @GET("6410000/busstationservice/v2/getBusStationViaRouteListv2")
    suspend fun getBusStationViaRouteList(
        @Query("serviceKey") serviceKey: String,
        @Query("stationId") stationId: String,
        @Query("format") format: String = "json"
    ): Response<GBusStationViaRouteResponse>
}

// --- 정류소 경유 노선 응답 모델 ---
data class GBusStationViaRouteResponse(val response: GBusStationViaRouteData)
data class GBusStationViaRouteData(val msgHeader: GBusHeader, val msgBody: GBusStationViaRouteBody?)
data class GBusStationViaRouteBody(val busRouteList: Any?)
data class GBusStationViaRouteItem(
    val routeId: Long,
    val routeName: String,
    val routeTypeCd: String,
    val routeTypeName: String,
    val regionName: String
)

// --- 정류소 검색 응답 모델 ---
data class GBusStationListResponse(val response: GBusStationListData)
data class GBusStationListData(val msgHeader: GBusHeader, val msgBody: GBusStationListBody?)
data class GBusStationListBody(val busStationList: Any?)

// --- 도착 정보 응답 모델 ---
data class GBusArrivalResponse(val response: GBusArrivalData)
data class GBusArrivalData(val msgHeader: GBusHeader, val msgBody: GBusArrivalBody?)
data class GBusArrivalBody(val busArrivalList: Any?)
data class GBusArrivalItem(
    val routeId: Long,
    val stationId: Long,
    val predictTime1: Int, // 첫번째 버스 도착예정시간(분)
    val predictTime2: Int?, // 두번째 버스 도착예정시간(분)
    val locationNo1: Int, // 첫번째 버스 위치(몇 번째 전 정류소)
    val locationNo2: Int?, // 두번째 버스 위치(몇 번째 전 정류소)
    val remainSeatCnt1: Int, // 첫번째 버스 잔여좌석
    val remainSeatCnt2: Int?, // 두번째 버스 잔여좌석
    val plateNo1: String?, // 첫번째 버스 차량번호
    val plateNo2: String?, // 두번째 버스 차량번호
    val staOrder: Int? // 정류소 순서
)

// --- 경유 정류소 응답 모델 ---
data class GBusRouteStationResponse(val response: GBusRouteStationData)
data class GBusRouteStationData(val msgHeader: GBusHeader, val msgBody: GBusRouteStationBody?)
data class GBusRouteStationBody(val busRouteStationList: Any?)

data class GBusRouteStationItem(
    val stationId: Long,
    val stationName: String,
    val stationSeq: Int,
    val mobileNo: String?,
    val x: Double,
    val y: Double,
    val turnYn: String // 회차점 여부 (Y/N)
)

// --- 공통 및 기존 모델 ---
data class GBusHeader(val queryTime: String, val resultCode: Int, val resultMessage: String)
data class GBusRouteResponse(val response: GBusRouteData)
data class GBusRouteData(val comMsgHeader: String?, val msgHeader: GBusHeader, val msgBody: GBusRouteBody?)
data class GBusRouteBody(val busRouteList: Any?)
data class GBusRouteItem(val routeId: Long, val routeName: String, val routeTypeName: String, val regionName: String, val startStationName: String, val endStationName: String)

data class GBusLocationResponse(val response: GBusLocationData)
data class GBusLocationData(val msgHeader: GBusHeader, val msgBody: GBusLocationBody?)
data class GBusLocationBody(val busLocationList: Any?)
data class GBusLocationItem(val plateNo: String, val stationId: Long, val stationSeq: Int, val stateCd: Int, val remainSeatCnt: Int, val routeId: Long)

data class GBusStationDetailResponse(val response: GBusStationDetailData)
data class GBusStationDetailData(val msgHeader: GBusHeader, val msgBody: GBusStationDetailBody?)
data class GBusStationDetailBody(val busStationInfo: GBusStationItem)
data class GBusStationItem(val stationId: Long, val stationName: String, val mobileNo: String?, val regionName: String, val x: Double, val y: Double, val centerYn: String)

data class GBusStationAroundResponse(val response: GBusStationAroundData)
data class GBusStationAroundData(val msgHeader: GBusHeader, val msgBody: GBusStationAroundBody?)
data class GBusStationAroundBody(val busStationAroundList: Any?)
data class GBusStationAroundItem(val stationId: Long, val stationName: String, val mobileNo: String?, val regionName: String, val x: Double, val y: Double, val distance: Int)
