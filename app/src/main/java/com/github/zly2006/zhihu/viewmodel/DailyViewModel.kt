/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.zly2006.zhihu.data.DailyStoriesResponse
import com.github.zly2006.zhihu.ui.DailySection
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DailyViewModel : ViewModel() {
    var sections by mutableStateOf<List<DailySection>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isLoadingMore by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var nextDate: String? = null

    private suspend fun fetchStories(httpClient: HttpClient, url: String): DailyStoriesResponse {
        val response = httpClient.get(url)
        if (!response.status.isSuccess()) {
            throw Exception("获取 $url 失败：HTTP ${response.status.value} ${response.status.description}")
        }
        return response.body()
    }

    suspend fun loadLatest(httpClient: HttpClient) {
        isLoading = true
        try {
            val data = fetchStories(httpClient, "https://news-at.zhihu.com/api/4/stories/latest")
            sections = listOf(DailySection(data.date, data.stories))
            nextDate = data.date
            error = null
        } catch (e: Exception) {
            error = "加载失败: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    suspend fun loadDate(httpClient: HttpClient, date: String) {
        isLoading = true
        sections = emptyList()
        try {
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.time = sdf.parse(date)!!
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val nextDay = sdf.format(cal.time)
            val data = fetchStories(httpClient, "https://news-at.zhihu.com/api/4/stories/before/$nextDay")
            sections = listOf(DailySection(data.date, data.stories))
            nextDate = data.date
            error = null
        } catch (e: Exception) {
            error = "加载失败: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    suspend fun loadMore(httpClient: HttpClient) {
        if (isLoadingMore || nextDate == null) return
        isLoadingMore = true
        try {
            val data = fetchStories(httpClient, "https://news-at.zhihu.com/api/4/stories/before/$nextDate")
            sections = sections + DailySection(data.date, data.stories)
            nextDate = data.date
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoadingMore = false
        }
    }
}
