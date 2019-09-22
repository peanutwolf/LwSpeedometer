package com.vigurskiy.speedometerdatasource.api;

import com.vigurskiy.speedometerdatasource.api.OnDataChangeListener;

interface DataSourceService{

    void provideData(float maxValue, in OnDataChangeListener listener);

}