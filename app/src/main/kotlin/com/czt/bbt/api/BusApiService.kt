package com.czt.bbt.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface BusApiService {
    // 1. 경기도 버스 노선번호 목록조회 v2 : 노선번호에 해당하는 노선의 목록을 조회한다
    // 노선 검색에서 사용
    @GET("6410000/busrouteservice/v2/getBusRouteListv2")
    suspend fun getBusRouteList(
        @Query("serviceKey") serviceKey: String,
        @Query("keyword") keyword: String,
        @Query("format") format: String = "json"
    ): Response<GBusRouteResponse>

    // 2. 경기도 버스 실시간 위치정보 v2 : 노선 ID 에 해당하는 노선의 실시간 차량 위치 정보를 조회한다
    // 위치조회에서 사용
    @GET("6410000/buslocationservice/v2/getBusLocationListv2")
    suspend fun getBusLocationList(
        @Query("serviceKey") serviceKey: String,
        @Query("routeId") routeId: String,
        @Query("format") format: String = "json"
    ): Response<GBusLocationResponse>

    // 4. 경기도 버스 주변정류소 목록조회 v2 : 위치 좌표(WGS84) 반경 500m 내에 있는 정류소 목록 (정류소명, ID, 정류소번호, 좌표값, 중앙차로여부 등) 을 제공한다.
    @GET("6410000/busstationservice/v2/getBusStationAroundListv2")
    suspend fun getBusStationAroundList(
        @Query("serviceKey") serviceKey: String,
        @Query("x") x: String,
        @Query("y") y: String,
        @Query("format") format: String = "json"
    ): Response<GBusStationAroundResponse>

    // 5. 경기도 버스 경유 정류소 목록 조회 v2 : 노선ID에 해당하는 노선의 경유 정류소 목록을 조회한다
    @GET("6410000/busrouteservice/v2/getBusRouteStationListv2")
    suspend fun getBusRouteStationList(
        @Query("serviceKey") serviceKey: String,
        @Query("routeId") routeId: String,
        @Query("format") format: String = "json"
    ): Response<GBusRouteStationResponse>

    // 6. 경기도 버스 정류소명/번호 목록조회 v2 : 정류소명/번호에 해당하는 정류소 목록(정류소명, ID, 정류소번호, 좌표값, 중앙차로여부 등)을 제공한다.
    @GET("6410000/busstationservice/v2/getBusStationListv2")
    suspend fun getBusStationList(
        @Query("serviceKey") serviceKey: String,
        @Query("keyword") keyword: String,
        @Query("format") format: String = "json"
    ): Response<GBusStationListResponse>

    // 7. 경기도 버스 정류소 경유 노선 목록 조회 v2 : 해당 정류소를 경유하는 모든 노선정보(노선번호, ID, 유형, 운행지역 등)를 제공한다.
    @GET("6410000/busstationservice/v2/getBusStationViaRouteListv2")
    suspend fun getBusStationViaRouteList(
        @Query("serviceKey") serviceKey: String,
        @Query("stationId") stationId: String,
        @Query("format") format: String = "json"
    ): Response<GBusStationViaRouteResponse>

    // 8. 경기도 버스 도착 정보 목록 조회 v2
    @GET("6410000/busarrivalservice/v2/getBusArrivalListv2")
    suspend fun getBusArrivalListV2(
        @Query("serviceKey") serviceKey: String,
        @Query("stationId") stationId: String,
        @Query("format") format: String = "json"
    ): Response<GBusArrivalListResponseV2>

    // 9. 경기도 버스 도착 정보 항목 조회 v2
    @GET("6410000/busarrivalservice/v2/getBusArrivalItemv2")
    suspend fun getBusArrivalItemV2(
        @Query("serviceKey") serviceKey: String,
        @Query("stationId") stationId: String,
        @Query("routeId") routeId: String,
        @Query("staOrder") staOrder: String,
        @Query("format") format: String = "json"
    ): Response<GBusArrivalItemResponseV2>
}

// --- 도착 정보 응답 모델 V2 ---
data class GBusArrivalListResponseV2(val response: GBusArrivalDataV2)
data class GBusArrivalDataV2(val msgHeader: GBusHeader, val msgBody: GBusArrivalBodyV2?)
data class GBusArrivalBodyV2(val busArrivalList: List<GBusArrivalInfoItem>?)

data class GBusArrivalItemResponseV2(val response: GBusArrivalItemDataV2)
data class GBusArrivalItemDataV2(val msgHeader: GBusHeader, val msgBody: GBusArrivalItemBodyV2?)
data class GBusArrivalItemBodyV2(val busArrivalItem: GBusArrivalInfoItem?)

data class GBusArrivalInfoItem(
    val stateCd2: Any?,
    val crowded1: Any?,
    val crowded2: Any?,
    val flag: String?,
    val locationNo1: Any?,
    val locationNo2: Any?,
    val lowPlate1: Any?,
    val lowPlate2: Any?,
    val plateNo1: String?,
    val plateNo2: String?,
    val predictTime1: Any?,
    val predictTime2: Any?,
    val remainSeatCnt1: Any?,
    val remainSeatCnt2: Any?,
    val routeDestId: Any?,
    val routeDestName: String?,
    val routeId: Any,
    val routeName: String?,
    val routeTypeCd: Any?,
    val staOrder: Any,
    val stationId: Any,
    val stationNm1: String?,
    val stationNm2: String?,
    val taglessCd1: Any?,
    val taglessCd2: Any?,
    val turnSeq: Any?,
    val vehId1: Any?,
    val vehId2: Any?,
    val predictTimeSec1: Any?,
    val predictTimeSec2: Any?,
    val stateCd1: Any?
)

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
