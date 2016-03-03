package com.netflix.vms.transformer.hollowoutput;

import java.util.List;

public class VideoImages implements Cloneable {

    public List<ArtWorkDescriptor> artWorkDescriptors = null;
    public ArtWorkIndex artWorkIndex = null;

    public boolean equals(Object other) {
        if(other == this)  return true;
        if(!(other instanceof VideoImages))
            return false;

        VideoImages o = (VideoImages) other;
        if(o.artWorkDescriptors == null) {
            if(artWorkDescriptors != null) return false;
        } else if(!o.artWorkDescriptors.equals(artWorkDescriptors)) return false;
        if(o.artWorkIndex == null) {
            if(artWorkIndex != null) return false;
        } else if(!o.artWorkIndex.equals(artWorkIndex)) return false;
        return true;
    }

    public int hashCode() {
        int hashCode = 1;
        hashCode = hashCode * 31 + (artWorkDescriptors == null ? 1237 : artWorkDescriptors.hashCode());
        hashCode = hashCode * 31 + (artWorkIndex == null ? 1237 : artWorkIndex.hashCode());
        return hashCode;
    }

    public VideoImages clone() {
        try {
            VideoImages clone = (VideoImages)super.clone();
            clone.__assigned_ordinal = -1;
            return clone;
        } catch (CloneNotSupportedException cnse) { throw new RuntimeException(cnse); }
    }

    @SuppressWarnings("unused")
    private int __assigned_ordinal = -1;
}