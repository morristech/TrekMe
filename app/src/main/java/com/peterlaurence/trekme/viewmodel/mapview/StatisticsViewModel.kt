package com.peterlaurence.trekme.viewmodel.mapview

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.peterlaurence.trekme.core.track.TrackStatistics
import com.peterlaurence.trekme.events.recording.GpxRecordEvents

/**
 * The view-model for displaying track statistics in the MapView fragment.
 *
 * @author P.Laurence on 01/05/20
 */
class StatisticsViewModel @ViewModelInject constructor(
        gpxRecordEvents: GpxRecordEvents
) : ViewModel() {
    /* In this context, a null value means that statistics shouldn't be displayed - the view should
     * reflect this appropriately */
    val stats: LiveData<TrackStatistics?> = gpxRecordEvents.trackStatisticsEvent.asLiveData()
}