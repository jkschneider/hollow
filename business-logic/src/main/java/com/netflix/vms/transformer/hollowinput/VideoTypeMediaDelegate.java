package com.netflix.vms.transformer.hollowinput;

import com.netflix.hollow.objects.delegate.HollowObjectDelegate;


@SuppressWarnings("all")
public interface VideoTypeMediaDelegate extends HollowObjectDelegate {

    public int getValueOrdinal(int ordinal);

    public VideoTypeMediaTypeAPI getTypeAPI();

}