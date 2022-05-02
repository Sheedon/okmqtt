package org.sheedon.sample.viewmodel;

import androidx.databinding.ObservableBoolean;
import androidx.lifecycle.ViewModel;

/**
 * java类作用描述
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/27 22:00
 */
public class SubscribeTopicViewModel extends ViewModel {

    public ObservableBoolean isSubscribe = new ObservableBoolean(false);
}
